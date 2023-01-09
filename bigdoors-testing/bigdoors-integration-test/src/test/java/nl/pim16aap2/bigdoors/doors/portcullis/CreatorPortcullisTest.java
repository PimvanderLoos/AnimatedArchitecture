package nl.pim16aap2.bigdoors.doors.portcullis;

import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.tooluser.creator.CreatorTestsUtil;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.OptionalInt;

class CreatorPortcullisTest extends CreatorTestsUtil
{
    private static final int blocksToMove = 17;

    @BeforeEach
    public void setup()
    {
        super.beforeEach();
    }

    @Test
    void createPortcullis()
    {
        rotationPoint = new Cuboid(min, max).getCenterBlock();
        openDirection = RotateDirection.UP;

        setEconomyEnabled(true);
        setEconomyPrice(12.34);
        setBuyDoor(true);

        final Portcullis actualDoor = new Portcullis(constructDoorBase(), blocksToMove);
        final CreatorPortcullis creator = new CreatorPortcullis(context, player);
        testCreation(creator, actualDoor,
                     doorName,
                     min.toLocation(locationFactory, world),
                     max.toLocation(locationFactory, world),
                     powerblock.toLocation(locationFactory, world),
                     openDirection,
                     blocksToMove,
                     true);
    }

    @Test
    void testBlocksToMove()
    {
        final CreatorPortcullis creator = new CreatorPortcullis(context, player);
        final int blocksToMoveLimit = blocksToMove - 1;
        Mockito.when(configLoader.maxBlocksToMove()).thenReturn(OptionalInt.of(blocksToMoveLimit));

        Assertions.assertFalse(creator.setBlocksToMove(blocksToMove));
        Mockito.verify(player).sendMessage(
            UnitTestUtil.toText(String.format("creator.base.error.blocks_to_move_too_far door.type.portcullis %d %d",
                                              blocksToMove, blocksToMoveLimit)));

        Mockito.when(configLoader.maxBlocksToMove()).thenReturn(OptionalInt.of(blocksToMove));
        Assertions.assertTrue(creator.setBlocksToMove(blocksToMove));
    }
}
