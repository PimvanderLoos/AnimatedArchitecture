package nl.pim16aap2.animatedarchitecture.structures.portcullis;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.creator.CreatorTestsUtil;
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
        openDirection = MovementDirection.UP;

        setEconomyEnabled(true);
        setEconomyPrice(12.34);
        setBuyStructure(true);

        final Portcullis actualStructure = new Portcullis(constructStructureBase(), blocksToMove);
        final CreatorPortcullis creator = new CreatorPortcullis(context, player, null);
        testCreation(creator, actualStructure,
                     structureName,
                     min.toLocation(locationFactory, world),
                     max.toLocation(locationFactory, world),
                     powerblock.toLocation(locationFactory, world),
                     false,
                     openDirection,
                     blocksToMove,
                     true);
    }

    @Test
    void testBlocksToMove()
    {
        final CreatorPortcullis creator = new CreatorPortcullis(context, player, null);
        final int blocksToMoveLimit = blocksToMove - 1;
        Mockito.when(config.maxBlocksToMove()).thenReturn(OptionalInt.of(blocksToMoveLimit));

        Assertions.assertFalse(creator.setBlocksToMove(blocksToMove));
        Mockito.verify(player).sendMessage(
            UnitTestUtil.textArgumentMatcher("creator.base.error.blocks_to_move_too_far"));

        Mockito.when(config.maxBlocksToMove()).thenReturn(OptionalInt.of(blocksToMove));
        Assertions.assertTrue(creator.setBlocksToMove(blocksToMove));
    }
}
