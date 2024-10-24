package nl.pim16aap2.animatedarchitecture.structures.garagedoor;

import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationType;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionCause;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionType;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.testing.AssistedFactoryMocker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CounterWeightGarageDoorAnimationComponentTest
{
    final AnimationRequestData.IFactory factory;

    {
        try
        {
            final var mocker = new AssistedFactoryMocker<>(
                AnimationRequestData.class,
                AnimationRequestData.IFactory.class
            );

            mocker.setMock(int.class, "serverTickTime", 50);
            this.factory = mocker.getFactory();
        }
        catch (NoSuchMethodException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getFinalPosition()
    {
        for (final var openingData : GarageDoorTestUtil.OPENING_DATA_LIST)
        {
            verifyFinalPosition(openingData);
            verifyFinalPosition(openingData.getOpposite());
        }
    }

    /**
     * Verifies that the final position is correct for the given data.
     * <p>
     * The reverse is also verified, to ensure that the final position is correct for the other direction.
     */
    private void verifyFinalPosition(GarageDoorTestUtil.OpeningData openingData)
    {
        final GarageDoor garageDoor = openingData.createGarageDoor();

        final var data = createData(garageDoor);
        final var currentToggleDir = openingData.currentToggleDir();

        final var component = new CounterWeightGarageDoorAnimationComponent(data, currentToggleDir);

        final var min = garageDoor.getMinimum();
        final var rotatedPositionMin = component.getFinalPosition(min.x(), min.y(), min.z()).position();
        final var max = garageDoor.getMaximum();
        final var rotatedPositionMax = component.getFinalPosition(max.x(), max.y(), max.z()).position();

        final var expectedCuboid = openingData.endCuboid();
        final var actualCuboid = new Cuboid(rotatedPositionMin.toInteger(), rotatedPositionMax.toInteger());

        Assertions.assertEquals(
            expectedCuboid,
            actualCuboid,
            "Expected cuboid:\n  <" + expectedCuboid + ">\n" +
                "but got:\n  <" + actualCuboid + ">\n" +
                "for opening data: '" + openingData.name() + "':\n" +
                openingData
        );
    }

    private AnimationRequestData createData(GarageDoor garageDoor)
    {
        return factory.newToggleRequestData(
            garageDoor.getSnapshot(),
            StructureActionCause.PLAYER,
            10D,
            false,
            false,
            garageDoor.getPotentialNewCoordinates().orElseThrow(),
            Mockito.mock(IPlayer.class),
            AnimationType.MOVE_BLOCKS,
            StructureActionType.TOGGLE
        );
    }
}
