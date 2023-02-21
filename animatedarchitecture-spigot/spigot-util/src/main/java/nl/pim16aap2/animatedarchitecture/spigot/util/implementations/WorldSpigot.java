package nl.pim16aap2.animatedarchitecture.spigot.util.implementations;

import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.WorldTime;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an implementation of {@link IWorld} for the Spigot platform.
 *
 * @author Pim
 */
public record WorldSpigot(String worldName, @Nullable World world) implements IWorld
{
    public WorldSpigot(String worldName)
    {
        this(worldName, getNamedWorld(worldName));
    }

    public WorldSpigot(World world)
    {
        this(world.getName(), world);
    }

    private static World getNamedWorld(String worldName)
    {
        return Util.requireNonNull(Bukkit.getWorld(worldName), "Bukkit world with name: " + worldName);
    }

    @Override
    public boolean exists()
    {
        return world != null;
    }

    /**
     * Gets the bukkit world represented by this {@link IWorld}
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
