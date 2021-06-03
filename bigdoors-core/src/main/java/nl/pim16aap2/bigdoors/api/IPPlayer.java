package nl.pim16aap2.bigdoors.api;

import nl.pim16aap2.bigdoors.commands.CommandDefinition;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a BigDoors player.
 *
 * @author Pim
 */
public interface IPPlayer extends IPPlayerDataContainer, ICommandSender
{
    /**
     * Gets the current location of this player.
     *
     * @return The current location of this player.
     */
    @NotNull Optional<IPLocation> getLocation();

    @Override
    default @NotNull Optional<IPPlayer> getPlayer()
    {
        return Optional.of(this);
    }

    @Override
    default boolean isPlayer()
    {
        return true;
    }

    @Override
    @NotNull CompletableFuture<Boolean> hasPermission(@NotNull String permission);

    @Override
    @NotNull CompletableFuture<BooleanPair> hasPermission(@NotNull CommandDefinition command);
}
