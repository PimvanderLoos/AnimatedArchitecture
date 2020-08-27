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
import nl.pim16aap2.bigdoors.tooluser.step.Step;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class CreatorTest extends CreatorTestsUtil
{
    @Test
    public void testSetName()
    {
        final @NotNull CreatorTestImpl creator = new CreatorTestImpl(PLAYER);

        Assert.assertEquals("CREATOR_GENERAL_GIVENAME", creator.getCurrentStepMessage());
        Assert.assertFalse(creator.completeNamingStep("0"));
        Assert.assertFalse(creator.handleInput(true));
        Assert.assertFalse(creator.handleInput(0));
        Assert.assertFalse(creator.handleInput(engine));
        Assert.assertFalse(creator.handleInput(engine.toLocation(world)));
        Assert.assertFalse(creator.handleInput(world));

        Assert.assertNull(creator.name);
        Assert.assertEquals(creator.getCurrentStep(), creator.stepSetName);

        Assert.assertTrue(creator.completeNamingStep(doorName));
        Assert.assertEquals(doorName, creator.name);
        Assert.assertEquals(creator.getCurrentStep(), creator.stepSetFirstPos);
    }

    @Test
    public void testSetFirstPos()
    {
        final @NotNull CreatorTestImpl creator = new CreatorTestImpl(PLAYER);
        Assert.assertTrue(creator.getProcedure().skipToStep(creator.stepSetFirstPos));

        Assert.assertEquals("CREATOR_BIGDOOR_STEP1", creator.getCurrentStepMessage());

        Assert.assertTrue(creator.setFirstPos(min.toLocation(world)));
        Assert.assertEquals(min, creator.firstPos);
        Assert.assertEquals(creator.getCurrentStep(), creator.stepSetSecondPos);
    }

    @Test
    public void testSetSecondPos()
    {
        final @NotNull TestConfigLoader testConfigLoader = getConfigLoader();
        final @NotNull CreatorTestImpl creator = new CreatorTestImpl(PLAYER);
        creator.firstPos = min;
        creator.world = world;
        final @NotNull Cuboid cuboid = new Cuboid(min, max);
        final int sizeLimit = cuboid.getVolume() - 1;
        testConfigLoader.maxDoorSize = OptionalInt.of(sizeLimit);
        Assert.assertTrue(creator.getProcedure().skipToStep(creator.stepSetSecondPos));

        Assert.assertEquals("CREATOR_BIGDOOR_STEP2", creator.getCurrentStepMessage());
        Assert.assertFalse(creator.setSecondPos(max.toLocation(world2)));
        Assert.assertFalse(creator.setSecondPos(max.toLocation(world)));
        Assert.assertEquals("CREATOR_GENERAL_AREATOOBIG " + cuboid.getVolume() + " " + sizeLimit,
                            PLAYER.getLastMessage());

        Assert.assertEquals(creator.getCurrentStep(), creator.stepSetSecondPos);

        testConfigLoader.maxDoorSize = OptionalInt.of(cuboid.getVolume());
        Assert.assertTrue(creator.setSecondPos(max.toLocation(world)));
        Assert.assertEquals(new Cuboid(min, max), creator.cuboid);
        Assert.assertEquals(creator.getCurrentStep(), creator.stepSetEnginePos);

        testConfigLoader.maxDoorSize = OptionalInt.empty();
    }

    @Test
    public void testSetEnginePos()
    {
        final @NotNull CreatorTestImpl creator = new CreatorTestImpl(PLAYER);
        creator.world = world;
        creator.cuboid = new Cuboid(min, max);
        Assert.assertTrue(creator.getProcedure().skipToStep(creator.stepSetEnginePos));

        Assert.assertEquals("CREATOR_BIGDOOR_STEP3", creator.getCurrentStepMessage());
        Assert.assertFalse(creator.completeSetEngineStep(engine.toLocation(world2)));
        Assert.assertFalse(creator.completeSetEngineStep(powerblock.toLocation(world)));
        Assert.assertEquals("CREATOR_GENERAL_INVALIDROTATIONPOINT", PLAYER.getLastMessage());

        Assert.assertEquals(creator.getCurrentStep(), creator.stepSetEnginePos);

        Assert.assertTrue(creator.completeSetEngineStep(engine.toLocation(world)));
        Assert.assertEquals(engine, creator.engine);
        Assert.assertEquals(creator.getCurrentStep(), creator.stepSetPowerBlockPos);
    }

    @Test
    public void testSetPowerBlockPos()
    {
        final @NotNull TestConfigLoader testConfigLoader = getConfigLoader();
        final @NotNull CreatorTestImpl creator = new CreatorTestImpl(PLAYER);
        creator.world = world;
        creator.cuboid = new Cuboid(min, max);
        final double distance = creator.cuboid.getCenter().getDistance(powerblock);
        final int limit = (int) distance - 1;
        testConfigLoader.maxPowerBlockDistance = OptionalInt.of(limit);
        Assert.assertTrue(creator.getProcedure().skipToStep(creator.stepSetPowerBlockPos));

        Assert.assertEquals("CREATOR_GENERAL_SETPOWERBLOCK", creator.getCurrentStepMessage());
        Assert.assertFalse(creator.completeSetPowerBlockStep(powerblock.toLocation(world2)));
        Assert.assertFalse(creator.completeSetPowerBlockStep(engine.toLocation(world)));
        Assert.assertEquals("CREATOR_GENERAL_POWERBLOCKINSIDEDOOR", PLAYER.getLastMessage());
        Assert.assertFalse(creator.completeSetPowerBlockStep(powerblock.toLocation(world)));
        Assert.assertEquals("CREATOR_GENERAL_POWERBLOCKTOOFAR " + String.format("%.2f", distance) + " " + limit,
                            PLAYER.getLastMessage());

        Assert.assertEquals(creator.getCurrentStep(), creator.stepSetPowerBlockPos);
        testConfigLoader.maxPowerBlockDistance = OptionalInt.of(limit + 2);

        Assert.assertTrue(creator.completeSetPowerBlockStep(powerblock.toLocation(world)));
        Assert.assertEquals(powerblock, creator.powerblock);
        Assert.assertEquals(creator.getCurrentStep(), creator.stepSetOpenDir);
    }

    @Test
    public void testParseOpenDirection()
    {
        final @NotNull CreatorTestImpl creator = new CreatorTestImpl(PLAYER);

        final @NotNull List<RotateDirection> validOpenDirections = creator.getDoorType().getValidOpenDirections();
        Assert.assertTrue(UnitTestUtil.optionalEquals(null, creator.parseOpenDirection("-1")));
        Assert.assertTrue(UnitTestUtil.optionalEquals(null, creator.parseOpenDirection(Integer.toString(
            validOpenDirections.size()))));

        for (int idx = 0; idx < validOpenDirections.size(); ++idx)
            Assert.assertTrue(UnitTestUtil.optionalEquals(validOpenDirections.get(idx),
                                                          creator.parseOpenDirection(Integer.toString(idx))));

        for (final @NotNull RotateDirection rotateDirection : RotateDirection.values())
        {
            final @Nullable RotateDirection goalDir;
            if (creator.getDoorType().isValidOpenDirection(rotateDirection))
                goalDir = rotateDirection;
            else
                goalDir = null;
            Assert.assertTrue(UnitTestUtil.optionalEquals(goalDir, creator.parseOpenDirection(rotateDirection.name())));
        }
    }

    @Test
    public void testSetOpenDir()
    {
        final @NotNull TestEconomyManager testEconomyManager = getEconomyManager();
        final @NotNull CreatorTestImpl creator = new CreatorTestImpl(PLAYER);
        testEconomyManager.isEconomyEnabled = true;
        testEconomyManager.price = OptionalDouble.of(10d);
        creator.cuboid = new Cuboid(min, max);
        Assert.assertTrue(creator.getProcedure().skipToStep(creator.stepSetOpenDir));

        @Nullable RotateDirection invalidDir = null;
        @Nullable RotateDirection validDir = null;
        for (final @NotNull RotateDirection rotateDirection : RotateDirection.values())
        {
            if (creator.getDoorType().isValidOpenDirection(rotateDirection))
                validDir = validDir == null ? rotateDirection : validDir;
            else
                invalidDir = invalidDir == null ? rotateDirection : invalidDir;
            if (validDir != null && invalidDir != null)
                break;
        }
        Assert.assertNotNull(invalidDir);
        Assert.assertNotNull(validDir);

        Assert.assertTrue(creator.getCurrentStepMessage().startsWith("CREATOR_GENERAL_SETOPENDIR"));
        Assert.assertFalse(creator.completeSetOpenDirStep(invalidDir.name()));

        Assert.assertNull(creator.opendir);
        Assert.assertEquals(creator.getCurrentStep(), creator.stepSetOpenDir);

        Assert.assertTrue(creator.completeSetOpenDirStep(validDir.name()));
        Assert.assertEquals(validDir, creator.opendir);

        Assert.assertEquals(creator.getCurrentStep(), creator.stepConfirmPrice);


        // Cleanup
        testEconomyManager.isEconomyEnabled = false;
        testEconomyManager.price = OptionalDouble.empty();
    }

    @Test
    public void testSkipConfirmPrice()
    {
        final @NotNull CreatorTestImpl creator = new CreatorTestImpl(PLAYER);
        Assert.assertTrue(creator.getProcedure().skipToStep(creator.stepSetOpenDir));

        @Nullable RotateDirection validDir = null;
        for (final @NotNull RotateDirection rotateDirection : RotateDirection.values())
            if (creator.getDoorType().isValidOpenDirection(rotateDirection))
            {
                validDir = rotateDirection;
                break;
            }
        Assert.assertNotNull(validDir);
        Assert.assertTrue(creator.completeSetOpenDirStep(validDir.name()));
        Assert.assertEquals(creator.getCurrentStep(), creator.stepCompleteProcess);
    }

    @Test
    public void testBuyDoor()
    {
        final @NotNull TestEconomyManager testEconomyManager = getEconomyManager();
        final @NotNull CreatorTestImpl creator = new CreatorTestImpl(PLAYER);
        creator.cuboid = new Cuboid(min, max);
        testEconomyManager.isEconomyEnabled = true;
        testEconomyManager.buyDoor = true;
        final double price = 100;
        testEconomyManager.price = OptionalDouble.of(price);

        Assert.assertTrue(creator.buyDoor());

        testEconomyManager.isEconomyEnabled = false;
        Assert.assertTrue(creator.buyDoor());

        testEconomyManager.isEconomyEnabled = true;
        testEconomyManager.buyDoor = false;
        Assert.assertFalse(creator.buyDoor());

        // Cleanup
        testEconomyManager.isEconomyEnabled = false;
        testEconomyManager.buyDoor = true;
        testEconomyManager.price = OptionalDouble.empty();
    }

    @Test
    public void testConfirmPrice()
    {
        final @NotNull TestEconomyManager testEconomyManager = getEconomyManager();
        final @NotNull CreatorTestImpl creator = new CreatorTestImpl(PLAYER);
        testEconomyManager.isEconomyEnabled = true;
        testEconomyManager.buyDoor = false;
        final double price = 100.73462;
        testEconomyManager.price = OptionalDouble.of(price);
        creator.cuboid = new Cuboid(min, max);
        Assert.assertTrue(creator.getProcedure().skipToStep(creator.stepConfirmPrice));

        Assert.assertEquals("CREATOR_GENERAL_CONFIRMPRICE " + String.format("%.2f", price),
                            creator.getCurrentStepMessage());
        Assert.assertEquals(creator.getCurrentStep(), creator.stepConfirmPrice);

        OptionalDouble foundPrice = creator.getPrice();
        Assert.assertTrue(foundPrice.isPresent());
        Assert.assertTrue(Math.abs(creator.getPrice().getAsDouble() - price) < UnitTestUtil.EPSILON);

        creator.confirmPrice(false);
        Assert.assertEquals("CREATOR_GENERAL_CANCELLED", PLAYER.getLastMessage());

        creator.confirmPrice(true);
        Assert.assertEquals(String.format("CREATOR_GENERAL_INSUFFICIENTFUNDS %.2f", price), PLAYER.getLastMessage());

        testEconomyManager.buyDoor = true;
        creator.confirmPrice(true);
        Assert.assertEquals(creator.stepCompleteProcess, creator.getCurrentStep());

        // Cleanup
        testEconomyManager.isEconomyEnabled = false;
        testEconomyManager.buyDoor = true;
        testEconomyManager.price = OptionalDouble.empty();
    }

    private static class CreatorTestImpl extends Creator
    {
        public boolean isFinished = false;
        private Step<Creator> stepSetName;
        private Step<Creator> stepSetFirstPos;
        private Step<Creator> stepSetSecondPos;
        private Step<Creator> stepSetEnginePos;
        private Step<Creator> stepSetPowerBlockPos;
        private Step<Creator> stepSetOpenDir;
        private Step<Creator> stepConfirmPrice;
        private Step<Creator> stepCompleteProcess;

        protected CreatorTestImpl(final @NotNull IPPlayer player)
        {
            super(player);
        }

        @Override
        protected List<IStep> generateSteps()
            throws InstantiationException
        {
            stepSetName = factorySetName.message(Message.CREATOR_GENERAL_GIVENAME).construct();

            stepSetFirstPos = factorySetFirstPos.message(Message.CREATOR_BIGDOOR_STEP1).construct();
            stepSetSecondPos = factorySetSecondPos.message(Message.CREATOR_BIGDOOR_STEP2).construct();
            stepSetEnginePos = factorySetEnginePos.message(Message.CREATOR_BIGDOOR_STEP3).construct();
            stepSetPowerBlockPos = factorySetPowerBlockPos.message(Message.CREATOR_GENERAL_SETPOWERBLOCK)
                                                          .construct();
            stepSetOpenDir = factorySetOpenDir.message(Message.CREATOR_GENERAL_SETOPENDIR).construct();
            stepConfirmPrice = factoryConfirmPrice.message(Message.CREATOR_GENERAL_CONFIRMPRICE).construct();
            stepCompleteProcess = factoryCompleteProcess.message(Message.CREATOR_BIGDOOR_SUCCESS).construct();

            // Just using BigDoor messages as example. It doesn't actually matter much.
            return Arrays.asList(stepSetName,
                                 stepSetFirstPos,
                                 stepSetSecondPos,
                                 stepSetEnginePos,
                                 stepSetPowerBlockPos,
                                 stepSetOpenDir,
                                 stepConfirmPrice,
                                 stepCompleteProcess);
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
