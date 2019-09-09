package nl.pim16aap2.bigdoors.spigotutil.implementations;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Represents an implementation of {@link IPLocation} for the Spigot platform.
 *
 * @author Pim
 */
public final class PLocationSpigot implements IPLocation
{
    @NotNull
    private Location location;
    @NotNull
    private IPWorld world;

    public PLocationSpigot(final @NotNull IPWorld world, final double x, final double y, final double z)
    {
        final @Nullable World bukkitWorld = world instanceof PWorldSpigot ?
                                            ((PWorldSpigot) world).getBukkitWorld() : null;
        location = new Location(bukkitWorld, x, y, z);
        this.world = world;
    }

    public PLocationSpigot(final @NotNull Location location)
    {
        Objects.requireNonNull(location.getWorld());
        this.location = location;
        world = new PWorldSpigot(location.getWorld());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IPWorld getWorld()
    {
        return world;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Vector2Di getChunk()
    {
        return new Vector2Di(location.getBlockX() << 4, location.getBlockZ() << 4);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBlockX()
    {
        return location.getBlockX();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBlockY()
    {
        return location.getBlockY();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBlockZ()
    {
        return location.getBlockZ();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getX()
    {
        return location.getX();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getY()
    {
        return location.getY();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getZ()
    {
        return location.getZ();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setX(double newVal)
    {
        location.setX(newVal);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setY(double newVal)
    {
        location.setY(newVal);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setZ(double newVal)
    {
        location.setZ(newVal);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IPLocation add(final double x, final double y, final double z)
    {
        location.add(x, y, z);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IPLocation add(final @NotNull Vector3Di vector)
    {
        return add(vector.getX(), vector.getY(), vector.getZ());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IPLocation add(final @NotNull Vector3Dd vector)
    {
        return add(vector.getX(), vector.getY(), vector.getZ());
    }

    /**
     * Gets the bukkit location represented by this {@link IPLocation}
     *
     * @return The Bukkit location.
     */
    @NotNull
    public Location getBukkitLocation()
    {
        return location;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return getWorld().toString() + ": " + getX() + ":" + getY() + ":" + getZ();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toIntPositionString()
    {
        return String.format("(%d;%d;%d)", getBlockX(), getBlockY(), getBlockZ());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toDoublePositionString()
    {
        return String.format("(%.2f;%.2f;%.2f)", getX(), getY(), getZ());
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
        PLocationSpigot other = (PLocationSpigot) o;
        return location.equals(other.getBukkitLocation());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return location.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public PLocationSpigot clone()
    {
        try
        {
            PLocationSpigot cloned = (PLocationSpigot) super.clone();
            location = location.clone();
            world = world.clone();
            return cloned;
        }
        catch (CloneNotSupportedException e)
        {
            throw new Error(e);
        }
    }
}
