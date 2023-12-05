package nl.pim16aap2.animatedarchitecture.spigot.util.api;

import nl.pim16aap2.animatedarchitecture.core.api.IPermissionsManager;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;

/**
 * This interface is used to provide a permissions manager for Spigot.
 */
public interface IPermissionsManagerSpigot extends IPermissionsManager
{
    /**
     * Gets the highest numerical permission suffix of the base permission node it can find, if any.
     * <p>
     * For example, if a player has "permission.node.1" and "permission.node.2", this method would return (the
     * OptionalInt of) 2.
     *
     * @param player
     *     The player whose permissions to check.
     * @param permissionBase
     *     The base permission. In the example above, this would be "permission.node." (note the '.' at the end).
     * @return The highest numerical suffix of the base permission node if it exists, otherwise an empty OptionalInt.
     */
    OptionalInt getMaxPermissionSuffix(Player player, String permissionBase);

    /**
     * Checks if a player has a certain permission node or not.
     *
     * @param player
     *     The player whose permissions to check.
     *     <p>
     *     This player CANNOT be offline or a fake player! Use
     *     {@link #hasPermissionOffline(World, OfflinePlayer, String)} instead.
     * @param permissionNode
     *     The permission node to check.
     * @return True if the player has the permission node.
     *
     * @throws RuntimeException
     *     If the provided player is offline or a fake player.
     */
    boolean hasPermission(Player player, String permissionNode);

    /**
     * Asynchronously checks if an (offline) player has a certain permission node.
     *
     * @param world
     *     The world to check in.
     * @param player
     *     The player whose permissions to check.
     * @param permission
     *     The permission node to check.
     * @return A CompletableFuture that will be completed with true if the player has the permission node, false
     */
    CompletableFuture<Boolean> hasPermissionOffline(World world, OfflinePlayer player, String permission);

    /**
     * Checks if a player has bypass permissions for a specific attribute.
     * <p>
     * Having bypass permissions means that a player is able to access a given attribute for structures even if they are
     * not co-owners of those structures.
     *
     * @param player
     *     The player whose permissions to check.
     * @param structureAttribute
     *     The attribute of the Structure for which to verify if the player has bypass permissions.
     * @return True if the player has bypass permissions for the attribute.
     */
    boolean hasBypassPermissionsForAttribute(Player player, StructureAttribute structureAttribute);

    /**
     * Checks if the given player is a server operator or not.
     *
     * @param player
     *     The player to check.
     * @return True if the player is a server operator on this server.
     */
    boolean isOp(Player player);
}
