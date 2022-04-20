package nl.pim16aap2.bigdoors.tooluser.creator;

import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

/**
 * This class tests the general creation flow of the creator process.
 * <p>
 * The specific methods are test in {@link CreatorTest}.
 */
class CreatorFullTest extends CreatorTestsUtil
{
    private static DoorType doorType;

    @Test
    void runThroughProcess()
    {
        rotationPoint = new Cuboid(min, max).getCenterBlock();
        openDirection = RotateDirection.NORTH;

        doorType = Mockito.mock(DoorType.class);
        Mockito.when(doorType.getValidOpenDirections())
               .thenReturn(EnumSet.of(RotateDirection.NORTH, RotateDirection.SOUTH));

        final var door = Mockito.mock(AbstractDoor.class);
        Mockito.when(door.getDoorType()).thenReturn(doorType);

        final var creator = new CreatorTestImpl(context, player, door);

        setEconomyEnabled(true);
        setEconomyPrice(12.34);
        setBuyDoor(true);

        testCreation(creator, door,
                     doorName,
                     UnitTestUtil.getLocation(min, world),
                     UnitTestUtil.getLocation(max, world),
                     UnitTestUtil.getLocation(rotationPoint, world),
                     UnitTestUtil.getLocation(powerblock, world),
                     openDirection,
                     true);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Test
    void delayedOpenDirectionInput()
    {
        rotationPoint = new Cuboid(min, max).getCenterBlock();
        openDirection = RotateDirection.NORTH;

        doorType = Mockito.mock(DoorType.class);
        Mockito.when(doorType.getValidOpenDirections())
               .thenReturn(EnumSet.of(RotateDirection.NORTH, RotateDirection.SOUTH));

        final var door = Mockito.mock(AbstractDoor.class);
        Mockito.when(door.getDoorType()).thenReturn(doorType);

        final var creator = new CreatorTestImpl(context, player, door);

        setEconomyEnabled(true);
        setEconomyPrice(12.34);
        setBuyDoor(true);

        applySteps(creator,
                   doorName,
                   UnitTestUtil.getLocation(min, world),
                   UnitTestUtil.getLocation(max, world),
                   UnitTestUtil.getLocation(rotationPoint, world),
                   UnitTestUtil.getLocation(powerblock, world));

        Assertions.assertFalse(delayedCommandInputManager.getInputRequest(player).get()
                                                         .provide(RotateDirection.EAST).join());
        Assertions.assertTrue(delayedCommandInputManager.getInputRequest(player).get()
                                                        .provide(openDirection).join());

        testCreation(creator, door, true);
    }

    private static class CreatorTestImpl extends Creator
    {
        private final AbstractDoor door;

        protected CreatorTestImpl(ToolUser.Context context, IPPlayer player, AbstractDoor door)
        {
            super(context, player, null);
            this.door = door;
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
                                 factorySetOpenDir.messageKey("CREATOR_BASE_SET_OPEN_DIR").construct(),
                                 factoryConfirmPrice.messageKey("CREATOR_BASE_CONFIRM_PRICE").construct(),
                                 factoryCompleteProcess.messageKey("CREATOR_BIG_DOOR_SUCCESS").construct());
        }

        @Override
        protected void giveTool()
        {
        }

        @Override
        protected AbstractDoor constructDoor()
        {
            return door;
        }

        @Override
        protected DoorType getDoorType()
        {
            return doorType;
        }
    }
}
