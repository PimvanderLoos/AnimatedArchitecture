package nl.pim16aap2.bigdoors.util;

import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Supplier;

/**
 * Iterator that iterates over all values between 2 {@link Vector3Di}s.
 *
 * @author Pim
 */
public class PositionIterator implements Iterable<Vector3Di>
{
    @NotNull
    private final Vector3Di posA;
    @NotNull
    private final Vector3Di posB;
    @NotNull
    private final IterationMode iterationMode;
    private final int dx, dy, dz;

    /**
     * Construct a new PositionIterator. When iterating between two values, the starting values are the first result and
     * the ending values are the last result. If the starting and ending values are the same, there will be a single
     * result.
     *
     * @param posA          The starting values.
     * @param posB          The ending values.
     * @param iterationMode The mode of iteration.
     */
    public PositionIterator(final @NotNull Vector3Di posA, final @NotNull Vector3Di posB,
                            final @NotNull IterationMode iterationMode)
    {
        this.posA = posA;
        this.posB = posB;
        this.iterationMode = iterationMode;
        dx = posA.getX() > posB.getX() ? -1 : 1;
        dy = posA.getY() > posB.getY() ? -1 : 1;
        dz = posA.getZ() > posB.getZ() ? -1 : 1;
    }

    @NotNull
    @Override
    public Iterator<Vector3Di> iterator()
    {
        return new CustomIterator(this);
    }

    /**
     * Iterator that iterates over all values between 2 {@link Vector3Di}s.
     */
    private class CustomIterator implements Iterator<Vector3Di>
    {
        private int x, y, z;
        private final Supplier<Boolean> outerLoop, middleLoop, innerLoop;
        private boolean firstValue = true;

        private CustomIterator(final @NotNull PositionIterator locationIterator)
        {
            x = posA.getX();
            y = posA.getY();
            z = posA.getZ();
            outerLoop = getIncrementor(locationIterator.iterationMode.getIndex(0));
            middleLoop = getIncrementor(locationIterator.iterationMode.getIndex(1));
            innerLoop = getIncrementor(locationIterator.iterationMode.getIndex(2));
        }

        /**
         * Gets the increment supplier for a specified {@link #PositionIterator ::Axis}.
         *
         * @param axis The {@link #PositionIterator ::Axis}.
         * @return The supplier that increments an {@link #PositionIterator ::Axis}.
         */
        private Supplier<Boolean> getIncrementor(final Axis axis)
        {
            switch (axis)
            {
                case X:
                    return this::incrementX;
                case Y:
                    return this::incrementY;
                case Z:
                    return this::incrementZ;
            }
            return null;
        }

        /**
         * Increments {@link #x} by a step of {@link #dx}. If the value of X matches {@link #posB}.x, then it's reset to
         * {@link #posA}.x.
         *
         * @return True if the value of X was reset to {@link #posA}.x.
         */
        private boolean incrementX()
        {
            if (x == posB.getX())
            {
                x = posA.getX();
                return true;
            }
            x += dx;
            return false;
        }

        /**
         * Increments {@link #y} by a step of {@link #dy}. If the value of Y matches {@link #posB}.y, then it's reset to
         * {@link #posA}.y.
         *
         * @return True if the value of Y was reset to {@link #posA}.y.
         */
        private boolean incrementY()
        {
            if (y == posB.getY())
            {
                y = posA.getY();
                return true;
            }
            y += dy;
            return false;
        }

        /**
         * Increments {@link #z} by a step of {@link #dz}. If the value of Z matches {@link #posB}.z, then it's reset to
         * {@link #posA}.z.
         *
         * @return True if the value of Z was reset to {@link #posA}.z.
         */
        private boolean incrementZ()
        {
            if (z == posB.getZ())
            {
                z = posA.getZ();
                return true;
            }
            z += dz;
            return false;
        }

        /**
         * Checks if the next element exists
         */
        @Override
        public boolean hasNext()
        {
            return firstValue || !(x == posB.getX() && y == posB.getY() && z == posB.getZ());
        }

        /**
         * Moves the cursor/iterator to next element
         */
        @Override
        public Vector3Di next()
        {
            if (firstValue)
            {
                firstValue = false;
                return new Vector3Di(posA.getX(), posA.getY(), posA.getZ());
            }

            if (innerLoop.get())
                if (middleLoop.get())
                    outerLoop.get();
            return new Vector3Di(x, y, z);
        }
    }

    /**
     * The order of iteration. {@link #XYZ} would equate to an outer loop for X, a 'middle' loop for Y and an inner loop
     * for Z.
     */
    public enum IterationMode
    {
        XYZ(new Axis[]{Axis.X, Axis.Y, Axis.Z}),
        XZY(new Axis[]{Axis.X, Axis.Z, Axis.Y}),
        YXZ(new Axis[]{Axis.Y, Axis.X, Axis.Z}),
        YZX(new Axis[]{Axis.Y, Axis.Z, Axis.X}),
        ZXY(new Axis[]{Axis.Z, Axis.X, Axis.Y}),
        ZYX(new Axis[]{Axis.Z, Axis.Y, Axis.X}),
        ;

        private final Axis[] order;

        IterationMode(final Axis[] order)
        {
            this.order = order;
        }

        private Axis getIndex(final int idx)
        {
            return order[idx];
        }
    }

    private enum Axis
    {
        X,
        Y,
        Z
    }
}
