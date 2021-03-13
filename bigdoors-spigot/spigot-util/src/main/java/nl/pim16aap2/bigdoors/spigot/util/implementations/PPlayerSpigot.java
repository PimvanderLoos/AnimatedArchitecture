package nl.pim16aap2.bigdoors.spigot.util.implementations;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.util.PLogger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Represents an implementation of {@link IPPlayer} for the Spigot platform.
 *
 * @author Pim
 */
// TODO: Clean up this class. E.g., the name is not available for offline players.
public final class PPlayerSpigot implements IPPlayer
{
    @NotNull
    private final String name;
    @NotNull
    private final UUID uuid;

    public PPlayerSpigot(final @NonNull PPlayerData playerData)
    {
        name = playerData.getName();
        uuid = playerData.getUUID();
    }

    public PPlayerSpigot(final @NotNull UUID uuid, final @NotNull String name)
    {
        this.name = name;
        this.uuid = uuid;
    }

    public PPlayerSpigot(final @NotNull IPPlayer player)
    {
        this(player.getUUID(), player.getName());
    }

    public PPlayerSpigot(final @NotNull OfflinePlayer player)
    {
        this(player.getUniqueId(), player.getName());
    }

    @Override
    public @NotNull UUID getUUID()
    {
        return uuid;
    }

    @Override
    public boolean hasProtectionBypassPermission()
    {
        throw new UnsupportedOperationException("Method not implemented!");
    }

    @Override
    public @NotNull Optional<IPLocation> getLocation()
    {
        @Nullable Player player = getBukkitPlayer();
        return player == null ? Optional.empty() : Optional.of(SpigotAdapter.wrapLocation(player.getLocation()));
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
        // TODO: IMPLEMENT THIS
        throw new UnsupportedOperationException("Method not implemented!");
    }

    @Override
    public @NotNull String getName()
    {
        return name;
    }

    @Override
    public void sendMessage(final @NotNull Level level, final @NotNull String message)
    {
        Player player = getBukkitPlayer();
        if (player != null)
            player.sendMessage(message);
    }

    /**
     * Gets the bukkit player represented by this {@link IPPlayer}
     *
     * @return The Bukkit player.
     */
    public @Nullable Player getBukkitPlayer()
    {
        return Bukkit.getPlayer(uuid);
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
            PLogger.get().logThrowableSilently(er);
            throw er;
        }
    }
}
