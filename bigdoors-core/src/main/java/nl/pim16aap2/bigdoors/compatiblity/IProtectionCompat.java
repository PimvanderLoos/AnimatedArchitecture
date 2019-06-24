package nl.pim16aap2.bigdoors.compatiblity;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Represents a Compatibility hook.
 *
 * @author Pim
 */
interface IProtectionCompat
{
    /**
     * Check if this compatiblity hook allows a player to break blocks at a given
     * location.
     * 
     * @param player The player to check.
     * @param loc    The location to check.
     * @return True if the player is allowed to break blocks at the given location.
     */
    public boolean canBreakBlock(Player player, Location loc);

    /**
     * Check if this compatiblity hook allows a player to break blocks between two
     * locations.
     * 
     * @param player The player to check.
     * @param loc1   The start location to check.
     * @param loc2   The end location to check.
     * @return True if the player is allowed to break all the blocks between (and
     *         including) the given locations.
     */
    public boolean canBreakBlocksBetweenLocs(Player player, Location loc1, Location loc2);

    /**
     * Check if the hook initialized properly.
     * 
     * @return True if the hook initialized properly.
     */
    public boolean success();

    /**
     * Get the {@link JavaPlugin} that is being hooked into.
     * 
     * @return The {@link JavaPlugin} that is being hooked into.
     */
    public JavaPlugin getPlugin();

    /**
     * Get the name of the {@link JavaPlugin} that is being hooked into.
     * 
     * @return The name of the {@link JavaPlugin} that is being hooked into.
     */
    public String getName();
}
