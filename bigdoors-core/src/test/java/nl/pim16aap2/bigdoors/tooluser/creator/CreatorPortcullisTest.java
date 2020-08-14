package nl.pim16aap2.bigdoors.tooluser.creator;

import junit.framework.Assert;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.Portcullis;
import nl.pim16aap2.bigdoors.testimplementations.TestConfigLoader;
import nl.pim16aap2.bigdoors.testimplementations.TestEconomyManager;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

class CreatorPortcullisTest extends CreatorTestsUtil
{
    private static final int blocksToMove = 17;

    @Test
    public void createPortcullis()
        throws InterruptedException
    {
        engine = new Cuboid(min, max).getCenterBlock();
        openDirection = RotateDirection.UP;
        String openDirectionName = "0";


        final @NotNull Portcullis actualDoor = new Portcullis(constructDoorData(), blocksToMove);
        final @NotNull CreatorPortcullis creator = new CreatorPortcullis(PLAYER);
        testCreation(creator, actualDoor,
                     doorName,
                     min.toLocation(world),
                     max.toLocation(world),
                     powerblock.toLocation(world),
                     openDirectionName,
                     blocksToMove);
    }


    @Test
    public void testBlocksToMove()
        throws InterruptedException
    {
        engine = new Cuboid(min, max).getCenterBlock();
        openDirection = RotateDirection.UP;
        String openDirectionName = "0";

        final @NotNull TestConfigLoader testConfigLoader
            = (TestConfigLoader) UnitTestUtil.PLATFORM.getConfigLoader();

        final @NotNull Portcullis actualDoor = new Portcullis(constructDoorData(), blocksToMove);
        final @NotNull CreatorPortcullis creator = new CreatorPortcullis(PLAYER);

        ((TestEconomyManager) UnitTestUtil.PLATFORM.getEconomyManager()).isEconomyEnabled = false;
        final @NotNull AtomicReference<AbstractDoorBase> resultDoorRef = setupInsertHijack();

        Assert.assertEquals("CREATOR_GENERAL_GIVENAME", creator.getCurrentStepMessage());

        Assert.assertTrue(creator.handleInput(doorName));
        Assert.assertTrue(creator.handleInput(min.toLocation(world)));
        Assert.assertTrue(creator.handleInput(max.toLocation(world)));
        Assert.assertTrue(creator.handleInput(powerblock.toLocation(world)));
        Assert.assertTrue(creator.handleInput(openDirectionName));

        testConfigLoader.maxBlocksToMove = OptionalInt.of(2);
        Assert.assertFalse(creator.handleInput(blocksToMove));
        Assert.assertEquals("CREATOR_GENERAL_BLOCKSTOMOVETOOFAR " + blocksToMove + " 2", PLAYER.getBeforeLastMessage());

        testConfigLoader.maxBlocksToMove = OptionalInt.of(blocksToMove);
        // Causes the actual insertion.
        Assert.assertTrue(creator.handleInput(blocksToMove));

        // Wait for the thread pool to finish inserting the door etc,
        threadPool.awaitTermination(20L, TimeUnit.MILLISECONDS);
        AbstractDoorBase resultDoor = resultDoorRef.get();
        Assert.assertNotNull(resultDoor);
        Assert.assertEquals(actualDoor, resultDoor);

        // reset
        testConfigLoader.maxBlocksToMove = OptionalInt.empty();
    }
}
