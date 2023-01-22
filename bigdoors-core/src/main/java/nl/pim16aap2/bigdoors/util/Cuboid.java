package nl.pim16aap2.bigdoors.util;

import com.google.errorprone.annotations.CheckReturnValue;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.function.UnaryOperator;

/**
 * Represents a cuboid as described by 2 {@link Vector3Di}s.
 *
 * @author Pim
 */
@ToString
@EqualsAndHashCode
public final class Cuboid
{
    /**
     * Gets the lower bound position.
     */
    @Getter
    private final Vector3Di min;

    /**
     * Gets the upper bound position.
     */
    @Getter
    private final Vector3Di max;

    /**
     * Gets the total number of blocks in this cuboid. It is inclusive of lower and upper bound. E.g. the volume of
     * [(1,1,1)(2,2,2)] = 8.
     */
    @Getter
    @EqualsAndHashCode.Exclude
    private final int volume;

    /**
     * Gets the dimensions of this movable.
     */
    @Getter
    @EqualsAndHashCode.Exclude
    private final Vector3Di dimensions;

    public Cuboid(Vector3Di a, Vector3Di b)
    {
        final Vector3Di[] minmax = getSortedCoordinates(a, b);
        this.min = minmax[0];
        this.max = minmax[1];
        volume = calculateVolume();
        dimensions = calculateDimensions();
    }

    /**
     * Constructs a new cuboid from doubles.
     *
     * @param a
     *     The first position.
     * @param b
     *     The second position.
     * @param roundingMode
     *     The rounding mode to use when rounding doubles into integer values.
     * @return The new cuboid.
     */
    @CheckReturnValue @Contract(pure = true)
    public static Cuboid of(Vector3Dd a, Vector3Dd b, RoundingMode roundingMode)
    {
        final double xMin = Math.min(a.x(), b.x());
        final double yMin = Math.min(a.y(), b.y());
        final double zMin = Math.min(a.z(), b.z());

        final double xMax = Math.max(a.x(), b.x());
        final double yMax = Math.max(a.y(), b.y());
        final double zMax = Math.max(a.z(), b.z());

        @Nullable Vector3Di min = null;
        @Nullable Vector3Di max = null;
        switch (roundingMode)
        {
            case NEAREST:
            {
                min = new Vector3Di((int) Math.round(xMin), (int) Math.round(yMin), (int) Math.round(zMin));
                max = new Vector3Di((int) Math.round(xMax), (int) Math.round(yMax), (int) Math.round(zMax));
                break;
            }
            case INWARD:
            {
                min = new Vector3Di((int) Math.ceil(xMin), (int) Math.ceil(yMin), (int) Math.ceil(zMin));
                max = new Vector3Di((int) Math.floor(xMax), (int) Math.floor(yMax), (int) Math.floor(zMax));
                break;
            }
            case OUTWARD:
            {
                min = new Vector3Di((int) Math.floor(xMin), (int) Math.floor(yMin), (int) Math.floor(zMin));
                max = new Vector3Di((int) Math.ceil(xMax), (int) Math.ceil(yMax), (int) Math.ceil(zMax));
                break;
            }
        }
        return new Cuboid(Util.requireNonNull(min, "Minimum"), Util.requireNonNull(max, "Maximum"));
    }

    /**
     * Checks if a position is inside this cuboid. This includes the edges.
     *
     * @param pos
     *     The position to check.
     * @return True if the position lies inside this cuboid (including the edges).
     */
    @CheckReturnValue @Contract(pure = true)
    public boolean isPosInsideCuboid(Vector3Di pos)
    {
        return pos.x() >= min.x() && pos.x() <= max.x() &&
            pos.y() >= min.y() && pos.y() <= max.y() &&
            pos.z() >= min.z() && pos.z() <= max.z();
    }

    /**
     * Gets the distance of a test value to a range of two other values.
     * <p>
     * So for a test value of 5 and a range of 6 to 10, this distance would be 1.
     * <p>
     * If the test value exists within the provided range, 0 is returned.
     *
     * @param test
     *     The test value.
     * @param min
     *     The lower bound value of the range.
     * @param max
     *     The upper bound value of the range.
     * @return The distance between the test value and the provided range, or 0 if the test value lies on the provided
     * range.
     */
    @CheckReturnValue @Contract(pure = true)
    private static int getOuterDistance(int test, int min, int max)
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
     * @param x
     *     The x-coordinate to check.
     * @param y
     *     The y-coordinate to check.
     * @param z
     *     The z-coordinate to check.
     * @param range
     *     The range the position might be in. A range of 0 gives the same result as
     *     {@link #isPosInsideCuboid(Vector3Di)}.
     * @return True if the provided position lies within the range of this cuboid.
     */
    @CheckReturnValue @Contract(pure = true)
    public boolean isInRange(int x, int y, int z, int range)
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
    @CheckReturnValue @Contract(pure = true)
    public boolean isInRange(Vector3Di pos, int range)
    {
        return isInRange(pos.x(), pos.y(), pos.z(), range);
    }

    /**
     * See {@link #isInRange(int, int, int, int)}
     */
    @CheckReturnValue @Contract(pure = true)
    public boolean isInRange(IPLocation loc, int range)
    {
        return isInRange(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), range);
    }

