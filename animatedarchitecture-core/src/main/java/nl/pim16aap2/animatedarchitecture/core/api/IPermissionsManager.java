package nl.pim16aap2.animatedarchitecture.core.api;

import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;

import java.util.OptionalInt;

public interface IPermissionsManager
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
    OptionalInt getMaxPermissionSuffix(IPlayer player, String permissionBase);

    /**
     * Checks if a player has a certain permission node or not.
     *
     * @param player
     *     The player whose permissions to check.
     * @param permissionNode
     *     The permission node to check.
     * @return True if the player has the permission node.
     */
    boolean hasPermission(IPlayer player, String permissionNode);

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
    boolean hasBypassPermissionsForAttribute(IPlayer player, StructureAttribute structureAttribute);

    /**
     * Checks if the given player is a server operator or not.
     *
     * @param player
     *     The player to check.
     * @return True if the player is a server operator on this server.
     */
    boolean isOp(IPlayer player);
}
