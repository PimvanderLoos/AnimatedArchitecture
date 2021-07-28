package nl.pim16aap2.bigdoors.util;

import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IPLocationConst;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents a cuboid as described by 2 {@link Vector3Di}s.
 *
 * @author Pim
 */
public class CuboidConst
{
    protected Vector3Di min, max;

    /**
     * Gets the total number of blocks in this cuboid. It is inclusive of lower and upper bound. E.g. the volume of
     * [(1,1,1)(2,2,2)] = 8.
     *
     * @return The total number of blocks in this cuboid.
     */
    @Getter
    protected int volume;

    /**
     * Gets the dimensions of this door.
     *
     * @return The dimensions of this door.
     */
    @SuppressWarnings("NullAway.Init") @Getter
    private Vector3Di dimensions;

    public CuboidConst(final Vector3Di min, final Vector3Di max)
    {
        this.min = min;
        this.max = max;
        onCoordsUpdate();
    }

    public CuboidConst(final CuboidConst cuboidConst)
    {
        this(cuboidConst.min, cuboidConst.max);
    }

    /**
     * Makes sure that min has the lowest x,y,z values and max the highest.
     */
    protected void onCoordsUpdate()
    {
        final int minX = Math.min(min.x(), max.x());
        final int minY = Math.min(min.y(), max.y());
        final int minZ = Math.min(min.z(), max.z());

        final int maxX = Math.max(min.x(), max.x());
        final int maxY = Math.max(min.y(), max.y());
        final int maxZ = Math.max(min.z(), max.z());

        min = new Vector3Di(minX, minY, minZ);
        max = new Vector3Di(maxX, maxY, maxZ);

        volume = calculateVolume();
        dimensions = calculateDimensions();
    }

    private int calculateVolume()
    {
        int x = max.x() - min.x() + 1;
        int y = max.y() - min.y() + 1;
        int z = max.z() - min.z() + 1;

        return x * y * z;
    }

    /**
     * Gets the lower bound position.
     *
     * @return The lower bound position.
     */
    public @NotNull Vector3Di getMin()
    {
        return min;
    }

    /**
     * Gets the upper bound position.
     *
     * @return The upper bound position.
     */
    public @NotNull Vector3Di getMax()
    {
        return max;
    }

    private Vector3Di calculateDimensions()
    {
        int x = max.x() - min.x() + 1;
        int y = max.y() - min.y() + 1;
        int z = max.z() - min.z() + 1;
        return new Vector3Di(x, y, z);
    }

    /**
     * Checks if a position is inside this cuboid. This includes the edges.
     *
     * @param pos The position to check.
     * @return True if the position lies inside this cuboid (including the edges).
     */
    public boolean isPosInsideCuboid(final @NotNull Vector3Di pos)
    {
        return pos.x() >= min.x() && pos.x() <= max.x() &&
            pos.y() >= min.y() && pos.y() <= max.y() &&
            pos.z() >= min.z() && pos.z() <= max.z();
    }

    private static int getOuterDistance(final int test, final int min, final int max)
    {
        if (Util.between(test, min, max))
            return 0;
        if (test <= min)
            return min - test;
        return test - max;
    }

    /**
     * Checks if a position is within a certain range of the outer edge of the cuboid.
     * <p>
     * Any point inside the cuboid will always be within range.
     * <p>
     * For example, a range of 1 would include the cuboid itself as well as a 1 deep layer of blocks around it
     * (including corners).
     *
     * @param x     The x-coordinate to check.
     * @param y     The y-coordinate to check.
     * @param z     The z-coordinate to check.
     * @param range The range the position might be in. A range of 0 gives the same result as {@link
     *              #isPosInsideCuboid(Vector3Di)}.
     * @return True if the provided position lies within the range of this cuboid.
     */
    public boolean isInRange(final int x, int y, int z, final int range)
    {
        if (range < 0)
            throw new IllegalArgumentException("Range (" + range + ") cannot be smaller than 0!");

        return getOuterDistance(x, min.x(), max.x()) <= range &&
            getOuterDistance(y, min.y(), max.y()) <= range &&
            getOuterDistance(z, min.z(), max.z()) <= range;
    }

    /**
     * See {@link #isInRange(int, int, int, int)}
     */
    public boolean isInRange(final @NotNull Vector3Di pos, final int range)
    {
        return isInRange(pos.x(), pos.y(), pos.z(), range);
    }

    /**
     * See {@link #isInRange(int, int, int, int)}
     */
    public boolean isInRange(final @NotNull IPLocationConst loc, final int range)
    {
        return isInRange(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), range);
    }

    /**
     * Gets the center point of the cuboid.
     *
     * @return The center point of the cuboid.
     */
    public @NotNull Vector3Dd getCenter()
    {
        double cX = max.x() - ((max.x() - min.x()) / 2.0f);
        double cY = max.y() - ((max.y() - min.y()) / 2.0f);
        double cZ = max.z() - ((max.z() - min.z()) / 2.0f);
        return new Vector3Dd(cX, cY, cZ);
    }

    /**
     * Gets the center block of the cuboid. The results are cast to ints, basically taking the floor.
     *
     * @return The center block of the cuboid.
     */
    public @NotNull Vector3Di getCenterBlock()
    {
        int cX = (int) (max.x() - ((max.x() - min.x()) / 2.0f));
        int cY = (int) (max.y() - ((max.y() - min.y()) / 2.0f));
        int cZ = (int) (max.z() - ((max.z() - min.z()) / 2.0f));
        return new Vector3Di(cX, cY, cZ);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(min, max);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (!(o instanceof CuboidConst))
            return false;

        final CuboidConst other = (CuboidConst) o;
        return min.equals(other.min) && max.equals(other.max);
    }

    @Override
    public @NotNull Cuboid clone()
    {
        return new Cuboid(this);
    }
}
