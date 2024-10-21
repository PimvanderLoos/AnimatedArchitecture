package nl.pim16aap2.animatedarchitecture.structures.garagedoor;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationType;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionCause;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionType;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureBaseBuilder;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.testing.AssistedFactoryMocker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CounterWeightGarageDoorAnimationComponentTest
{
    private AnimationRequestData.IFactory animationRequestDataFactory;
    private StructureBaseBuilder structureBaseBuilder;

    @BeforeEach
    public void beforeEach()
        throws Exception
    {
        structureBaseBuilder = UnitTestUtil.newStructureBaseBuilder().structureBaseBuilder();

        this.animationRequestDataFactory = new AssistedFactoryMocker<>(
            AnimationRequestData.class,
            AnimationRequestData.IFactory.class
        ).setMock(int.class, "serverTickTime", 50)
            .getFactory();
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
        final GarageDoor garageDoor = openingData.createGarageDoor(structureBaseBuilder);

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
        return animationRequestDataFactory.newToggleRequestData(
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