    /**
     * Gets the center point of the cuboid.
     *
     * @return The center point of the cuboid.
     */
    @CheckReturnValue @Contract(pure = true)
    public Vector3Dd getCenter()
    {
        final double cX = (max.x() + min.x()) / 2.0f;
        final double cY = (max.y() + min.y()) / 2.0f;
        final double cZ = (max.z() + min.z()) / 2.0f;
        return new Vector3Dd(cX, cY, cZ);
    }

    /**
     * Gets the center block of the cuboid. The results are cast to ints, basically taking the floor.
     *
     * @return The center block of the cuboid.
     */
    @CheckReturnValue @Contract(pure = true)
    public Vector3Di getCenterBlock()
    {
        final int cX = (int) ((max.x() + min.x()) / 2.0f);
        final int cY = (int) ((max.y() + min.y()) / 2.0f);
        final int cZ = (int) ((max.z() + min.z()) / 2.0f);
        return new Vector3Di(cX, cY, cZ);
    }

    /**
     * Applies an update function to both {@link #min} and {@link #max}.
     *
     * @param updateFunction
     *     The update function used to update both {@link Vector3Di}s.
     * @return A new {@link Cuboid}.
     */
    @CheckReturnValue @Contract(pure = true)
    public Cuboid updatePositions(UnaryOperator<Vector3Di> updateFunction)
    {
        final Vector3Di newMin = updateFunction.apply(min);
        final Vector3Di newMax = updateFunction.apply(max);
        return new Cuboid(newMin, newMax);
    }

    /**
     * Moves this {@link Cuboid}.
     *
     * @param x
     *     The number of blocks to move in the x-axis.
     * @param y
     *     The number of blocks to move in the y-axis.
     * @param z
     *     The number of blocks to move in the z-axis.
     * @return A new {@link Cuboid}.
     */
    @CheckReturnValue @Contract(pure = true)
    public Cuboid move(int x, int y, int z)
    {
        return new Cuboid(min.add(x, y, z), max.add(x, y, z));
    }

    /**
     * Changes the dimensions of this {@link Cuboid}. The changes are applied symmetrically. So a change of 1 in the
     * x-axis, means that the length of the x-axis is increased by 2 (1 subtracted from min and 1 added to max).
     *
     * @param x
     *     The number of blocks to change in the x-axis.
     * @param y
     *     The number of blocks to change in the y-axis.
     * @param z
     *     The number of blocks to change in the z-axis.
     * @return A new {@link Cuboid}.
     */
    @CheckReturnValue @Contract(pure = true)
    public Cuboid grow(int x, int y, int z)
    {
        return new Cuboid(min.subtract(x, y, z), max.add(x, y, z));
    }

    @CheckReturnValue @Contract(pure = true)
    private static Vector3Di[] getSortedCoordinates(Vector3Di a, Vector3Di b)
    {
        final int minX = Math.min(a.x(), b.x());
        final int minY = Math.min(a.y(), b.y());
        final int minZ = Math.min(a.z(), b.z());

        final int maxX = Math.max(a.x(), b.x());
        final int maxY = Math.max(a.y(), b.y());
        final int maxZ = Math.max(a.z(), b.z());

        final Vector3Di min = new Vector3Di(minX, minY, minZ);
        final Vector3Di max = new Vector3Di(maxX, maxY, maxZ);
        return new Vector3Di[]{min, max};
    }

    @CheckReturnValue @Contract(pure = true)
    private int calculateVolume()
    {
        final int x = max.x() - min.x() + 1;
        final int y = max.y() - min.y() + 1;
        final int z = max.z() - min.z() + 1;
        return x * y * z;
    }

    @CheckReturnValue @Contract(pure = true)
    private Vector3Di calculateDimensions()
    {
        final int x = max.x() - min.x() + 1;
        final int y = max.y() - min.y() + 1;
        final int z = max.z() - min.z() + 1;
        return new Vector3Di(x, y, z);
    }

    /**
     * @return All 8 corners of this cuboid.
     */
    @CheckReturnValue @Contract(pure = true)
    public Vector3Di[] getCorners()
    {
        return new Vector3Di[]{
            min, new Vector3Di(min.x(), min.y(), max.z()),
            new Vector3Di(max.x(), min.y(), min.z()), new Vector3Di(max.x(), min.y(), max.z()),

            new Vector3Di(min.x(), max.y(), min.z()), new Vector3Di(min.x(), max.y(), max.z()),
            new Vector3Di(max.x(), max.y(), min.z()), max};
    }

    /**
     * @return The area described by this cuboid when you disregard the vertical (y) dimension.
     */
    public Rectangle asFlatRectangle()
    {
        return new Rectangle(new Vector2Di(min.x(), min.z()), new Vector2Di(max.x(), max.z()));
    }

    /**
     * Determines how to deal with double values.
     */
    public enum RoundingMode
    {
        /**
         * Rounds the double value to the nearest integer. E.g. 0.1 -> 0, and 0.9 -> 1.
         */
        NEAREST,

        /**
         * Rounds the double values towards the center of the cuboid.
         */
        INWARD,

        /**
         * Rounds the double values outward from the center of the cuboid.
         */
        OUTWARD
    }
}
