package nl.pim16aap2.bigdoors.util;

import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a cuboid as described by 2 {@link Vector3Di}s.
 *
 * @author Pim
 */
public class Cuboid extends CuboidConst
{
    public Cuboid(final @NotNull Vector3DiConst min, final @NotNull Vector3DiConst max)
    {
        super(min, max);
    }

    public Cuboid(final @NotNull CuboidConst cuboidConst)
    {
        this(cuboidConst.min, cuboidConst.max);
    }

    /**
     * Updates the coordinates of this {@link Cuboid}.
     * <p>
     * This also invalidates {@link #volume} and causes the min/max coordinates to be rebalanced.
     *
     * @param first  The first of the two new coordinates.
     * @param second The first of the two new coordinates.
     */
    public @NotNull Cuboid updatePositions(final @NotNull Vector3DiConst first, final @NotNull Vector3DiConst second)
    {
        min = new Vector3Di(first);
        max = new Vector3Di(second);
        onCoordsUpdate();
        return this;
    }

    /**
     * Moves this {@link Cuboid}.
     *
     * @param x The number of blocks to move in the x-axis.
     * @param y The number of blocks to move in the y-axis.
     * @param z The number of blocks to move in the z-axis.
     */
    public @NotNull Cuboid move(final int x, final int y, final int z)
    {
        min.add(x, y, z);
        max.add(x, y, z);
        return this;
    }

    /**
     * Changes the dimensions of this {@link Cuboid}. The changes are applied symmetrically. So a change of 1 in the
     * x-axis, means that the length of the x-axis is increased by 2 (1 subtracted from min and 1 added to max).
     *
     * @param x The number of blocks to change in the x-axis.
     * @param y The number of blocks to change in the y-axis.
     * @param z The number of blocks to change in the z-axis.
     */
    public @NotNull Cuboid changeDimensions(final int x, final int y, final int z)
    {
        min.add(-x, -y, -z);
        max.add(x, y, z);
        // Fix the min/max values to avoid issues with overlapping changes.
        onCoordsUpdate();
        return this;
    }

    @Override
    public @NotNull String toString()
    {
        return "{ " + min.toString() + " " + max.toString() + " }";
    }
}
