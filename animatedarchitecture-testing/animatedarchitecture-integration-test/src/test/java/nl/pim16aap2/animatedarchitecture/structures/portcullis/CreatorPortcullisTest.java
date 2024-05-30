package nl.pim16aap2.animatedarchitecture.structures.portcullis;

import nl.altindag.log.LogCaptor;
import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.creator.CreatorTestsUtil;
import nl.pim16aap2.testing.logging.WithLogCapture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

import java.util.OptionalInt;

@WithLogCapture
@Timeout(1)
class CreatorPortcullisTest extends CreatorTestsUtil
{
    private static final int blocksToMove = 11;

    @BeforeEach
    public void setup()
    {
        super.beforeEach();
    }

    @Test
    void createPortcullis(LogCaptor logCaptor)
    {
        logCaptor.setLogLevelToInfo();

        rotationPoint = new Cuboid(min, max).getCenterBlock();
        openDirection = MovementDirection.UP;

        setEconomyEnabled(true);
        setEconomyPrice(12.34);
        setBuyStructure(true);

        final CreatorPortcullis creator = new CreatorPortcullis(context, player, null);
        final Portcullis actualStructure =
            new Portcullis(constructStructureBase(getTemporaryUid(creator)), blocksToMove);

        testCreation(creator, actualStructure,
            structureName,
            min.toLocation(locationFactory, world),
            max.toLocation(locationFactory, world),
            powerblock.toLocation(locationFactory, world),
            false,
            openDirection,
            blocksToMove,
            true,
            true
        );
    }

    @Test
    void testBlocksToMove(LogCaptor logCaptor)
    {
        logCaptor.setLogLevelToInfo();

        final CreatorPortcullis creator = new CreatorPortcullis(context, player, null);
        final int blocksToMoveLimit = blocksToMove - 1;
        Mockito.when(config.maxBlocksToMove()).thenReturn(OptionalInt.of(blocksToMoveLimit));

        Assertions.assertFalse(creator.provideBlocksToMove(blocksToMove));
        Mockito.verify(player).sendMessage(
            UnitTestUtil.textArgumentMatcher("creator.base.error.blocks_to_move_too_far"));

        Mockito.when(config.maxBlocksToMove()).thenReturn(OptionalInt.of(blocksToMove));
        Assertions.assertTrue(creator.provideBlocksToMove(blocksToMove));
    }
}
