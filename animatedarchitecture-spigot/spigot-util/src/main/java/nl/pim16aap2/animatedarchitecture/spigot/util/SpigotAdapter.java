package nl.pim16aap2.animatedarchitecture.spigot.util;

import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.LocationSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.WorldSpigot;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

/**
 * Class that contains utility methods to convert between AnimatedArchitecture and Spigot objects.
 * <p>
 * For example, {@link #getBukkitWorld(IWorld)} converts an {@link IWorld} to a {@link World} and
 * {@link #wrapWorld(World)} can be used to do the opposite.
 */
public final class SpigotAdapter
{
    private SpigotAdapter()
    {
        // Utility class
    }

    /**
     * Converts an {@link IWorld} object to a {@link World} object.
     * <p>
     * If the {@link ILocation} is an {@link WorldSpigot}, only a simple cast is performed. Otherwise, a new
     * {@link World} is constructed.
     *
     * @param world
     *     The AnimatedArchitecture world.
     * @return The Spigot world.
     */
    public static @Nullable World getBukkitWorld(IWorld world)
    {
        if (world instanceof WorldSpigot worldSpigot)
            return worldSpigot.getBukkitWorld();

        return Bukkit.getWorld(world.worldName());
    }

    /**
     * Converts an {@link ILocation} object to a {@link Location} object.
     * <p>
     * If the {@link ILocation} is an {@link LocationSpigot}, only a simple cast is performed. Otherwise, a new
     * {@link Location} is constructed.
     *
     * @param location
     *     The AnimatedArchitecture location.
     * @return The Spigot location.
     */
    public static Location getBukkitLocation(ILocation location)
    {
        if (location instanceof LocationSpigot)
            return ((LocationSpigot) location).getBukkitLocation();

        return new Location(
            getBukkitWorld(location.getWorld()),
            location.getX(),
            location.getY(),
            location.getZ()
        );
    }

    /**
     * Gets a Bukkit vector from a AnimatedArchitecture vector.
     *
     * @param vector
     *     The AnimatedArchitecture vector.
     * @return The bukkit vector.
     */
    public static Vector getBukkitVector(Vector3Di vector)
    {
        return new Vector(vector.x(), vector.y(), vector.z());
    }

    /**
     * Gets a Bukkit vector from a AnimatedArchitecture vector.
     *
     * @param vector
     *     The AnimatedArchitecture vector.
     * @return The bukkit vector.
     */
    public static Vector getBukkitVector(Vector3Dd vector)
    {
        return new Vector(vector.x(), vector.y(), vector.z());
    }

    /**
     * Wraps a Bukkit location in an ILocation.
     *
     * @param location
     *     The Bukkit location.
     * @return The ILocation.
     */
    public static ILocation wrapLocation(Location location)
    {
        return new LocationSpigot(location);
    }

    /**
     * Wraps a Bukkit world in an IWorld.
     *
     * @param world
     *     The Bukkit world.
     * @return The IWorld.
     */
    public static IWorld wrapWorld(World world)
    {
        return new WorldSpigot(world);
    }
}
