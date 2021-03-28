package nl.pim16aap2.bigdoors.doors.portcullis;

import nl.pim16aap2.bigdoors.tooluser.creator.CreatorTestsUtil;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;
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
    public void createPortcullis()
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
    {
        final @NotNull CreatorPortcullis creator = new CreatorPortcullis(PLAYER);
        final int blocksToMoveLimit = blocksToMove - 1;
        Mockito.when(configLoader.maxBlocksToMove()).thenReturn(OptionalInt.of(blocksToMoveLimit));

        Assertions.assertFalse(creator.setBlocksToMove(blocksToMove));
        Assertions.assertEquals(String.format("CREATOR_GENERAL_BLOCKSTOMOVETOOFAR %d %d",
                                              blocksToMove, blocksToMoveLimit),
                                PLAYER.getLastMessage());
        Mockito.when(configLoader.maxBlocksToMove()).thenReturn(OptionalInt.of(blocksToMove));
        Assertions.assertTrue(creator.setBlocksToMove(blocksToMove));
    }
}
