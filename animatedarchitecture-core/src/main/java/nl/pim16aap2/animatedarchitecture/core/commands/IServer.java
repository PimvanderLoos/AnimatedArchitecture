package nl.pim16aap2.animatedarchitecture.core.commands;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import org.jspecify.annotations.Nullable;

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
    @FormatMethod
    default String formatCommand(
        String commandName,
        @FormatString String subCommandFormat,
        @Nullable Object @Nullable ... args)
    {
        return commandName + " " + String.format(subCommandFormat, args);
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
