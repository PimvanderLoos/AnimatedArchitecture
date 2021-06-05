package nl.pim16aap2.bigdoors.util;

import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IPLocationConst;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
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
    private Vector3DiConst dimensions;

    public CuboidConst(final Vector3DiConst min, final Vector3DiConst max)
    {
        this.min = new Vector3Di(min);
        this.max = new Vector3Di(max);
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

        volume = calculateVolume();
        dimensions = calculateDimensions();
    }

    private int calculateVolume()
    {
        int x = max.getX() - min.getX() + 1;
        int y = max.getY() - min.getY() + 1;
        int z = max.getZ() - min.getZ() + 1;

        return x * y * z;
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

    private Vector3DiConst calculateDimensions()
    {
        int x = max.getX() - min.getX() + 1;
        int y = max.getY() - min.getY() + 1;
        int z = max.getZ() - min.getZ() + 1;
        return new Vector3DiConst(x, y, z);
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
     *              #isPosInsideCuboid(Vector3DiConst)}.
     * @return True if the provided position lies within the range of this cuboid.
     */
    public boolean isInRange(final int x, int y, int z, final int range)
    {
        if (range < 0)
            throw new IllegalArgumentException("Range (" + range + ") cannot be smaller than 0!");

        return getOuterDistance(x, min.getX(), max.getX()) <= range &&
            getOuterDistance(y, min.getY(), max.getY()) <= range &&
            getOuterDistance(z, min.getZ(), max.getZ()) <= range;
    }

    /**
     * See {@link #isInRange(int, int, int, int)}
     */
    public boolean isInRange(final @NotNull Vector3DiConst pos, final int range)
    {
        return isInRange(pos.getX(), pos.getY(), pos.getZ(), range);
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
        return Objects.hash(min, max);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (!(o instanceof CuboidConst))
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
