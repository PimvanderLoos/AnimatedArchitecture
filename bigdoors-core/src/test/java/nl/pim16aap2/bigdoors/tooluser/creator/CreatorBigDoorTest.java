package nl.pim16aap2.bigdoors.tooluser.creator;

import junit.framework.Assert;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.BigDoor;
import nl.pim16aap2.bigdoors.testimplementations.TestEconomyManager;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.OptionalDouble;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(MockitoExtension.class)
class CreatorBigDoorTest extends CreatorTestsUtil
{
    @Test
    public void testFreeCreation()
        throws InterruptedException
    {
        final @NotNull BigDoor actualDoor = new BigDoor(constructDoorData());

        ((TestEconomyManager) UnitTestUtil.PLATFORM.getEconomyManager()).isEconomyEnabled = false;
        final @NotNull AtomicReference<AbstractDoorBase> resultDoorRef = setupInsertHijack();
        final @NotNull CreatorBigDoor bdc = new CreatorBigDoor(PLAYER);

        Assert.assertEquals("CREATOR_GENERAL_GIVENAME", bdc.getCurrentStepMessage());

        Assert.assertTrue(bdc.handleInput(doorName));
        Assert.assertFalse(bdc.handleInput(true));
        Assert.assertFalse(bdc.handleInput(false));
        Assert.assertTrue(bdc.handleInput(min.toLocation(world)));
        Assert.assertTrue(bdc.handleInput(max.toLocation(world)));
        Assert.assertTrue(bdc.handleInput(engine.toLocation(world)));
        Assert.assertFalse(bdc.handleInput(engine.toLocation(world)));
        Assert.assertFalse(bdc.handleInput(powerblock.toLocation(world2)));
        Assert.assertTrue(bdc.handleInput(powerblock.toLocation(world)));
        Assert.assertFalse(bdc.handleInput("3"));
        Assert.assertFalse(bdc.handleInput(RotateDirection.NONE.name()));

        // Causes the actual insertion.
        Assert.assertTrue(bdc.handleInput(openDirection.name()));

        // Wait for the thread pool to finish inserting the door etc,
        threadPool.awaitTermination(20L, TimeUnit.MILLISECONDS);
        AbstractDoorBase resultDoor = resultDoorRef.get();
        Assert.assertNotNull(resultDoor);
        Assert.assertEquals(actualDoor, resultDoor);
    }

    @Test
    public void testPriceCreation()
        throws InterruptedException
    {
        final @NotNull BigDoor actualDoor = new BigDoor(constructDoorData());

        ((TestEconomyManager) UnitTestUtil.PLATFORM.getEconomyManager()).price = OptionalDouble.of(10.746D);
        ((TestEconomyManager) UnitTestUtil.PLATFORM.getEconomyManager()).isEconomyEnabled = true;
        ((TestEconomyManager) UnitTestUtil.PLATFORM.getEconomyManager()).buyDoor = true;
        final @NotNull AtomicReference<AbstractDoorBase> resultDoorRef = setupInsertHijack();
        final @NotNull CreatorBigDoor bdc = new CreatorBigDoor(PLAYER);

        Assert.assertEquals("CREATOR_GENERAL_GIVENAME", bdc.getCurrentStepMessage());

        Assert.assertTrue(bdc.handleInput(doorName));
        Assert.assertFalse(bdc.handleInput(true));
        Assert.assertFalse(bdc.handleInput(false));
        Assert.assertTrue(bdc.handleInput(min.toLocation(world)));
        Assert.assertTrue(bdc.handleInput(max.toLocation(world)));
        Assert.assertTrue(bdc.handleInput(engine.toLocation(world)));
        Assert.assertFalse(bdc.handleInput(engine.toLocation(world)));
        Assert.assertFalse(bdc.handleInput(powerblock.toLocation(world2)));
        Assert.assertTrue(bdc.handleInput(powerblock.toLocation(world)));
        Assert.assertFalse(bdc.handleInput("3"));
        Assert.assertFalse(bdc.handleInput(RotateDirection.NONE.name()));
        Assert.assertTrue(bdc.handleInput(openDirection.name()));

        Assert.assertEquals("CREATOR_GENERAL_CONFIRMPRICE 10.75", bdc.getCurrentStepMessage());

        // Causes the actual insertion.
        Assert.assertTrue(bdc.handleInput(true));


        // Wait for the thread pool to finish inserting the door etc,
        threadPool.awaitTermination(20L, TimeUnit.MILLISECONDS);
        AbstractDoorBase resultDoor = resultDoorRef.get();
        Assert.assertNotNull(resultDoor);
        Assert.assertEquals(actualDoor, resultDoor);
    }
}
