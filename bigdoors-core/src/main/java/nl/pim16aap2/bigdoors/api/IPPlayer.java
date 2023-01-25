package nl.pim16aap2.bigdoors.api;

import nl.pim16aap2.bigdoors.commands.CommandDefinition;
import nl.pim16aap2.bigdoors.commands.ICommandSender;
import nl.pim16aap2.bigdoors.commands.PermissionsStatus;

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
    Optional<IPLocation> getLocation();

    @Override
    default Optional<IPPlayer> getPlayer()
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
