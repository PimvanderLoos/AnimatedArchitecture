package nl.pim16aap2.bigdoors.tooluser.creator;

import junit.framework.Assert;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.Elevator;
import nl.pim16aap2.bigdoors.testimplementations.TestEconomyManager;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

class CreatorElevatorTest extends CreatorTestsUtil
{
    private static final int blocksToMove = 17;

    @Test
    public void testFreeCreation()
        throws InterruptedException
    {
        openDirection = RotateDirection.UP;
        engine = new Cuboid(min, max).getCenterBlock();
        final @NotNull Elevator actualDoor = new Elevator(constructDoorData(), blocksToMove);

        ((TestEconomyManager) UnitTestUtil.PLATFORM.getEconomyManager()).isEconomyEnabled = false;
        final @NotNull AtomicReference<AbstractDoorBase> resultDoorRef = setupInsertHijack();
        final @NotNull CreatorElevator creator = new CreatorElevator(PLAYER);

        Assert.assertEquals("CREATOR_GENERAL_GIVENAME", creator.getCurrentStepMessage());

        Assert.assertTrue(creator.handleInput(doorName));
        Assert.assertFalse(creator.handleInput(true));
        Assert.assertFalse(creator.handleInput(false));
        Assert.assertTrue(creator.handleInput(min.toLocation(world)));
        Assert.assertTrue(creator.handleInput(max.toLocation(world)));
        Assert.assertFalse(creator.handleInput(powerblock.toLocation(world2)));
        Assert.assertTrue(creator.handleInput(powerblock.toLocation(world)));
        Assert.assertFalse(creator.handleInput("3"));
        Assert.assertFalse(creator.handleInput(RotateDirection.NONE.name()));
        Assert.assertTrue(creator.handleInput(openDirection.name()));

        // Causes the actual insertion.
        Assert.assertTrue(creator.handleInput(blocksToMove));

        // Wait for the thread pool to finish inserting the door etc,
        threadPool.awaitTermination(20L, TimeUnit.MILLISECONDS);
        AbstractDoorBase resultDoor = resultDoorRef.get();
        Assert.assertNotNull(resultDoor);
        Assert.assertEquals(actualDoor, resultDoor);
    }
}
