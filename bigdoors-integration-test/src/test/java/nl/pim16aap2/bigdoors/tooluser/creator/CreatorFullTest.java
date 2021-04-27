package nl.pim16aap2.bigdoors.tooluser.creator;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.bigdoor.DoorTypeBigDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.tooluser.step.Step;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

class CreatorFullTest extends CreatorTestsUtil
{
    @Test
    void testSetName()
    {
        final @NonNull CreatorTestImpl creator = new CreatorTestImpl(player);

        Assertions.assertEquals("CREATOR_GENERAL_GIVENAME", creator.getCurrentStepMessage());
        Assertions.assertFalse(creator.completeNamingStep("0"));
        Assertions.assertFalse(creator.handleInput(true));
        Assertions.assertFalse(creator.handleInput(0));
        Assertions.assertFalse(creator.handleInput(engine));
        Assertions.assertFalse(creator.handleInput(engine.toLocation(world)));
        Assertions.assertFalse(creator.handleInput(world));

        Assertions.assertNull(creator.name);
        UnitTestUtil.optionalEquals(creator.stepSetName, creator.getCurrentStep());

        Assertions.assertTrue(creator.handleInput(doorName));
        Assertions.assertEquals(doorName, creator.name);

        UnitTestUtil.optionalEquals(creator.stepSetFirstPos, creator.getCurrentStep());
    }

    @Test
    void testSetFirstPos()
    {
        final @NonNull CreatorTestImpl creator = new CreatorTestImpl(player);
        Assertions.assertTrue(creator.getProcedure().skipToStep(creator.stepSetFirstPos));

        Assertions.assertEquals("CREATOR_BIGDOOR_STEP1", creator.getCurrentStepMessage());

        Assertions.assertTrue(creator.handleInput(min.toLocation(world)));
        Assertions.assertEquals(min, creator.firstPos);

        UnitTestUtil.optionalEquals(creator.stepSetSecondPos, creator.getCurrentStep());
    }

    @Test
    void testSetSecondPos()
    {
        final @NonNull CreatorTestImpl creator = new CreatorTestImpl(player);
        creator.firstPos = min;
        creator.world = world;
        final @NonNull Cuboid cuboid = new Cuboid(min, max);
        final int sizeLimit = cuboid.getVolume() - 1;
        Mockito.when(configLoader.maxDoorSize()).thenReturn(OptionalInt.of(sizeLimit));
        Assertions.assertTrue(creator.getProcedure().skipToStep(creator.stepSetSecondPos));

        Assertions.assertEquals("CREATOR_BIGDOOR_STEP2", creator.getCurrentStepMessage());
        Assertions.assertFalse(creator.setSecondPos(max.toLocation(world2)));
        Assertions.assertFalse(creator.setSecondPos(max.toLocation(world)));
        Mockito.verify(player).sendMessage("CREATOR_GENERAL_AREATOOBIG " + cuboid.getVolume() + " " + sizeLimit);

        UnitTestUtil.optionalEquals(creator.stepSetSecondPos, creator.getCurrentStep());

        Mockito.when(configLoader.maxDoorSize()).thenReturn(OptionalInt.of(cuboid.getVolume()));
        Assertions.assertTrue(creator.handleInput(max.toLocation(world)));
        Assertions.assertEquals(new Cuboid(min, max), creator.cuboid);

        UnitTestUtil.optionalEquals(creator.stepSetEnginePos, creator.getCurrentStep());
    }

    @Test
    void testSetEnginePos()
    {
        final @NonNull CreatorTestImpl creator = new CreatorTestImpl(player);
        creator.world = world;
        creator.cuboid = new Cuboid(min, max);
        Assertions.assertTrue(creator.getProcedure().skipToStep(creator.stepSetEnginePos));

        Assertions.assertEquals("CREATOR_BIGDOOR_STEP3", creator.getCurrentStepMessage());
        Assertions.assertFalse(creator.completeSetEngineStep(engine.toLocation(world2)));
        Assertions.assertFalse(creator.completeSetEngineStep(powerblock.toLocation(world)));
        Mockito.verify(player).sendMessage("CREATOR_GENERAL_INVALIDROTATIONPOINT");

        UnitTestUtil.optionalEquals(creator.stepSetEnginePos, creator.getCurrentStep());

        Assertions.assertTrue(creator.handleInput(engine.toLocation(world)));
        Assertions.assertEquals(engine, creator.engine);

        UnitTestUtil.optionalEquals(creator.stepSetPowerBlockPos, creator.getCurrentStep());
    }

