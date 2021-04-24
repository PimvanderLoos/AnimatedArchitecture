package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

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
    default @NonNull Optional<IPPlayer> getPlayer()
    {
        return Optional.empty();
    }

    @Override
    default boolean isPlayer()
    {
        return false;
    }

    @Override
    default @NonNull CompletableFuture<Boolean> hasPermission(@NonNull String permission)
    {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    default @NonNull CompletableFuture<BooleanPair> hasPermission(@NonNull CommandDefinition command)
    {
        return CompletableFuture.completedFuture(new BooleanPair(true, true));
    }
}
