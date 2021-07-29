package nl.pim16aap2.bigdoors.tooluser.creator;

import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

/**
 * This class tests the general creation flow of the creator process.
 * <p>
 * The specific methods are test in {@link CreatorTest}.
 */
class CreatorFullTest extends CreatorTestsUtil
{
    @Test
    void runThroughProcess()
    {
        engine = new Cuboid(min, max).getCenterBlock();
        openDirection = RotateDirection.UP;

        @NotNull val doorType = Mockito.mock(DoorType.class);
        Mockito.when(doorType.getValidOpenDirections())
               .thenReturn(Arrays.asList(RotateDirection.NORTH, RotateDirection.SOUTH));

        @NotNull val door = Mockito.mock(DoorBase.class);
        Mockito.when(door.getDoorType()).thenReturn(doorType);

        @NotNull val creator = new CreatorTestImpl(player, door);

        setEconomyEnabled(true);
        setEconomyPrice(12.34);
        setBuyDoor(true);

        Assertions.assertTrue(BigDoors.get().getPlatform().getEconomyManager().isEconomyEnabled());

        BigDoors.get().getPLogger().setConsoleLogLevel(Level.ALL);
        testCreation(creator, door,
                     doorName,
                     UnitTestUtil.getLocation(min, world),
                     UnitTestUtil.getLocation(max, world),
                     UnitTestUtil.getLocation(engine, world),
                     UnitTestUtil.getLocation(powerblock, world),
                     "0",
                     true);
    }

    private static class CreatorTestImpl extends Creator
    {
        private final @NotNull DoorBase door;

        protected CreatorTestImpl(final @NotNull IPPlayer player, final @NotNull DoorBase door)
        {
            super(player, null);
            this.door = door;
        }

        @Override
        protected @NotNull List<IStep> generateSteps()
            throws InstantiationException
        {
            return Arrays.asList(factorySetName.message(Message.CREATOR_GENERAL_GIVENAME).construct(),
                                 factorySetFirstPos.message(Message.CREATOR_BIGDOOR_STEP1).construct(),
                                 factorySetSecondPos.message(Message.CREATOR_BIGDOOR_STEP2).construct(),
                                 factorySetEnginePos.message(Message.CREATOR_BIGDOOR_STEP3).construct(),
                                 factorySetPowerBlockPos.message(Message.CREATOR_GENERAL_SETPOWERBLOCK).construct(),
                                 factorySetOpenDir.message(Message.CREATOR_GENERAL_SETOPENDIR).construct(),
                                 factoryConfirmPrice.message(Message.CREATOR_GENERAL_CONFIRMPRICE).construct(),
                                 factoryCompleteProcess.message(Message.CREATOR_BIGDOOR_SUCCESS).construct());
        }

        @Override
        protected void giveTool()
        {
        }

        @Override
        protected @NotNull DoorBase constructDoor()
        {
            return door;
        }

        @Override
        protected @NotNull DoorType getDoorType()
        {
            return door.getDoorType();
        }
    }
}
