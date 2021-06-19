package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;
import org.jetbrains.annotations.NotNull;

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
    default @NotNull Optional<IPPlayer> getPlayer()
    {
        return Optional.empty();
    }

    @Override
    default boolean isPlayer()
    {
        return false;
    }

    @Override
    default @NotNull CompletableFuture<Boolean> hasPermission(@NotNull String permission)
    {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    default @NotNull CompletableFuture<BooleanPair> hasPermission(@NotNull CommandDefinition command)
    {
        return CompletableFuture.completedFuture(new BooleanPair(true, true));
    }
}
