package nl.pim16aap2.bigdoors.api.factories;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.ICustomCraftFallingBlock;
import nl.pim16aap2.bigdoors.api.INMSBlock;
import nl.pim16aap2.bigdoors.api.IPLocationConst;

/**
 * Represents a Factory for {@link ICustomCraftFallingBlock} and {@link INMSBlock}.
 *
 * @author Pim
 */
public interface IFallingBlockFactory
{
//    /**
//     * Constructs {@link ICustomCraftFallingBlock}s from all blocks in the given area for allowed blocks.
//     *
//     * @param pWorld The world.
//     * @param min    The lower bound coordinates.
//     * @param max    The upper bound coordinates.
//     * @return A list of {@link ICustomCraftFallingBlock}s constructed from the blocks in the area.
//     */
//    @NonNull List<PBlockData> constructFBlocks(final @NonNull IPWorld pWorld, final @NonNull Vector3Di min,
//                                               final @NonNull Vector3Di max);

    /**
     * Creates a new {@link ICustomCraftFallingBlock} at the given location made of the provided block.
     *
     * @param loc   The location at which the {@link ICustomCraftFallingBlock} will be spawned.
     * @param block The block that the {@link ICustomCraftFallingBlock} will be made out of.
     * @return The {@link ICustomCraftFallingBlock} that was constructed.
     */
    @NonNull ICustomCraftFallingBlock fallingBlockFactory(final @NonNull IPLocationConst loc,
                                                          final @NonNull INMSBlock block)
        throws Exception;

    /**
     * Creates a {@link INMSBlock} based on the block at the provided location.
     *
     * @param loc The location of the block.
     * @return The {@link INMSBlock} of the block at the provided location.
     */
    @NonNull INMSBlock nmsBlockFactory(final @NonNull IPLocationConst loc)
        throws Exception;
}
