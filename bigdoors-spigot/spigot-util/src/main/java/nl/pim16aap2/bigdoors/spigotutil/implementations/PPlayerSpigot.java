package nl.pim16aap2.bigdoors.spigotutil.implementations;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

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

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public UUID getUUID()
    {
        return uuid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public String getName()
    {
        return name;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return String.format("%s (%s)", getUUID().toString(), getName());
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return getUUID().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PPlayerSpigot clone()
    {
        try
        {
            return (PPlayerSpigot) super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            throw new Error(e);
        }
    }
}