    @Test
    void testSetPowerBlockPos()
    {
        final @NonNull CreatorTestImpl creator = new CreatorTestImpl(player);
        creator.world = world;
        creator.cuboid = new Cuboid(min, max);
        final double distance = creator.cuboid.getCenter().getDistance(powerblock);
        final int limit = (int) distance - 1;
        Mockito.when(configLoader.maxPowerBlockDistance()).thenReturn(OptionalInt.of(limit));
        Assertions.assertTrue(creator.getProcedure().skipToStep(creator.stepSetPowerBlockPos));

        Assertions.assertEquals("CREATOR_GENERAL_SETPOWERBLOCK", creator.getCurrentStepMessage());
        Assertions.assertFalse(creator.completeSetPowerBlockStep(powerblock.toLocation(world2)));
        Assertions.assertFalse(creator.completeSetPowerBlockStep(engine.toLocation(world)));
        Mockito.verify(player).sendMessage("CREATOR_GENERAL_POWERBLOCKINSIDEDOOR");
        Assertions.assertFalse(creator.completeSetPowerBlockStep(powerblock.toLocation(world)));
        Mockito.verify(player).sendMessage("CREATOR_GENERAL_POWERBLOCKTOOFAR " +
                                               String.format("%.2f", distance) + " " + limit);

        UnitTestUtil.optionalEquals(creator.stepSetPowerBlockPos, creator.getCurrentStep());
        Mockito.when(configLoader.maxPowerBlockDistance()).thenReturn(OptionalInt.of(limit + 2));

        Assertions.assertTrue(creator.handleInput(powerblock.toLocation(world)));
        Assertions.assertEquals(powerblock, creator.powerblock);

        UnitTestUtil.optionalEquals(creator.stepSetOpenDir, creator.getCurrentStep());
    }

    @Test
    void testParseOpenDirection()
    {
        final @NonNull CreatorTestImpl creator = new CreatorTestImpl(player);

        final @NonNull List<RotateDirection> validOpenDirections = creator.getDoorType().getValidOpenDirections();
        UnitTestUtil.optionalEquals(null, creator.parseOpenDirection("-1"));
        UnitTestUtil.optionalEquals(null, creator.parseOpenDirection(Integer.toString(validOpenDirections.size())));

        for (int idx = 0; idx < validOpenDirections.size(); ++idx)
            UnitTestUtil.optionalEquals(validOpenDirections.get(idx),
                                        creator.parseOpenDirection(Integer.toString(idx)));

        for (final @NonNull RotateDirection rotateDirection : RotateDirection.values())
        {
            final @Nullable RotateDirection goalDir;
            if (creator.getDoorType().isValidOpenDirection(rotateDirection))
                goalDir = rotateDirection;
            else
                goalDir = null;
            UnitTestUtil.optionalEquals(goalDir, creator.parseOpenDirection(rotateDirection.name()));
        }
    }

