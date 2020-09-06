package nl.pim16aap2.bigdoors.util;

import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a cuboid as described by 2 {@link Vector3Di}s.
 *
 * @author Pim
 */
public class CuboidConst
{
    @NotNull
    protected Vector3Di min, max;

    @Nullable
    protected Integer volume = null;

    public CuboidConst(final @NotNull Vector3DiConst min, final @NotNull Vector3DiConst max)
    {
        this.min = new Vector3Di(min);
        this.max = new Vector3Di(max);
        minMaxFix();
    }

    public CuboidConst(final @NotNull CuboidConst cuboidConst)
    {
        this(cuboidConst.min, cuboidConst.max);
    }

    /**
     * Makes sure that min has the lowest x,y,z values and max the highest.
     */
    protected void minMaxFix()
    {
        int minX = Math.min(min.getX(), max.getX());
        int minY = Math.min(min.getY(), max.getY());
        int minZ = Math.min(min.getZ(), max.getZ());

        int maxX = Math.max(min.getX(), max.getX());
        int maxY = Math.max(min.getY(), max.getY());
        int maxZ = Math.max(min.getZ(), max.getZ());

        min.setX(minX);
        min.setY(minY);
        min.setZ(minZ);

        max.setX(maxX);
        max.setY(maxY);
        max.setZ(maxZ);
    }

    /**
     * Gets the lower bound position.
     *
     * @return The lower bound position.
     */
    public @NotNull Vector3DiConst getMin()
    {
        return min;
    }

    /**
     * Gets the upper bound position.
     *
     * @return The upper bound position.
     */
    public @NotNull Vector3DiConst getMax()
    {
        return max;
    }

    /**
     * Gets the total number of blocks in this cuboid. It is inclusive of lower and upper bound. E.g. the volume of
     * [(1,1,1)(2,2,2)] = 8.
     *
     * @return The total number of blocks in this cuboid.
     */
    public @NotNull Integer getVolume()
    {
        if (volume != null)
            return volume;

        int x = max.getX() - min.getX() + 1;
        int y = max.getY() - min.getY() + 1;
        int z = max.getZ() - min.getZ() + 1;

        return volume = x * y * z;
    }

    /**
     * Gets the dimensions of this door.
     *
     * @return The dimensions of this door.
     */
    public @NotNull Vector3Di getDimensions()
    {
        int x = max.getX() - min.getX() + 1;
        int y = max.getY() - min.getY() + 1;
        int z = max.getZ() - min.getZ() + 1;
        return new Vector3Di(x, y, z);
    }

    /**
     * Checks if a position is inside this cuboid. This includes the edges.
     *
     * @param pos The position to check.
     * @return True if the position lies inside this cuboid (including the edges).
     */
    public boolean isPosInsideCuboid(final @NotNull Vector3DiConst pos)
    {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX() &&
            pos.getY() >= min.getY() && pos.getY() <= max.getY() &&
            pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    /**
     * Gets the center point of the cuboid.
     *
     * @return The center point of the cuboid.
     */
    public @NotNull Vector3Dd getCenter()
    {
        double cX = max.getX() - ((max.getX() - min.getX()) / 2.0f);
        double cY = max.getY() - ((max.getY() - min.getY()) / 2.0f);
        double cZ = max.getZ() - ((max.getZ() - min.getZ()) / 2.0f);
        return new Vector3Dd(cX, cY, cZ);
    }

    /**
     * Gets the center block of the cuboid. The results are cast to ints, basically taking the floor.
     *
     * @return The center block of the cuboid.
     */
    public @NotNull Vector3Di getCenterBlock()
    {
        int cX = (int) (max.getX() - ((max.getX() - min.getX()) / 2.0f));
        int cY = (int) (max.getY() - ((max.getY() - min.getY()) / 2.0f));
        int cZ = (int) (max.getZ() - ((max.getZ() - min.getZ()) / 2.0f));
        return new Vector3Di(cX, cY, cZ);
    }

    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        CuboidConst other = (CuboidConst) o;
        return min.equals(other.min) && max.equals(other.max);
    }

    @Override
    public @NotNull Cuboid clone()
    {
        return new Cuboid(this);
    }
}
