package nl.pim16aap2.bigdoors.util;

import lombok.Getter;
import nl.pim16aap2.bigdoors.util.vector.IVector3DiConst;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a cuboid as described by 2 {@link Vector3Di}s.
 *
 * @author Pim
 */
public class Cuboid
{
    @NotNull
    @Getter
    private Vector3Di min, max;

    @Nullable
    private Integer volume = null;

    public Cuboid(final @NotNull Vector3Di min, final @NotNull Vector3Di max)
    {
        this.min = min;
        this.max = max;
        minMaxFix();
    }

    /**
     * Updates the lower bound coordinates of this {@link Cuboid}.
     * <p>
     * This also invalidates {@link #volume} and causes the min/max coordinates to be rebalanced.
     *
     * @param min The new
     */
    public void setMin(final @NotNull Vector3Di min)
    {
        this.min = min;
        minMaxFix();
        volume = null;
    }

    public void setMax(final @NotNull Vector3Di max)
    {
        this.max = max;
        minMaxFix();
        volume = null;
    }

    /**
     * Gets the total number of blocks in this cuboid.
     *
     * @return The total number of blocks in this cuboid.
     */
    public Integer getVolume()
    {
        if (volume != null)
            return volume;

        int x = max.getX() - min.getX() + 1;
        int y = max.getY() - min.getY() + 1;
        int z = max.getZ() - min.getZ() + 1;

        return volume = x * y * z;
    }

    /**
     * Checks if a position is inside this cuboid. This includes the edges.
     *
     * @param pos The position to check.
     * @return True if the position lies inside this cuboid (including the edges).
     */
    public boolean isPosInsideCuboid(final @NotNull IVector3DiConst pos)
    {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX() &&
            pos.getY() >= min.getY() && pos.getY() <= max.getY() &&
            pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    /**
     * Makes sure that min has the lowest x,y,z values and max the highest.
     */
    private void minMaxFix()
    {
        int minX = Math.min(min.getX(), max.getX());
        int minY = Math.min(min.getY(), max.getY());
        int minZ = Math.min(min.getZ(), max.getZ());

        int maxX = Math.max(min.getZ(), max.getX());
        int maxY = Math.max(min.getY(), max.getY());
        int maxZ = Math.max(min.getZ(), max.getZ());

        min.setX(minX);
        min.setY(minY);
        min.setZ(minZ);

        max.setX(maxX);
        max.setY(maxY);
        max.setZ(maxZ);
    }
}
