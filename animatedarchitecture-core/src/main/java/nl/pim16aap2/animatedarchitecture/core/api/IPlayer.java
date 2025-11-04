package nl.pim16aap2.animatedarchitecture.core.api;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandDefinition;
import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.commands.PermissionsStatus;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a AnimatedArchitecture player.
 */
public interface IPlayer extends IPlayerDataContainer, ICommandSender
{
    /**
     * Gets the current location of this player.
     *
     * @return The current location of this player.
     */
    Optional<ILocation> getLocation();

    @Override
    default Optional<IPlayer> getPlayer()
    {
        return Optional.of(this);
    }

    @Override
    default boolean isPlayer()
    {
        return true;
    }

    @Override
    @FormatMethod
    default String formatCommand(
        String commandName,
        @FormatString String subCommandFormat,
        @Nullable Object @Nullable ... args)
    {
        return "/" + commandName + " " + String.format(subCommandFormat, args);
    }

    @Override
    CompletableFuture<Boolean> hasPermission(String permission);

    @Override
    CompletableFuture<PermissionsStatus> hasPermission(CommandDefinition command);

    /**
     * Returns whether this player is online.
     *
     * @return True if this player is online.
     */
    boolean isOnline();
}
