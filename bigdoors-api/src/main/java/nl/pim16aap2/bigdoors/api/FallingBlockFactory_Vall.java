package nl.pim16aap2.bigdoors.api;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Represents a Factory for {@link CustomCraftFallingBlock_Vall} and
 * {@link NMSBlock_Vall}.
 *
 * @author Pim
 */
public interface FallingBlockFactory_Vall
{
    /**
     * Create a new {@link CustomCraftFallingBlock_Vall} at the given location made
     * of the provided block.
     * 
     * @param loc   The location at which the {@link CustomCraftFallingBlock_Vall}
     *              will be spawned.
     * @param block The block that the {@link CustomCraftFallingBlock_Vall} will be
     *              made out of.
     * @return The {@link CustomCraftFallingBlock_Vall} that was constructed.
     */
    public CustomCraftFallingBlock_Vall fallingBlockFactory(Location loc, NMSBlock_Vall block);

    /**
     * Create a {@link NMSBlock_Vall} based on the block at the provided coordinates in the provided world.
     * @param world The world of the block.
     * @param x The X coordinate of the block.
     * @param y The Y coordinate of the block.
     * @param z The Z coordinate of the block.
     * @return The {@link NMSBlock_Vall} of the block at the provided coordinates in the provided world.
     */
    public NMSBlock_Vall nmsBlockFactory(World world, int x, int y, int z);
}
