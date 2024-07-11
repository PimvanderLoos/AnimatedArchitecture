package nl.pim16aap2.util;

import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Represents a lazily initialized object.
 * <p>
 * Initialization is thread-safe.
 *
 * @param <T>
 *     The type of the object to initialize lazily.
 */
@ThreadSafe
public final class LazyValue<T>
{
    private final Supplier<T> supplier;
    private volatile @Nullable T value;

    /**
     * @param supplier
     *     The supplier to use to create the instance of the object when needed.
     *     <p>
     *     Note that the return value may not be null!
     */
    public LazyValue(Supplier<T> supplier)
    {
        this.supplier = supplier;
    }

    /**
     * Gets the lazily initialized object. If it does not exist yet, a new instance will be created using
     * {@link #supplier}.
     *
     * @return The lazily initialized object.
     */
    public T get()
    {
        @Nullable T tmp = value;
        if (tmp != null)
            return tmp;

        synchronized (this)
        {
            tmp = value;
            if (tmp == null)
                tmp = value = supplier.get();
            return Objects.requireNonNull(tmp, "Instance obtained from supplier must not be null!");
        }
    }

    /**
     * Resets the lazily initialized value (if it has been initialized yet).
     *
     * @return The previously-initialized value or null if it was not initialized yet.
     */
    public @Nullable T reset()
    {
        @Nullable T tmp = this.value;
        if (tmp != null)
        {
            synchronized (this)
            {
                tmp = this.value;
                this.value = null;
            }
        }
        return tmp;
    }

    /**
     * Note that calling this method will result in {@link #get()} being called for both objects if the other object is
     * also a {@link LazyValue}.
     */
    @Override
    public boolean equals(final Object o)
    {
        if (o == this)
            return true;
        if (!(o instanceof LazyValue<?> other))
            return false;
        return Objects.equals(this.supplier, other.supplier) && Objects.equals(this.get(), other.get());
    }

    /**
     * Note that calling this method will result in {@link #get()} being called.
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(get(), this.supplier);
    }

    @Override
    public String toString()
    {
        return "LazyInit(obj=" + value + ")";
    }
}
