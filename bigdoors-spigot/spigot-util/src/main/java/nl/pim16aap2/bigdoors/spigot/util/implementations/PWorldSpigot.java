package nl.pim16aap2.bigdoors.spigot.util.implementations;

import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.WorldTime;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents an implementation of {@link IPWorld} for the Spigot platform.
 *
 * @author Pim
 */
public final class PWorldSpigot implements IPWorld
{
    @NotNull
    private final UUID uuid;
    @Nullable
    private final World world;

    public PWorldSpigot(final @NotNull UUID worldUUID)
    {
        uuid = worldUUID;
        final @Nullable World bukkitWorld = Bukkit.getWorld(worldUUID);
        if (bukkitWorld == null)
            PLogger.get().logException(
                new NullPointerException("World \"" + worldUUID.toString() + "\" could not be found!"));
        world = bukkitWorld;
    }

    public PWorldSpigot(final @NotNull World world)
    {
        uuid = world.getUID();
        this.world = world;
    }

    @Override
    @NotNull
    public String getName()
    {
        return world == null ? "ERROR" : world.getName();
    }

    @Override
    @NotNull
    public UUID getUID()
    {
        return uuid;
    }

    @Override
    public boolean exists()
    {
        return world != null;
    }

    /**
     * Gets the bukkit world represented by this {@link IPWorld}
     *
     * @return The Bukkit world.
     */
    @Nullable
    public World getBukkitWorld()
    {
        return world;
    }

    @Override
    @NotNull
    public WorldTime getTime()
    {
        return new WorldTime(world == null ? 0 : world.getTime());
    }

    @Override
    public String toString()
    {
        String worldName = world == null ? "" : (" (" + world.getName() + ")");
        return uuid.toString() + worldName;
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
        return getUID().equals(((IPWorld) o).getUID());
    }

    @Override
    public int hashCode()
    {
        return getUID().hashCode();
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public PWorldSpigot clone()
    {
        try
        {
            return (PWorldSpigot) super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            throw new Error(e);
        }
    }
}
