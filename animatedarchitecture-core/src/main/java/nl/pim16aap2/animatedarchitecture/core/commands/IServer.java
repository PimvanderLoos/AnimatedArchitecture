package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the server as an {@link ICommandSender}
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
    @com.google.errorprone.annotations.FormatMethod
    default String formatCommand(
        @com.google.errorprone.annotations.FormatString String format,
        @org.jspecify.annotations.Nullable Object @org.jspecify.annotations.Nullable ... args)
    {
        return String.format(format, args);
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
