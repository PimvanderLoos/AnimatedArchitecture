package nl.pim16aap2.bigdoors.core.commands;

import nl.pim16aap2.bigdoors.core.api.IPlayer;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the server as an {@link ICommandSender}
 *
 * @author Pim
 */
public interface IServer extends ICommandSender
{
    @Override
    default Optional<IPlayer> getPlayer()
    {
        return Optional.empty();
    }

    @Override
    default boolean isPlayer()
    {
        return false;
    }

    @Override
    default CompletableFuture<Boolean> hasPermission(String permission)
    {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    default CompletableFuture<PermissionsStatus> hasPermission(CommandDefinition command)
    {
        return CompletableFuture.completedFuture(new PermissionsStatus(true, true));
    }
}
