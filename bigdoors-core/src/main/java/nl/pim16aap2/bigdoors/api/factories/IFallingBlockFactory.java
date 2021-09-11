package nl.pim16aap2.bigdoors.api.factories;

import nl.pim16aap2.bigdoors.api.ICustomCraftFallingBlock;
import nl.pim16aap2.bigdoors.api.INMSBlock;
import nl.pim16aap2.bigdoors.api.IPLocation;

/**
 * Represents a IFactory for {@link ICustomCraftFallingBlock} and {@link INMSBlock}.
 *
 * @author Pim
 */
public interface IFallingBlockFactory
{
    /**
     * Creates a new {@link ICustomCraftFallingBlock} at the given location made of the provided block.
     *
     * @param loc
     *     The location at which the {@link ICustomCraftFallingBlock} will be spawned.
     * @param block
     *     The block that the {@link ICustomCraftFallingBlock} will be made out of.
     * @return The {@link ICustomCraftFallingBlock} that was constructed.
     */
    ICustomCraftFallingBlock fallingBlockFactory(IPLocation loc, INMSBlock block)
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
