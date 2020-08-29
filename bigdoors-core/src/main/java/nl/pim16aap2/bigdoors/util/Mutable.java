package nl.pim16aap2.bigdoors.util;

import org.jetbrains.annotations.Nullable;

/**
 * Class used to wrap immutable objects and as such make them mutable.
 *
 * @author Pim
 */
public class Mutable<T>
{
    /**
     * The value stored in this {@link Mutable}.
     */
    private @Nullable T val;

    /**
     * Constructs a new {@link Mutable} with a given value.
     *
     * @param val The value to store.
     */
    public Mutable(@Nullable T val)
    {
        this.val = val;
    }

    /**
     * Gets the value stored in this {@link Mutable}.
     *
     * @return The value stored in this {@link Mutable}.
     */
    public @Nullable T getVal()
    {
        return val;
    }

    /**
     * Checks if the value stored in this {@link Mutable} is null.
     *
     * @return True if the value stored in this {@link Mutable} is null.
     */
    public boolean isEmpty()
    {
        return val == null;
    }

    /**
     * Sets the value stored in this {@link Mutable}
     *
     * @param val The value that will be stored in this {@link Mutable}.
     */
    public void setVal(@Nullable T val)
    {
        this.val = val;
    }
}
