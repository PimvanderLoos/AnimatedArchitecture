package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.api.IPPlayer;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the server as an {@link ICommandSender}
 *
 * @author Pim
 */
public interface IPServer extends ICommandSender
{
    @Override
    default Optional<IPPlayer> getPlayer()
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
