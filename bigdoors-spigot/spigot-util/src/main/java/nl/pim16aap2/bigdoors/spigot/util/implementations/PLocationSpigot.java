package nl.pim16aap2.bigdoors.spigot.util.implementations;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3DdConst;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Represents an implementation of {@link IPLocation} for the Spigot platform.
 *
 * @author Pim
 */
public final class PLocationSpigot implements IPLocation
{
    @NonNull
    private Location location;

    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    @NonNull
    private IPWorld world;

    public PLocationSpigot(final @NonNull IPWorld world, final double x, final double y, final double z)
    {
        final @Nullable World bukkitWorld = world instanceof PWorldSpigot ?
                                            ((PWorldSpigot) world).getBukkitWorld() :
                                            Bukkit.getWorld(world.getWorldName());
        location = new Location(bukkitWorld, x, y, z);
        this.world = world;
    }

    public PLocationSpigot(final @NonNull Location location)
    {
        Objects.requireNonNull(location.getWorld());
        this.location = location.clone();
        world = new PWorldSpigot(location.getWorld());
    }

    @Override
    public @NonNull Vector2Di getChunk()
    {
        return new Vector2Di(location.getBlockX() << 4, location.getBlockZ() << 4);
    }

    @Override
    public int getBlockX()
    {
        return location.getBlockX();
    }

    @Override
    public int getBlockY()
    {
        return location.getBlockY();
    }

    @Override
    public int getBlockZ()
    {
        return location.getBlockZ();
    }

    @Override
    public double getX()
    {
        return location.getX();
    }

    @Override
    public double getY()
    {
        return location.getY();
    }

    @Override
    public double getZ()
    {
        return location.getZ();
    }

    @Override
    public void setX(double newVal)
    {
        location.setX(newVal);
    }

    @Override
    public void setY(double newVal)
    {
        location.setY(newVal);
    }

    @Override
    public void setZ(double newVal)
    {
        location.setZ(newVal);
    }

    @Override
    public @NonNull IPLocation add(final double x, final double y, final double z)
    {
        location.add(x, y, z);
        return this;
    }

    @Override
    public @NonNull IPLocation add(final @NonNull Vector3DiConst vector)
    {
        return add(vector.getX(), vector.getY(), vector.getZ());
    }

    @Override
    public @NonNull IPLocation add(final @NonNull Vector3DdConst vector)
    {
        return add(vector.getX(), vector.getY(), vector.getZ());
    }

    /**
     * Gets the bukkit location represented by this {@link IPLocation}
     *
     * @return The Bukkit location.
     */
    public @NonNull Location getBukkitLocation()
    {
        return location;
    }

    @Override
    public String toString()
    {
        return getWorld().toString() + ": " + getX() + ":" + getY() + ":" + getZ();
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
        PLocationSpigot other = (PLocationSpigot) o;
        return location.equals(other.getBukkitLocation());
    }

    @Override
    public int hashCode()
    {
        return location.hashCode();
    }

    @Override
    public @NonNull PLocationSpigot clone()
    {
        try
        {
            PLocationSpigot cloned = (PLocationSpigot) super.clone();
            cloned.location = location.clone();
            cloned.world = world.clone();
            return cloned;
        }
        catch (CloneNotSupportedException e)
        {
            Error er = new Error(e);
            BigDoors.get().getPLogger().logThrowableSilently(er);
            throw er;
        }
    }
}
