package nl.pim16aap2.bigdoors.util;

import nl.pim16aap2.bigdoors.util.functional.TriIntConsumer;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Iterator that iterates over all values between 2 {@link Vector3Di}s.
 *
 * @author Pim
 * @deprecated This class does not have any tests and is likely to be removed in the future!
 */
@Deprecated
public class PositionIterator implements Iterable<Vector3Di>
{
    private final Vector3Di posA;
    private final Vector3Di posB;
    private final IterationMode iterationMode;
    private final int dx, dy, dz;
    private final int volume;

    /**
     * Construct a new PositionIterator. When iterating between two values, the starting values are the first result and
     * the ending values are the last result. If the starting and ending values are the same, there will be a single
     * result.
     *
     * @param posA          The starting values.
     * @param posB          The ending values.
     * @param iterationMode The mode of iteration.
     * @param volume        The total number of blocks between posA and posB (inclusive). Do not use this if you do not
     *                      know this.
     */
    public PositionIterator(Vector3Di posA, Vector3Di posB, IterationMode iterationMode, int volume)
    {
        if (volume < 1)
            throw new IllegalArgumentException("Volume " + volume + " cannot be smaller than 1 block!");
        this.posA = posA;
        this.posB = posB;
        this.iterationMode = iterationMode;
        this.volume = volume;
        dx = posA.x() > posB.x() ? -1 : 1;
        dy = posA.y() > posB.y() ? -1 : 1;
        dz = posA.z() > posB.z() ? -1 : 1;
    }

    /**
     * Construct a new PositionIterator. When iterating between two values, the starting values are the first result and
     * the ending values are the last result. If the starting and ending values are the same, there will be a single
     * result.
     *
     * @param posA          The starting values.
     * @param posB          The ending values.
     * @param iterationMode The mode of iteration.
     */
    public PositionIterator(Vector3Di posA, Vector3Di posB, IterationMode iterationMode)
    {
        this(posA, posB, iterationMode, getVolume(posA, posB));
    }

    /**
     * Construct a new PositionIterator. When iterating between two values, the starting values are the first result and
     * the ending values are the last result. If the starting and ending values are the same, there will be a single
     * result.
     *
     * @param cuboid        The {@link Cuboid} to iterate over, from {@link Cuboid#getMin()} to {@link
     *                      Cuboid#getMax()}.
     * @param iterationMode The mode of iteration.
     */
    public PositionIterator(Cuboid cuboid, IterationMode iterationMode)
    {
        this(cuboid.getMin(), cuboid.getMax(), iterationMode, cuboid.getVolume());
    }

    private static int getVolume(Vector3Di posA, Vector3Di posB)
    {
        return new Cuboid(posA, posB).getVolume();
    }

    @Override
    public CustomIterator iterator()
    {
        return new CustomIterator(this);
    }

    /**
     * Works similar to {@link #forEach(Consumer)}, but with the exception that each step will be applied without
     * constructing a new {@link Vector3Di}.
     *
     * @param action The method to apply for each step.
     */
    public void forEach(TriIntConsumer action)
    {
        iterator().forEach(action);
    }

    /**
     * Iterator that iterates over all values between 2 {@link Vector3Di}s.
     */
    private class CustomIterator implements Iterator<Vector3Di>
    {
        private int x, y, z;
        private final Supplier<Boolean> outerLoop, middleLoop, innerLoop;
        private final Runnable resetMiddleLoop, resetInnerLoop;
        private int currentIndex = 0;

        private CustomIterator(PositionIterator locationIterator)
        {
            x = posA.x();
            y = posA.y();
            z = posA.z();
            outerLoop = getIncrementor(locationIterator.iterationMode.getIndex(0));
            middleLoop = getIncrementor(locationIterator.iterationMode.getIndex(1));
            innerLoop = getIncrementor(locationIterator.iterationMode.getIndex(2));

            resetMiddleLoop = getResetMethod(locationIterator.iterationMode.getIndex(1));
            resetInnerLoop = getResetMethod(locationIterator.iterationMode.getIndex(2));
        }

        private void forEach(TriIntConsumer action)
        {
            do action.accept(x, y, z);
            while (step());
        }

        /**
         * Gets the increment supplier for a specified {@link #PositionIterator ::Axis}.
         *
         * @param axis The {@link #PositionIterator ::Axis}.
         * @return The supplier that increments an {@link #PositionIterator ::Axis}.
         */
        @SuppressWarnings("NullAway") // Workaround for https://github.com/uber/NullAway/issues/289
        private Supplier<Boolean> getIncrementor(Axis axis)
        {
            return switch (axis)
                {
                    case X -> this::incrementX;
                    case Y -> this::incrementY;
                    case Z -> this::incrementZ;
                };
        }

        /**
         * Increments {@link #x} by a step of {@link #dx} if possible.
         * <p>
         * If no new values are available, i.e. {@link #x} == {@link Vector3Di#x()} for {@link #posB}, this method does
         * nothing other than returning false.
         *
         * @return True if the x axis could be updated, otherwise false.
         */
        private boolean incrementX()
        {
            if (x == posB.x())
                return false;

            x += dx;
            return true;
        }

        /**
         * Increments {@link #y} by a step of {@link #dy} if possible.
         * <p>
         * If no new values are available, i.e. {@link #y} == {@link Vector3Di#y()} for {@link #posB}, this method does
         * nothing other than returning false.
         *
         * @return True if the y axis could be updated, otherwise false.
         */
        private boolean incrementY()
        {
            if (y == posB.y())
                return false;

            y += dy;
            return true;
        }

        /**
         * Increments {@link #z} by a step of {@link #dz} if possible.
         * <p>
         * If no new values are available, i.e. {@link #z} == {@link Vector3Di#z()} for {@link #posB}, this method does
         * nothing other than returning false.
         *
         * @return True if the z axis could be updated, otherwise false.
         */
        private boolean incrementZ()
        {
            if (z == posB.z())
                return false;

            z += dz;
            return true;
        }

        /**
         * Gets the reset runnable for a specified {@link #PositionIterator ::Axis}.
         *
         * @param axis The {@link #PositionIterator ::Axis}.
         * @return The runnable that increments an {@link #PositionIterator ::Axis}.
         */
        @SuppressWarnings("NullAway") // Workaround for https://github.com/uber/NullAway/issues/289
        private Runnable getResetMethod(Axis axis)
        {
            return switch (axis)
                {
                    case X -> this::resetX;
                    case Y -> this::resetY;
                    case Z -> this::resetZ;
                };
        }

        /**
         * Resets the X axis to the starting value.
         */
        private void resetX()
        {
            x = posA.x();
        }

        /**
         * Resets the Y axis to the starting value.
         */
        private void resetY()
        {
            y = posA.y();
        }

        /**
         * Resets the Z axis to the starting value.
         */
        private void resetZ()
        {
            z = posA.z();
        }

        /**
         * Checks if the next element exists
         */
        @Override
        public boolean hasNext()
        {
            return currentIndex < volume;
        }

        /**
         * Moves the cursor/iterator to next element
         */
        @Override
        public Vector3Di next()
        {
            if (!step())
                throw new NoSuchElementException();
            return new Vector3Di(x, y, z);
        }

        private boolean step()
        {
            ++currentIndex;
            if (!innerLoop.get())
            {
                resetInnerLoop.run();
                if (!middleLoop.get())
                {
                    resetMiddleLoop.run();
                    return outerLoop.get();
                }
            }
            return true;
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

        IterationMode(Axis[] order)
        {
            this.order = order;
        }

        private Axis getIndex(int idx)
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
