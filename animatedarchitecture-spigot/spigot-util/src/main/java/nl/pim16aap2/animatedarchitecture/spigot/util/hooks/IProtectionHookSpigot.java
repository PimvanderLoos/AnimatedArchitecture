package nl.pim16aap2.animatedarchitecture.spigot.util.hooks;

import com.google.common.flogger.LazyArg;
import com.google.common.flogger.LazyArgs;
import nl.pim16aap2.animatedarchitecture.core.api.IPermissionsManager;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IPermissionsManagerSpigot;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;

/**
 * Represents a compatibility hook.
 */
public interface IProtectionHookSpigot
{

    /**
     * Method that runs before any other checks are done.
     * <p>
     * The intention of this method is to allow for running quick checks that can prevent more expensive checks from
     * being run.
     * <p>
     * This method is run on the main thread. If you need to run this method asynchronously, use
     * {@link #preCheckAsync(Player, World)} instead.
     * <p>
     * If the method returns false, no further checks will be run.
     *
     * @param player
     *     The player to check.
     * @param world
     *     The world to check in.
     * @return True if the player is allowed to break blocks in the given world.
     */
    default HookPreCheckResult preCheck(Player player, World world)
    {
        return HookPreCheckResult.ALLOW;
    }

    /**
     * Method that runs before any other checks are done.
     * <p>
     * The intention of this method is to allow for running quick checks that can prevent more expensive checks from
     * being run.
     * <p>
     * This method is run asynchronously. If you need to run this method on the main thread, use
     * {@link #preCheck(Player, World)} instead.
     * <p>
     * If the method returns false, no further checks will be run.
     *
     * @param player
     *     The player to check.
     * @param world
     *     The world to check in.
     * @return True if the player is allowed to break blocks in the given world.
     */
    default CompletableFuture<HookPreCheckResult> preCheckAsync(Player player, World world)
    {
        return CompletableFuture.completedFuture(HookPreCheckResult.ALLOW);
    }

    /**
     * Check if this compatibility hook allows a player to break blocks at a given location.
     *
     * @param player
     *     The (fake) player to check.
     * @param loc
     *     The location to check.
     * @return True if the player is allowed to break blocks at the given location.
     */
    CompletableFuture<Boolean> canBreakBlock(Player player, Location loc);

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
    CompletableFuture<Boolean> canBreakBlocksInCuboid(Player player, World world, Cuboid cuboid);

    /**
     * Get the name of the {@link JavaPlugin} that is being hooked into.
     *
     * @return The name of the {@link JavaPlugin} that is being hooked into.
     */
    String getName();

    /**
     * Get the {@link ProtectionHookContext} that is being used.
     *
     * @return The {@link ProtectionHookContext} that is being used.
     */
    ProtectionHookContext getContext();

    /**
     * Shortcut method to check if a player has a given permission node.
     * <p>
     * See {@link ProtectionHookContext#getPermissionsManager()} and
     * {@link IPermissionsManager#hasPermission(IPlayer, String)}
     *
     * @param player
     *     The player to check.
     * @param node
     *     The node to check.
     * @return True if the given node is set for the given player.
     */
    default boolean hasPermission(Player player, String node)
    {
        return getContext().getPermissionsManager().hasPermission(player, node);
    }

    /**
     * Shortcut method to check if an offline player has a given permission node.
     * <p>
     * See {@link ProtectionHookContext#getPermissionsManager()} and
     * {@link IPermissionsManagerSpigot#hasPermissionOffline(World, OfflinePlayer, String)}
     *
     * @param world
     *     The world to check in.
     * @param player
     *     The player to check.
     * @param permission
     *     The permission node to check.
     * @return A CompletableFuture that will be completed with true if the player has the permission node in the given
     * world, false otherwise.
     */
    default CompletableFuture<Boolean> hasPermissionOffline(World world, OfflinePlayer player, String permission)
    {
        return getContext().getPermissionsManager().hasPermissionOffline(world, player, permission);
    }

    /**
     * Formats a player's name to a string that can be used in a message.
     *
     * @param player
     *     The player to format.
     * @return The formatted player name wrapped in a {@link LazyArg}.
     */
    default LazyArg<String> lazyFormatPlayerName(Player player)
    {
        return LazyArgs.lazy(
            () ->
                String.format(
                    "['%s': '%s', fake: %b, online: %b, op: %b]",
                    player.getName(),
                    player.getUniqueId(),
                    player instanceof IFakePlayer,
                    player.isOnline(),
                    player.isOp()
                )
        );
    }
}
