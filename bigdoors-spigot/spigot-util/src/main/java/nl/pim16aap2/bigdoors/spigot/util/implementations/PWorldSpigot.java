package nl.pim16aap2.bigdoors.spigot.util.implementations;

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
public record PWorldSpigot(String worldName, @Nullable World world) implements IPWorld
{
    public PWorldSpigot(String worldName)
    {
        this(worldName, getNamedWorld(worldName));
    }

    public PWorldSpigot(World world)
    {
        this(world.getName(), world);
    }

    private static @Nullable World getNamedWorld(String worldName)
    {
        final @Nullable World bukkitWorld = Bukkit.getWorld(worldName);
        if (bukkitWorld == null)
            BigDoors.get().getPLogger().logThrowable(
                new NullPointerException("World \"" + worldName + "\" could not be found!"));
        return bukkitWorld;
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
    public WorldTime getTime()
    {
        return new WorldTime(world == null ? 0 : world.getTime());
    }
}
