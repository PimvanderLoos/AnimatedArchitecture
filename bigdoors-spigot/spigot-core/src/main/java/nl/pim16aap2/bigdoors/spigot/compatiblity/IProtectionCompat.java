package nl.pim16aap2.bigdoors.spigot.compatiblity;

import lombok.NonNull;
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
     * Check if this compatiblity hook allows a player to break blocks at a given location.
     *
     * @param player The player to check.
     * @param loc    The location to check.
     * @return True if the player is allowed to break blocks at the given location.
     */
    boolean canBreakBlock(final @NonNull Player player, final @NonNull Location loc);

    /**
     * Check if this compatiblity hook allows a player to break blocks between two locations.
     *
     * @param player The player to check.
     * @param loc1   The start location to check.
     * @param loc2   The end location to check.
     * @return True if the player is allowed to break all the blocks between (and including) the given locations.
     */
    boolean canBreakBlocksBetweenLocs(final @NonNull Player player, final @NonNull Location loc1,
                                      final @NonNull Location loc2);

    /**
     * Check if the hook initialized properly.
     *
     * @return True if the hook initialized properly.
     */
    boolean success();

    /**
     * Get the name of the {@link JavaPlugin} that is being hooked into.
     *
     * @return The name of the {@link JavaPlugin} that is being hooked into.
     */
    @NonNull String getName();
}
