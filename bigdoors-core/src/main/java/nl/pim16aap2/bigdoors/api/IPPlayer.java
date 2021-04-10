package nl.pim16aap2.bigdoors.api;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.commands.CommandDefinition;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a BigDoors player.
 *
 * @author Pim
 */
public interface IPPlayer extends IPPlayerDataContainer, IMessageable, ICommandSender
{
    /**
     * Gets the current location of this player.
     *
     * @return The current location of this player.
     */
    @NonNull Optional<IPLocation> getLocation();

    @Override
    default @NonNull Optional<IPPlayer> getPlayer()
    {
        return Optional.of(this);
    }

    @Override
    default boolean isPlayer()
    {
        return true;
    }

    @Override
    @NonNull CompletableFuture<Boolean> hasPermission(@NonNull String permission);

    @Override
    @NonNull CompletableFuture<BooleanPair> hasPermission(@NonNull CommandDefinition command);
}
