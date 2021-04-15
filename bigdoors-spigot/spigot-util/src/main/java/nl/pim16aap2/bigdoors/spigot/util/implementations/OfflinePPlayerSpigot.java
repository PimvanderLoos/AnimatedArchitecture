package nl.pim16aap2.bigdoors.spigot.util.implementations;

import lombok.NonNull;
import lombok.experimental.Delegate;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.logging.Level;

/**
 * Represents an implementation of {@link IPPlayer} for the Spigot platform.
 *
 * @author Pim
 */
public final class OfflinePPlayerSpigot implements IPPlayer
{
    @Delegate
    private final @NonNull PPlayerData playerData;
    private final @Nullable OfflinePlayer spigotPlayer;

    public OfflinePPlayerSpigot(final @NonNull PPlayerData playerData, final @Nullable OfflinePlayer spigotPlayer)
    {
        this.playerData = playerData;
        this.spigotPlayer = spigotPlayer;
    }

    public OfflinePPlayerSpigot(final @NonNull PPlayerData playerData)
    {
        this(playerData, Bukkit.getOfflinePlayer(playerData.getUUID()));
    }

    /**
     * Calling this method has no effect, as there is no player to send the message to.
     */
    @Override
    @Deprecated
    public void sendMessage(final @NonNull Level level, final @NonNull String message)
    {
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
    public @NonNull String toString()
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
    public @NonNull OfflinePPlayerSpigot clone()
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
    public @NonNull Optional<IPLocation> getLocation()
    {
        return Optional.empty();
    }
}
