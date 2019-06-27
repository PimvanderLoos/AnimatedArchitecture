package nl.pim16aap2.bigdoors.api;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Represents a Factory for {@link ICustomCraftFallingBlock} and
 * {@link INMSBlock}.
 *
 * @author Pim
 */
public interface IFallingBlockFactory
{
    /**
     * Create a new {@link ICustomCraftFallingBlock} at the given location made
     * of the provided block.
     * 
     * @param loc   The location at which the {@link ICustomCraftFallingBlock}
     *              will be spawned.
     * @param block The block that the {@link ICustomCraftFallingBlock} will be
     *              made out of.
     * @return The {@link ICustomCraftFallingBlock} that was constructed.
     */
    ICustomCraftFallingBlock fallingBlockFactory(Location loc, INMSBlock block);

    /**
     * Create a {@link INMSBlock} based on the block at the provided coordinates in the provided world.
     * @param world The world of the block.
     * @param x The X coordinate of the block.
     * @param y The Y coordinate of the block.
     * @param z The Z coordinate of the block.
     * @return The {@link INMSBlock} of the block at the provided coordinates in the provided world.
     */
    INMSBlock nmsBlockFactory(World world, int x, int y, int z);
}
