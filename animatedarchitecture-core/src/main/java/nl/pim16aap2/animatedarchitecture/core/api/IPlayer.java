package nl.pim16aap2.animatedarchitecture.core.api;

import nl.pim16aap2.animatedarchitecture.core.commands.CommandDefinition;
import nl.pim16aap2.animatedarchitecture.core.commands.ICommandSender;
import nl.pim16aap2.animatedarchitecture.core.commands.PermissionsStatus;

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
    CompletableFuture<Boolean> hasPermission(String permission);

    @Override
    CompletableFuture<PermissionsStatus> hasPermission(CommandDefinition command);

    /**
     * @return True if this player is online.
     */
    boolean isOnline();
}
