package nl.pim16aap2.bigdoors.tooluser.creator;

import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 * This class tests the general creation flow of the creator process.
 * <p>
 * The specific methods are test in {@link CreatorTest}.
 */
@Timeout(1)
class CreatorFullTest extends CreatorTestsUtil
{
    private static MovableType movableType;

    @Test
    void runThroughProcess()
    {
        rotationPoint = new Cuboid(min, max).getCenterBlock();
        openDirection = MovementDirection.NORTH;

        movableType = Mockito.mock(MovableType.class);
        Mockito.when(movableType.getValidOpenDirections())
               .thenReturn(EnumSet.of(MovementDirection.NORTH, MovementDirection.SOUTH));

        final var movable = Mockito.mock(AbstractMovable.class);
        Mockito.when(movable.getType()).thenReturn(movableType);

        final var creator = new CreatorTestImpl(context, player, movable);

        setEconomyEnabled(true);
        setEconomyPrice(12.34);
        setBuyMovable(true);

        testCreation(creator, movable,
                     movableName,
                     UnitTestUtil.getLocation(min, world),
                     UnitTestUtil.getLocation(max, world),
                     UnitTestUtil.getLocation(rotationPoint, world),
                     UnitTestUtil.getLocation(powerblock, world),
                     false,
                     openDirection,
                     true);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void delayedOpenDirectionInput()
    {
        rotationPoint = new Cuboid(min, max).getCenterBlock();
        openDirection = MovementDirection.NORTH;

        movableType = Mockito.mock(MovableType.class);
        Mockito.when(movableType.getValidOpenDirections())
               .thenReturn(EnumSet.of(MovementDirection.NORTH, MovementDirection.SOUTH));

        final var movable = Mockito.mock(AbstractMovable.class);
        Mockito.when(movable.getType()).thenReturn(movableType);

        final var creator = new CreatorTestImpl(context, player, movable);

        setEconomyEnabled(true);
        setEconomyPrice(12.34);
        setBuyMovable(true);

        applySteps(creator,
                   movableName,
                   UnitTestUtil.getLocation(min, world),
                   UnitTestUtil.getLocation(max, world),
                   UnitTestUtil.getLocation(rotationPoint, world),
                   UnitTestUtil.getLocation(powerblock, world));

        Assertions.assertDoesNotThrow(() -> delayedCommandInputManager.getInputRequest(player).get()
                                                                      .provide(false).join());
        Assertions.assertDoesNotThrow(() -> delayedCommandInputManager.getInputRequest(player).get()
                                                                      .provide(MovementDirection.EAST).join());
        Assertions.assertDoesNotThrow(() -> delayedCommandInputManager.getInputRequest(player).get()
                                                                      .provide(openDirection).join());

        testCreation(creator, movable, true);
    }

    private static class CreatorTestImpl extends Creator
    {
        private final AbstractMovable movable;

        protected CreatorTestImpl(ToolUser.Context context, IPPlayer player, AbstractMovable movable)
        {
            super(context, player, null);
            this.movable = movable;
        }

        @Override
        protected List<IStep> generateSteps()
            throws InstantiationException
        {
            return Arrays.asList(factorySetName.messageKey("CREATOR_BASE_GIVE_NAME").construct(),
                                 factorySetFirstPos.messageKey("CREATOR_BIG_DOOR_STEP1").construct(),
                                 factorySetSecondPos.messageKey("CREATOR_BIG_DOOR_STEP2").construct(),
                                 factorySetRotationPointPos.messageKey("CREATOR_BIG_DOOR_STEP3").construct(),
                                 factorySetPowerBlockPos.messageKey("CREATOR_BASE_SET_POWER_BLOCK").construct(),
                                 factorySetOpenStatus.messageKey("CREATOR_BASE_SET_OPEN_DIR").construct(),
                                 factorySetOpenDir.messageKey("CREATOR_BASE_SET_OPEN_DIR").construct(),
                                 factoryConfirmPrice.messageKey("CREATOR_BASE_CONFIRM_PRICE").construct(),
                                 factoryCompleteProcess.messageKey("CREATOR_BIG_DOOR_SUCCESS").construct());
        }

        @Override
        protected void giveTool()
        {
        }

        @Override
        protected AbstractMovable constructMovable()
        {
            return movable;
        }

        @Override
        protected MovableType getMovableType()
        {
            return movableType;
        }
    }
}
