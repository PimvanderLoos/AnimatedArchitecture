package nl.pim16aap2.animatedarchitecture.core.util;

import com.google.errorprone.annotations.CheckReturnValue;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector2Dd;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector2Di;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.function.UnaryOperator;

/**
 * Represents a rectangle as described by 2 {@link Vector2Di}s.
 *
 * @author Pim
 */
@ToString
@EqualsAndHashCode
public final class Rectangle
{
    /**
     * Gets the lower bound position.
     */
    @Getter
    private final Vector2Di min;

    /**
     * Gets the upper bound position.
     */
    @Getter
    private final Vector2Di max;

    /**
     * Gets the total number of blocks in this rectangle.
     */
    @Getter
    @EqualsAndHashCode.Exclude
    private final int surface;

    /**
     * Gets the dimensions of this rectangle.
     */
    @Getter
    @EqualsAndHashCode.Exclude
    private final Vector2Di dimensions;

    public Rectangle(Vector2Di a, Vector2Di b)
    {
        final Vector2Di[] minmax = getSortedCoordinates(a, b);
        this.min = minmax[0];
        this.max = minmax[1];
        surface = calculateSurface();
        dimensions = calculateDimensions();
    }

    /**
     * Constructs a new rectangle from doubles.
     *
     * @param a
     *     The first position.
     * @param b
     *     The second position.
     * @param roundingMode
     *     The rounding mode to use when rounding doubles into integer values.
     * @return The new rectangle.
     */
    @CheckReturnValue @Contract(pure = true)
    public static Rectangle of(Vector2Dd a, Vector2Dd b, RoundingMode roundingMode)
    {
        final double xMin = Math.min(a.x(), b.x());
        final double yMin = Math.min(a.y(), b.y());

        final double xMax = Math.max(a.x(), b.x());
        final double yMax = Math.max(a.y(), b.y());

        @Nullable Vector2Di min = null;
        @Nullable Vector2Di max = null;
        switch (roundingMode)
        {
            case NEAREST ->
            {
                min = new Vector2Di(MathUtil.round(xMin), MathUtil.round(yMin));
                max = new Vector2Di(MathUtil.round(xMax), MathUtil.round(yMax));
            }
            case INWARD ->
            {
                min = new Vector2Di(MathUtil.ceil(xMin), MathUtil.ceil(yMin));
                max = new Vector2Di(MathUtil.floor(xMax), MathUtil.floor(yMax));
            }
            case OUTWARD ->
            {
                min = new Vector2Di(MathUtil.floor(xMin), MathUtil.floor(yMin));
                max = new Vector2Di(MathUtil.ceil(xMax), MathUtil.ceil(yMax));
            }
        }
        return new Rectangle(Util.requireNonNull(min, "Minimum"), Util.requireNonNull(max, "Maximum"));
    }

    /**
     * Checks if a position is inside this rectangle. This includes the edges.
     *
     * @param pos
     *     The position to check.
     * @return True if the position lies inside this rectangle (including the edges).
     */
    @CheckReturnValue @Contract(pure = true)
    public boolean isPosInsideRectangle(Vector2Di pos)
    {
        return pos.x() >= min.x() && pos.x() <= max.x() &&
            pos.y() >= min.y() && pos.y() <= max.y();
    }

    /**
     * @return The center point of the rectangle.
     */
    @CheckReturnValue @Contract(pure = true)
    public Vector2Dd getCenter()
    {
        final double cX = (max.x() + min.x()) / 2.0f;
        final double cY = (max.y() + min.y()) / 2.0f;
        return new Vector2Dd(cX, cY);
    }

    /**
     * Gets the center block of the rectangle. The results are cast to ints, basically taking the floor.
     *
     * @return The center block of the rectangle.
     */
    @CheckReturnValue @Contract(pure = true)
    public Vector2Di getCenterBlock()
    {
        final int cX = Math.floorDiv(max.x() + min.x(), 2);
        final int cY = Math.floorDiv(max.y() + min.y(), 2);
        return new Vector2Di(cX, cY);
    }

    /**
     * Applies an update function to both {@link #min} and {@link #max}.
     *
     * @param updateFunction
     *     The update function used to update both {@link Vector2Di}s.
     * @return A new {@link Rectangle}.
     */
    @CheckReturnValue @Contract(pure = true)
    public Rectangle updatePositions(UnaryOperator<Vector2Di> updateFunction)
    {
        final Vector2Di newMin = updateFunction.apply(min);
        final Vector2Di newMax = updateFunction.apply(max);
        return new Rectangle(newMin, newMax);
    }

    /**
     * Moves this {@link Rectangle}.
     *
     * @param x
     *     The number of blocks to move in the x-axis.
     * @param y
     *     The number of blocks to move in the y-axis.
     * @return A new {@link Rectangle}.
     */
    @CheckReturnValue @Contract(pure = true)
    public Rectangle move(int x, int y)
    {
        return new Rectangle(min.add(x, y), max.add(x, y));
    }

    /**
     * Changes the dimensions of this {@link Rectangle}. The changes are applied symmetrically. So a change of 1 in the
     * x-axis, means that the length of the x-axis is increased by 2 (1 subtracted from min and 1 added to max).
     *
     * @param x
     *     The number of blocks to change in the x-axis.
     * @param y
     *     The number of blocks to change in the y-axis.
     * @return A new {@link Rectangle}.
     */
    @CheckReturnValue @Contract(pure = true)
    public Rectangle grow(int x, int y)
    {
        return new Rectangle(min.subtract(x, y), max.add(x, y));
    }

    @CheckReturnValue @Contract(pure = true)
    private static Vector2Di[] getSortedCoordinates(Vector2Di a, Vector2Di b)
    {
        final int minX = Math.min(a.x(), b.x());
        final int minY = Math.min(a.y(), b.y());

        final int maxX = Math.max(a.x(), b.x());
        final int maxY = Math.max(a.y(), b.y());

        final Vector2Di min = new Vector2Di(minX, minY);
        final Vector2Di max = new Vector2Di(maxX, maxY);
        return new Vector2Di[]{min, max};
    }

    @CheckReturnValue @Contract(pure = true)
    private int calculateSurface()
    {
        final int x = max.x() - min.x() + 1;
        final int y = max.y() - min.y() + 1;
        return x * y;
    }

    @CheckReturnValue @Contract(pure = true)
    private Vector2Di calculateDimensions()
    {
        final int x = max.x() - min.x() + 1;
        final int y = max.y() - min.y() + 1;
        return new Vector2Di(x, y);
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
         * Rounds the double values towards the center of the rectangle.
         */
        INWARD,

        /**
         * Rounds the double values outward from the center of the rectangle.
         */
        OUTWARD
    }
}

