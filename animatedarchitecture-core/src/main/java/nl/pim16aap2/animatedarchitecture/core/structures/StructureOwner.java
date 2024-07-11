package nl.pim16aap2.animatedarchitecture.core.structures;

import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.PlayerData;
import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;

/**
 * Contains all details needed about the owner of a Structure.
 *
 * @param structureUID
 *     The UID of the structure that is owned.
 * @param permission
 *     The {@link PermissionLevel} level at which the player owns the structure.
 * @param playerData
 *     The {@link IPlayer} object represented by this {@link StructureOwner}.
 */
public record StructureOwner(long structureUID, PermissionLevel permission, PlayerData playerData)
{
    /**
     * Checks if a command sender is the same as the player represented by this {@link StructureOwner}.
     *
     * @param commandSender
     *     The {@link ICommandSender} to check against.
     * @return {@code true} if the command sender is the same as the one represented by this {@link StructureOwner}.
     */
    public boolean matches(ICommandSender commandSender)
    {
        return commandSender
            .getPlayer()
            .map(player -> player.getUUID().equals(playerData.uuid()))
            .orElse(false);
    }
}
