package nl.pim16aap2.bigdoors.spigot.util.implementations;

import lombok.Getter;
import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.util.WorldTime;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an implementation of {@link IPWorld} for the Spigot platform.
 *
 * @author Pim
 */
public final class PWorldSpigot implements IPWorld
{
    @Getter(onMethod = @__({@Override}))
    private final @NonNull String worldName;
    private final @Nullable World world;

    public PWorldSpigot(final @NonNull String worldName)
    {
        this.worldName = worldName;
        final @Nullable World bukkitWorld = Bukkit.getWorld(worldName);
        if (bukkitWorld == null)
            BigDoors.get().getPLogger().logThrowable(
                new NullPointerException("World \"" + worldName + "\" could not be found!"));
        world = bukkitWorld;
    }

    public PWorldSpigot(final @NonNull World world)
    {
        worldName = world.getName();
        this.world = world;
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
    public @Nullable World getBukkitWorld()
    {
        return world;
    }

    @Override
    public @NonNull WorldTime getTime()
    {
        return new WorldTime(world == null ? 0 : world.getTime());
    }

    @Override
    public @NonNull String toString()
    {
        return worldName;
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
        return worldName.equals(((IPWorld) o).getWorldName());
    }

    @Override
    public int hashCode()
    {
        return worldName.hashCode();
    }

    @Override
    public @NonNull PWorldSpigot clone()
    {
        try
        {
            return (PWorldSpigot) super.clone();
        }
        catch (CloneNotSupportedException e)
        {
            Error er = new Error(e);
            BigDoors.get().getPLogger().logThrowableSilently(er);
            throw er;
        }
    }
}
