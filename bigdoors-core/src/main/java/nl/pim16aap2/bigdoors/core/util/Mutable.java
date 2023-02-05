package nl.pim16aap2.bigdoors.core.util;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Wrapper class with a mutable reference to an object.
 * <p>
 * This class makes no assumptions on the nullability of objects.
 * <p>
 * This class is not thread safe. When such functionality is required, use an alternative like {@link AtomicReference}
 * instead.
 *
 * @param <T>
 *     The type of the object to store.
 */
@SuppressWarnings({"unused", "NullableProblems"})
@NotThreadSafe
@ToString
@EqualsAndHashCode
public class Mutable<T>
{
    /**
     * The value stored in this {@link Mutable}.
     */
    private T val;

    /**
     * Constructs a new {@link Mutable} with a given value.
     *
     * @param val
     *     The value to store.
     */
    public Mutable(T val)
    {
        this.val = val;
    }

    /**
     * @return The value stored in this {@link Mutable}.
     */
    public T get()
    {
        return val;
    }

    /**
     * Sets the value stored in this {@link Mutable}
     *
     * @param val
     *     The value that will be stored in this {@link Mutable}.
     */
    public void set(T val)
    {
        this.val = val;
    }
}
