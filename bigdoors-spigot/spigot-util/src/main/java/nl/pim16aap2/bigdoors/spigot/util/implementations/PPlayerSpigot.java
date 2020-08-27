package nl.pim16aap2.bigdoors.spigot.util.implementations;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.util.PLogger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Represents an implementation of {@link IPPlayer} for the Spigot platform.
 *
 * @author Pim
 */
public final class PPlayerSpigot implements IPPlayer
{
    @NotNull
    private final String name;
    @NotNull
    private final UUID uuid;

    public PPlayerSpigot(final @NotNull UUID uuid, final @NotNull String name)
    {
        this.name = name;
        this.uuid = uuid;
    }

    public PPlayerSpigot(final @NotNull Player player)
    {
        this(player.getUniqueId(), player.getName());
    }

    public PPlayerSpigot(final @NotNull OfflinePlayer player)
    {
        this(player.getUniqueId(), player.getName());
    }

    @Override
    @NotNull
    public UUID getUUID()
    {
        return uuid;
    }

    @Override
    @NotNull
    public String getName()
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
    @Nullable
    public Player getBukkitPlayer()
    {
        return Bukkit.getPlayer(uuid);
    }

    @Override
    public String toString()
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
    public PPlayerSpigot clone()
    {
        try
        {
            return (PPlayerSpigot) super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            // TODO: Only log to file! It's already dumped in the console because it's thrown.
            Error er = new Error(e);
            PLogger.get().logError(er);
            throw er;
        }
    }
}
