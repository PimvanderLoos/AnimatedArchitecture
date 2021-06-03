package nl.pim16aap2.bigdoors.spigot.util.implementations;

import lombok.experimental.Delegate;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.commands.CommandDefinition;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Represents an implementation of {@link IPPlayer} for the Spigot platform.
 *
 * @author Pim
 */
public final class OfflinePPlayerSpigot implements IPPlayer
{
    @Delegate
    private final @NotNull PPlayerData playerData;
    private final @Nullable OfflinePlayer spigotPlayer;

    public OfflinePPlayerSpigot(final @NotNull PPlayerData playerData, final @Nullable OfflinePlayer spigotPlayer)
    {
        this.playerData = playerData;
        this.spigotPlayer = spigotPlayer;
    }

    public OfflinePPlayerSpigot(final @NotNull PPlayerData playerData)
    {
        this(playerData, Bukkit.getOfflinePlayer(playerData.getUUID()));
    }

    /**
     * Calling this method has no effect, as there is no player to send the message to.
     */
    @Override
    @Deprecated
    public void sendMessage(final @NotNull Level level, final @NotNull String message)
    {
    }

    @Override
    public @NotNull CompletableFuture<Boolean> hasPermission(@NotNull String permission)
    {
        return CompletableFuture.completedFuture(isOp());
    }

    @Override
    public @NotNull CompletableFuture<BooleanPair> hasPermission(@NotNull CommandDefinition command)
    {
        return CompletableFuture.completedFuture(new BooleanPair(isOp(), isOp()));
    }

    /**
     * Gets the bukkit {@link OfflinePlayer} represented by this {@link IPPlayer} if it exists.
     *
     * @return The Bukkit player.
     */
    public @Nullable OfflinePlayer getBukkitPlayer()
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
        return getUUID().equals(((OfflinePPlayerSpigot) o).getUUID());
    }

    @Override
    public int hashCode()
    {
        return getUUID().hashCode();
    }

    @Override
    public @NotNull OfflinePPlayerSpigot clone()
    {
        try
        {
            return (OfflinePPlayerSpigot) super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            Error er = new Error(e);
            BigDoors.get().getPLogger().logThrowableSilently(er);
            throw er;
        }
    }

    @Override
    public @NotNull Optional<IPLocation> getLocation()
    {
        return Optional.empty();
    }
}
