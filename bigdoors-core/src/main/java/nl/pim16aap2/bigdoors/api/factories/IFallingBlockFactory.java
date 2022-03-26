package nl.pim16aap2.bigdoors.api.factories;

import nl.pim16aap2.bigdoors.api.INMSBlock;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IAnimatedBlock;

/**
 * Represents a IFactory for {@link IAnimatedBlock} and {@link INMSBlock}.
 *
 * @author Pim
 */
public interface IFallingBlockFactory
{
    /**
     * Creates a new {@link IAnimatedBlock} at the given location made of the provided block.
     *
     * @param loc
     *     The location at which the {@link IAnimatedBlock} will be spawned.
     * @param block
     *     The block that the {@link IAnimatedBlock} will be made out of.
     * @return The {@link IAnimatedBlock} that was constructed.
     */
    IAnimatedBlock fallingBlockFactory(IPLocation loc, INMSBlock block)
        throws Exception;

    /**
     * Creates a {@link INMSBlock} based on the block at the provided location.
     *
     * @param loc
     *     The location of the block.
     * @return The {@link INMSBlock} of the block at the provided location.
     */
    INMSBlock nmsBlockFactory(IPLocation loc)
        throws Exception;
}
