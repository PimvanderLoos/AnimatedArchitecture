package nl.pim16aap2.animatedarchitecture.spigot.util.hooks;

import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Represents a compatibility hook.
 *
 * @author Pim
 */
public interface IProtectionHookSpigot
{
    /**
     * Check if this compatibility hook allows a player to break blocks at a given location.
     *
     * @param player
     *     The (fake) player to check.
     * @param loc
     *     The location to check.
     * @return True if the player is allowed to break blocks at the given location.
     */
    boolean canBreakBlock(Player player, Location loc);

    /**
     * Check if this compatibility hook allows a player to break blocks in a given cuboid.
     *
     * @param player
     *     The (fake) player to check.
     * @param world
     *     The world to check in.
     * @param cuboid
     *     The cuboid to check.
     * @return True if the player is allowed to break all the blocks in the given cuboid.
     */
    boolean canBreakBlocksBetweenLocs(Player player, World world, Cuboid cuboid);

    /**
     * Get the name of the {@link JavaPlugin} that is being hooked into.
     *
     * @return The name of the {@link JavaPlugin} that is being hooked into.
     */
    String getName();
}
