package nl.pim16aap2.animatedarchitecture.structures.portcullis;

import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.creator.CreatorTestsUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

import java.util.OptionalInt;

import static nl.pim16aap2.animatedarchitecture.core.UnitTestUtil.assertThatMessageable;

@Timeout(1)
class CreatorPortcullisTest extends CreatorTestsUtil
{
    private static final int blocksToMove = 11;

    private final StructureType type = StructureTypePortcullis.get();

    @BeforeEach
    public void setup()
    {
        super.beforeEach();
    }

    @Test
    void createPortcullis()
    {
        final boolean isOpen = false;
        openDirection = MovementDirection.UP;

        setEconomyEnabled(true);
        setEconomyPrice(12.34);
        setBuyStructure(true);

        final CreatorPortcullis creator = new CreatorPortcullis(context, player, null);
        final Structure actualStructure = constructStructure(
            type,
            creator.getUnregisteredUID(),
            Property.OPEN_STATUS, isOpen,
            Property.BLOCKS_TO_MOVE, blocksToMove
        );

        testCreation(creator, actualStructure,
            structureName,
            min.toLocation(locationFactory, world),
            max.toLocation(locationFactory, world),
            powerblock.toLocation(locationFactory, world),
            isOpen,
            openDirection,
            blocksToMove,
            true,
            true
        );
    }

    @Test
    void testBlocksToMove()
    {
        final CreatorPortcullis creator = new CreatorPortcullis(context, player, null);
        final int blocksToMoveLimit = blocksToMove - 1;
        Mockito.when(config.maxBlocksToMove()).thenReturn(OptionalInt.of(blocksToMoveLimit));

        Assertions.assertFalse(creator.provideBlocksToMove(blocksToMove));
        assertThatMessageable(player)
            .sentErrorMessage("creator.base.error.blocks_to_move_too_far")
            .withArgs(type.getLocalizationKey(), blocksToMove, blocksToMoveLimit);

        Mockito.when(config.maxBlocksToMove()).thenReturn(OptionalInt.of(blocksToMove));
        Assertions.assertTrue(creator.provideBlocksToMove(blocksToMove));
    }
}
