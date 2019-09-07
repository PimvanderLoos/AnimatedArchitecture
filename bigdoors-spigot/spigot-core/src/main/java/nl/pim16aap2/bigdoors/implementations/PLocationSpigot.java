package nl.pim16aap2.bigdoors.implementations;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.spigotutil.IPLocationSpigot;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an implementation of {@link IPLocation} for the Spigot platform.
 *
 * @author Pim
 */
public class PLocationSpigot implements IPLocation, IPLocationSpigot
{
    @NotNull
    private final Location location;
    @NotNull
    private final IPWorld world;

    public PLocationSpigot(final @NotNull IPWorld world, final double x, final double y, final double z)
    {
        final @Nullable World bukkitWorld = world instanceof PWorldSpigot ?
                                            ((PWorldSpigot) world).getBukkitWorld() : null;
        location = new Location(bukkitWorld, x, y, z);
        this.world = world;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull IPWorld getWorld()
    {
        return world;
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
    public Location getBukkitLocation()
    {
        return location;
    }
}
