package nl.pim16aap2.bigdoors.spigot.util.implementations;

import com.google.common.flogger.StackSize;
import com.google.errorprone.annotations.CheckReturnValue;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.api.IPLocation;
import nl.pim16aap2.bigdoors.core.api.IPWorld;
import nl.pim16aap2.bigdoors.core.util.Util;
import nl.pim16aap2.bigdoors.core.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Di;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an implementation of {@link IPLocation} for the Spigot platform.
 *
 * @author Pim
 */
@Flogger
public final class PLocationSpigot implements IPLocation
{
    private final Location location;

    private final IPWorld world;

    public PLocationSpigot(IPWorld ipWorld, @Nullable World bukkitWorld, double x, double y, double z)
    {
        location = new Location(bukkitWorld, x, y, z);
        world = ipWorld;
    }

    public PLocationSpigot(IPWorld world, double x, double y, double z)
    {
        final @Nullable World bukkitWorld = retrieveBukkitWorld(world);
        if (bukkitWorld == null)
            log.atFine().withStackTrace(StackSize.FULL).log("Bukkit world of world '%s' is null!", world);
        location = new Location(bukkitWorld, x, y, z);
        this.world = world;
    }

    public PLocationSpigot(World world, double x, double y, double z)
    {
        location = new Location(world, x, y, z);
        this.world = new PWorldSpigot(world);
    }

    public PLocationSpigot(Location location)
    {
        Util.requireNonNull(location.getWorld(), "world of location " + location);
        this.location = location.clone();
        world = new PWorldSpigot(location.getWorld());
    }

    /**
     * Attempts to retrieve a Bukkit World object from an IPWorld object.
     * <p>
     * If the IPWorld object is a PWorldSpigot subclass, the world is retrieved from its member variable. Otherwise its
     * name is used to retrieve it from Bukkit.
     *
     * @param ipWorld
     *     The {@link IPWorld}.
     * @return The Bukkit world, if it could be found.
     */
    @CheckReturnValue @Contract(pure = true)
    private static @Nullable World retrieveBukkitWorld(IPWorld ipWorld)
    {
        if (ipWorld instanceof PWorldSpigot ipWorldSpigot)
            return ipWorldSpigot.getBukkitWorld();
        return Bukkit.getWorld(ipWorld.worldName());
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
    public IPLocation setX(double newVal)
    {
        return new PLocationSpigot(world, location.getWorld(), newVal, getY(), getZ());
    }

    @Override
    @CheckReturnValue @Contract(pure = true)
    public IPLocation setY(double newVal)
    {
        return new PLocationSpigot(world, location.getWorld(), getX(), newVal, getZ());
    }

    @Override
    @CheckReturnValue @Contract(pure = true)
    public IPLocation setZ(double newVal)
    {
        return new PLocationSpigot(world, location.getWorld(), getX(), getY(), newVal);
    }

    @Override
    @CheckReturnValue @Contract(pure = true)
    public IPLocation add(double x, double y, double z)
    {
        return new PLocationSpigot(world, location.getWorld(), getX() + x, getY() + y, getZ() + z);
    }

    @Override
    @CheckReturnValue @Contract(pure = true)
    public IPLocation add(Vector3Di vector)
    {
        return add(vector.x(), vector.y(), vector.z());
    }

    @Override
    @CheckReturnValue @Contract(pure = true)
    public IPLocation add(Vector3Dd vector)
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
     * Gets the bukkit location represented by this {@link IPLocation}
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
        final PLocationSpigot other = (PLocationSpigot) o;
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
    public IPWorld getWorld()
    {
        return world;
    }
}
