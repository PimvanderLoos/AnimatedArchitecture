package nl.pim16aap2.bigdoors.api;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.commands.CommandDefinition;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ICommandSender extends IMessageable
{
    /**
     * Gets the {@link IPPlayer} that issued the command.
     *
     * @return The {@link IPPlayer} that issued the command, if it was a player that issued the command. Otherwise, an
     * empty Optional is returned.
     */
    @NonNull Optional<IPPlayer> getPlayer();

    /**
     * Checks if the command sender is a player.
     *
     * @return True if the command sender is a player.
     */
    boolean isPlayer();

    /**
     * Checks if this sender has a given permission.
     *
     * @param permission The permission node to check.
     * @return True if the player has access to the provided permission, otherwise false.
     */
    default @NonNull CompletableFuture<Boolean> hasPermission(@NonNull String permission)
    {
        return getPlayer().map(player -> player.hasPermission(permission))
                          .orElse(CompletableFuture.completedFuture(true));
    }

    /**
     * Checks if this sender has a given command.
     * <p>
     * Both the user permission (See {@link CommandDefinition#getUserPermission()}) and the admin permission (See {@link
     * CommandDefinition#getAdminPermission()} are checked.
     *
     * @param command The {@link CommandDefinition} of a command to check.
     * @return A {@link BooleanPair} that is true if the player has access to the provided permission, otherwise false
     * for both the user and the admin permission node respectively.
     */
    default @NonNull CompletableFuture<BooleanPair> hasPermission(@NonNull CommandDefinition command)
    {
        return getPlayer().map(player -> player.hasPermission(command))
                          .orElse(CompletableFuture.completedFuture(new BooleanPair(false, false)));
    }
}
