package nl.pim16aap2.animatedarchitecture.spigot.util.implementations;

import com.google.common.flogger.StackSize;
import com.google.errorprone.annotations.CheckReturnValue;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector2Di;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an implementation of {@link ILocation} for the Spigot platform.
 */
@Flogger
public final class LocationSpigot implements ILocation
{
    private final Location location;

    private final IWorld world;

    public LocationSpigot(IWorld world, @Nullable World bukkitWorld, double x, double y, double z)
    {
        location = new Location(bukkitWorld, x, y, z);
        this.world = world;
    }

    public LocationSpigot(IWorld world, double x, double y, double z)
    {
        final @Nullable World bukkitWorld = retrieveBukkitWorld(world);
        if (bukkitWorld == null)
            log.atFine().withStackTrace(StackSize.FULL).log("Bukkit world of world '%s' is null!", world);
        location = new Location(bukkitWorld, x, y, z);
        this.world = world;
    }

    public LocationSpigot(World world, double x, double y, double z)
    {
        location = new Location(world, x, y, z);
        this.world = new WorldSpigot(world);
    }

    public LocationSpigot(Location location)
    {
        Util.requireNonNull(location.getWorld(), "world of location " + location);
        this.location = location.clone();
        world = new WorldSpigot(location.getWorld());
    }

    /**
     * Attempts to retrieve a Bukkit World object from an IWorld object.
     * <p>
     * If the IWorld object is a WorldSpigot subclass, the world is retrieved from its member variable. Otherwise, its
     * name is used to retrieve it from Bukkit.
     *
     * @param world
     *     The {@link IWorld}.
     * @return The Bukkit world, if it could be found.
     */
    @CheckReturnValue @Contract(pure = true)
    private static @Nullable World retrieveBukkitWorld(IWorld world)
    {
        if (world instanceof WorldSpigot worldSpigot)
            return worldSpigot.getBukkitWorld();
        return Bukkit.getWorld(world.worldName());
    }

    @Override
    @CheckReturnValue @Contract(pure = true)
    public Vector2Di getChunk()
    {
        return new Vector2Di(location.getBlockX() << 4, location.getBlockZ() << 4);
    }

    @Override
    @CheckReturnValue @Contract(pure = true)
    public int getBlockX()
    {
        return location.getBlockX();
    }

    @Override
    @CheckReturnValue @Contract(pure = true)
    public int getBlockY()
    {
        return location.getBlockY();
    }

    @Override
    @CheckReturnValue @Contract(pure = true)
    public int getBlockZ()
    {
        return location.getBlockZ();
    }

    @Override
    @CheckReturnValue @Contract(pure = true)
    public double getX()
    {
        return location.getX();
    }

    @Override
    @CheckReturnValue @Contract(pure = true)
    public double getY()
    {
        return location.getY();
    }

    @Override
    @CheckReturnValue @Contract(pure = true)
    public double getZ()
    {
        return location.getZ();
    }

    @Override
    @CheckReturnValue @Contract(pure = true)
    public ILocation setX(double newVal)
    {
        return new LocationSpigot(world, location.getWorld(), newVal, getY(), getZ());
    }

    @Override
    @CheckReturnValue @Contract(pure = true)
    public ILocation setY(double newVal)
    {
        return new LocationSpigot(world, location.getWorld(), getX(), newVal, getZ());
    }

    @Override
    @CheckReturnValue @Contract(pure = true)
    public ILocation setZ(double newVal)
    {
        return new LocationSpigot(world, location.getWorld(), getX(), getY(), newVal);
    }

    @Override
    @CheckReturnValue @Contract(pure = true)
    public ILocation add(double x, double y, double z)
    {
        return new LocationSpigot(world, location.getWorld(), getX() + x, getY() + y, getZ() + z);
    }

    @Override
    @CheckReturnValue @Contract(pure = true)
    public ILocation add(Vector3Di vector)
    {
        return add(vector.x(), vector.y(), vector.z());
    }

    @Override
    @CheckReturnValue @Contract(pure = true)
    public ILocation add(Vector3Dd vector)
    {
        return add(vector.x(), vector.y(), vector.z());
    }

    /**
     * Gets the {@link Block} at the given location.
     * <p>
     * See {@link Location#getBlock()}.
     *
     * @return The block at this location.
     */
    @CheckReturnValue @Contract(pure = true)
    public Block getBlock()
    {
        return location.getBlock();
    }

    /**
     * Gets the bukkit location represented by this {@link ILocation}
     *
     * @return The Bukkit location.
     */
    @CheckReturnValue @Contract(pure = true)
    public Location getBukkitLocation()
    {
        return location.clone();
    }

    @Override
    @CheckReturnValue @Contract(pure = true)
    public String toString()
    {
        return getWorld() + ": " + getX() + ":" + getY() + ":" + getZ();
    }

    @Override
    @CheckReturnValue @Contract(pure = true)
    public boolean equals(@Nullable Object o)
    {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (getClass() != o.getClass())
            return false;
        final LocationSpigot other = (LocationSpigot) o;
        return location.equals(other.getBukkitLocation());
    }

    @Override
    @CheckReturnValue @Contract(pure = true)
    public int hashCode()
    {
        return location.hashCode();
    }

    @Override
    @CheckReturnValue @Contract(pure = true)
    public IWorld getWorld()
    {
        return world;
    }
}