    @Test
    void testSetOpenDir()
    {
        final @NonNull CreatorTestImpl creator = new CreatorTestImpl(player);
        setEconomyEnabled(true);
        setEconomyPrice(10d);
        creator.cuboid = new Cuboid(min, max);
        Assertions.assertTrue(creator.getProcedure().skipToStep(creator.stepSetOpenDir));

        @Nullable RotateDirection invalidDir = null;
        @Nullable RotateDirection validDir = null;
        for (final @NonNull RotateDirection rotateDirection : RotateDirection.values())
        {
            if (creator.getDoorType().isValidOpenDirection(rotateDirection))
                validDir = validDir == null ? rotateDirection : validDir;
            else
                invalidDir = invalidDir == null ? rotateDirection : invalidDir;
            if (validDir != null && invalidDir != null)
                break;
        }
        Assertions.assertNotNull(invalidDir);
        Assertions.assertNotNull(validDir);

        Assertions.assertTrue(creator.getCurrentStepMessage().startsWith("CREATOR_GENERAL_SETOPENDIR"));
        Assertions.assertFalse(creator.completeSetOpenDirStep(invalidDir.name()));

        Assertions.assertNull(creator.opendir);
        UnitTestUtil.optionalEquals(creator.stepSetOpenDir, creator.getCurrentStep());

        Assertions.assertTrue(creator.handleInput(validDir.name()));
        Assertions.assertEquals(validDir, creator.opendir);

        UnitTestUtil.optionalEquals(creator.stepConfirmPrice, creator.getCurrentStep());
    }

    @Test
    void testSkipConfirmPrice()
    {
        final @NonNull CreatorTestImpl creator = new CreatorTestImpl(player);
        Assertions.assertTrue(creator.getProcedure().skipToStep(creator.stepSetOpenDir));

        @Nullable RotateDirection validDir = null;
        for (final @NonNull RotateDirection rotateDirection : RotateDirection.values())
            if (creator.getDoorType().isValidOpenDirection(rotateDirection))
            {
                validDir = rotateDirection;
                break;
            }
        Assertions.assertNotNull(validDir);
        Assertions.assertTrue(creator.handleInput(validDir.name()));
        UnitTestUtil.optionalEquals(creator.stepCompleteProcess, creator.getCurrentStep());
    }

    @Test
    void testBuyDoor()
    {
        final @NonNull CreatorTestImpl creator = new CreatorTestImpl(player);
        creator.cuboid = new Cuboid(min, max);
        setEconomyEnabled(true);
        setBuyDoor(true);
        final double price = 100;
        setEconomyPrice(price);
        creator.world = world;

        Assertions.assertTrue(creator.buyDoor());

        setEconomyEnabled(false);
        Assertions.assertTrue(creator.buyDoor());

        setEconomyEnabled(true);
        setBuyDoor(false);
        Assertions.assertFalse(creator.buyDoor());
    }

    @Test
    void testConfirmPrice()
    {
        final @NonNull CreatorTestImpl creator = new CreatorTestImpl(player);
        creator.world = world;

        final double price = 100.73462;
        setEconomyEnabled(true);
        setBuyDoor(true);
        setEconomyPrice(price);

        creator.cuboid = new Cuboid(min, max);
        Assertions.assertTrue(creator.getProcedure().skipToStep(creator.stepConfirmPrice));

        Assertions.assertEquals("CREATOR_GENERAL_CONFIRMPRICE " + String.format("%.2f", price),
                                creator.getCurrentStepMessage());
        UnitTestUtil.optionalEquals(creator.stepConfirmPrice, creator.getCurrentStep());

        OptionalDouble foundPrice = creator.getPrice();
        Assertions.assertTrue(foundPrice.isPresent());
        Assertions.assertTrue(Math.abs(creator.getPrice().getAsDouble() - price) < UnitTestUtil.EPSILON);

        setBuyDoor(false);
        creator.confirmPrice(true);
        Mockito.verify(player).sendMessage(String.format("CREATOR_GENERAL_INSUFFICIENTFUNDS %.2f", price));
    }

    private static class CreatorTestImpl extends Creator
    {
        public boolean isFinished = false;
        private Step stepSetName;
        private Step stepSetFirstPos;
        private Step stepSetSecondPos;
        private Step stepSetEnginePos;
        private Step stepSetPowerBlockPos;
        private Step stepSetOpenDir;
        private Step stepConfirmPrice;
        private Step stepCompleteProcess;

        protected CreatorTestImpl(final @NonNull IPPlayer player)
        {
            super(player);
        }

        @Override
        protected @NonNull List<IStep> generateSteps()
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
        protected @NonNull AbstractDoorBase constructDoor()
        {
            return null;
        }

        @Override
        protected @NonNull DoorType getDoorType()
        {
            return DoorTypeBigDoor.get();
        }
    }
}
