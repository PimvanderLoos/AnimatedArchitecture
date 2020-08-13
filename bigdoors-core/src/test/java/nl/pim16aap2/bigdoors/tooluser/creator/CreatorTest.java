package nl.pim16aap2.bigdoors.tooluser.creator;

import junit.framework.Assert;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeBigDoor;
import nl.pim16aap2.bigdoors.testimplementations.TestConfigLoader;
import nl.pim16aap2.bigdoors.testimplementations.TestEconomyManager;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class CreatorTest extends CreatorTestsUtil
{
    @Test
    public void creator()
    {
        final @NotNull TestEconomyManager economyManager
            = (TestEconomyManager) UnitTestUtil.PLATFORM.getEconomyManager();
        final @NotNull TestConfigLoader testConfigLoader
            = (TestConfigLoader) UnitTestUtil.PLATFORM.getConfigLoader();

        economyManager.isEconomyEnabled = true;
        economyManager.buyDoor = false;
        economyManager.price = OptionalDouble.of(100);

        testConfigLoader.maxPowerBlockDistance = OptionalInt.of(10);

        powerblock = new Vector3Di(new Cuboid(min, max).getCenterBlock()).add(0, 11, 0);

        final @NotNull CreatorTestImpl creator = new CreatorTestImpl(PLAYER);

        Assert.assertEquals("CREATOR_GENERAL_GIVENAME", creator.getCurrentStepMessage());
        Assert.assertFalse(creator.handleInput("0"));
        Assert.assertTrue(creator.handleInput(doorName));

        Assert.assertEquals("CREATOR_BIGDOOR_STEP1", creator.getCurrentStepMessage());
        Assert.assertFalse(creator.handleInput(true));
        Assert.assertFalse(creator.handleInput(false));
        Assert.assertTrue(creator.handleInput(min.toLocation(world)));

        Assert.assertEquals("CREATOR_BIGDOOR_STEP2", creator.getCurrentStepMessage());
        Assert.assertFalse(creator.handleInput(max.toLocation(world2)));
        Assert.assertTrue(creator.handleInput(max.toLocation(world)));

        Assert.assertEquals("CREATOR_BIGDOOR_STEP3", creator.getCurrentStepMessage());
        Assert.assertFalse(creator.handleInput(powerblock.toLocation(world)));

        // In the and all following cases, we don't check the last message the player received, but the message
        // before it. This is done because the player will receive the instruction of the current step again
        // immediately after failing the step.
        Assert.assertEquals("CREATOR_GENERAL_INVALIDROTATIONPOINT", PLAYER.getBeforeLastMessage());
        Assert.assertTrue(creator.handleInput(engine.toLocation(world)));

        Assert.assertEquals("CREATOR_GENERAL_SETPOWERBLOCK", creator.getCurrentStepMessage());
        Assert.assertFalse(creator.handleInput(engine.toLocation(world)));
        Assert.assertEquals("CREATOR_GENERAL_POWERBLOCKINSIDEDOOR", PLAYER.getBeforeLastMessage());
        Assert.assertEquals("CREATOR_GENERAL_SETPOWERBLOCK", creator.getCurrentStepMessage());

        Assert.assertFalse(creator.handleInput(powerblock.toLocation(world)));
        Assert.assertEquals("CREATOR_GENERAL_POWERBLOCKTOOFAR 11.00 10", PLAYER.getBeforeLastMessage());

        Assert.assertFalse(creator.handleInput(engine.toLocation(world)));
        Assert.assertEquals("CREATOR_GENERAL_POWERBLOCKINSIDEDOOR", PLAYER.getBeforeLastMessage());

        testConfigLoader.maxPowerBlockDistance = OptionalInt.of(100);
        Assert.assertTrue(creator.handleInput(powerblock.toLocation(world)));

        Assert.assertTrue(creator.getCurrentStepMessage().startsWith("CREATOR_GENERAL_SETOPENDIR"));
        Assert.assertFalse(creator.handleInput("3"));
        Assert.assertFalse(creator.handleInput(RotateDirection.NONE.name()));
        Assert.assertTrue(creator.handleInput(openDirection.name()));

        Assert.assertEquals("CREATOR_GENERAL_CONFIRMPRICE 100.00", creator.getCurrentStepMessage());
        Assert.assertTrue(creator.handleInput(true));
        // Just get the last message here, because there are no more messages after this stage, as the failure to buy
        // the door should have caused the process to be aborted.
        Assert.assertEquals("CREATOR_GENERAL_INSUFFICIENTFUNDS 100.00", PLAYER.getLastMessage());


        Assert.assertFalse(creator.handleInput(true));
        Assert.assertFalse(creator.isFinished);

        // Reset
        testConfigLoader.maxPowerBlockDistance = OptionalInt.empty();
        economyManager.isEconomyEnabled = false;
        economyManager.buyDoor = true;
        economyManager.price = OptionalDouble.empty();
    }


    private static class CreatorTestImpl extends Creator
    {
        public boolean isFinished = false;

        protected CreatorTestImpl(final @NotNull IPPlayer player)
        {
            super(player);
        }

        @Override
        protected List<IStep> generateSteps()
            throws InstantiationException
        {
            // Just using BigDoor messages as example. It doesn't actually matter much.
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
        public boolean completeCreationProcess()
        {
            isFinished = true;
            return true;
        }

        @Override
        protected void giveTool()
        {
        }

        @Override
        @NotNull
        protected AbstractDoorBase constructDoor()
        {
            return null;
        }

        @Override
        @NotNull
        protected DoorType getDoorType()
        {
            return DoorTypeBigDoor.get();
        }
    }
}
