package nl.pim16aap2.bigdoors.spigot.util.implementations;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.commands.CommandDefinition;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Represents an implementation of {@link IPPlayer} for the Spigot platform.
 *
 * @author Pim
 */
public final class PPlayerSpigot implements IPPlayer
{
    final @NonNull Player spigotPlayer;

    public PPlayerSpigot(final @NonNull Player spigotPlayer)
    {
        this.spigotPlayer = spigotPlayer;
    }

    @Override
    public @NonNull UUID getUUID()
    {
        return spigotPlayer.getUniqueId();
    }

    @Override
    public boolean hasProtectionBypassPermission()
    {
        throw new UnsupportedOperationException("Method not implemented!");
    }

    @Override
    public @NotNull Optional<IPLocation> getLocation()
    {
        return Optional.of(SpigotAdapter.wrapLocation(spigotPlayer.getLocation()));
    }

    @Override
    public @NonNull CompletableFuture<Boolean> hasPermission(@NonNull String permission)
    {
        return CompletableFuture.completedFuture(spigotPlayer.hasPermission(permission));
    }

    @Override
    public @NonNull CompletableFuture<BooleanPair> hasPermission(@NonNull CommandDefinition command)
    {
        return CompletableFuture.completedFuture(new BooleanPair(
            command.getUserPermission().map(spigotPlayer::hasPermission).orElse(false),
            command.getAdminPermission().map(spigotPlayer::hasPermission).orElse(false)));
    }

    @Override
    public int getDoorSizeLimit()
    {
        // TODO: IMPLEMENT THIS
        throw new UnsupportedOperationException("Method not implemented!");
    }

    @Override
    public int getDoorCountLimit()
    {
        // TODO: IMPLEMENT THIS
        throw new UnsupportedOperationException("Method not implemented!");
    }

    @Override
    public boolean isOp()
    {
        return spigotPlayer.isOp();
    }

    @Override
    public @NotNull String getName()
    {
        return spigotPlayer.getName();
    }

    @Override
    public void sendMessage(final @NotNull Level level, final @NotNull String message)
    {
        spigotPlayer.sendMessage(message);
    }

    /**
     * Gets the bukkit player represented by this {@link IPPlayer}
     *
     * @return The Bukkit player.
     */
    public @NonNull Player getBukkitPlayer()
    {
        return spigotPlayer;
    }

    @Override
    public @NotNull String toString()
    {
        return asString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (getClass() != o.getClass())
            return false;
        return getUUID().equals(((PPlayerSpigot) o).getUUID());
    }

    @Override
    public int hashCode()
    {
        return getUUID().hashCode();
    }

    @Override
    public @NotNull PPlayerSpigot clone()
    {
        try
        {
            return (PPlayerSpigot) super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            Error er = new Error(e);
            BigDoors.get().getPLogger().logThrowableSilently(er);
            throw er;
        }
    }
}
