package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.api.IMessageable;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ICommandSender extends IMessageable
{
    /**
     * Gets the {@link IPlayer} that issued the command.
     *
     * @return The {@link IPlayer} that issued the command, if it was a player that issued the command. Otherwise, an
     * empty Optional is returned.
     */
    Optional<IPlayer> getPlayer();

    /**
     * Checks if the command sender is a player.
     *
     * @return True if the command sender is a player.
     */
    boolean isPlayer();

    /**
     * Checks if this sender has a given permission.
     *
     * @param permission
     *     The permission node to check.
     * @return True if the player has access to the provided permission, otherwise false.
     */
    default CompletableFuture<Boolean> hasPermission(String permission)
    {
        return getPlayer()
            .map(player -> player.hasPermission(permission))
            .orElse(CompletableFuture.completedFuture(false));
    }

    /**
     * Checks if this sender has a given command.
     * <p>
     * Both the user permission (See {@link CommandDefinition#getUserPermission()}) and the admin permission (See
     * {@link CommandDefinition#getAdminPermission()}) are checked.
     *
     * @param command
     *     The {@link CommandDefinition} of a command to check.
     * @return A {@link PermissionsStatus} that is true if the player has access to the provided permissions, otherwise
     * false for the user and the admin permission nodes respectively.
     */
    default CompletableFuture<PermissionsStatus> hasPermission(CommandDefinition command)
    {
        return getPlayer()
            .map(player -> player.hasPermission(command))
            .orElse(CompletableFuture.completedFuture(new PermissionsStatus(false, false)));
    }
}
