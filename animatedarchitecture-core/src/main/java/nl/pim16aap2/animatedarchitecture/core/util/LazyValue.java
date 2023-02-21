package nl.pim16aap2.animatedarchitecture.core.util;

import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.ThreadSafe;
import java.util.function.Supplier;

/**
 * Represents a wrapper class for lazily retrieved values.
 *
 * @param <T>
 *     The type of the data to store.
 */
@ThreadSafe
public final class LazyValue<T>
{
    private final Supplier<T> supplier;
    private volatile @Nullable T value;

    /**
     * @param supplier
     *     The supplier used to set the value. Note that the return value may not be null!
     */
    public LazyValue(Supplier<T> supplier)
    {
        this.supplier = supplier;
    }

    /**
     * Gets the lazily initialized value.
     * <p>
     * If the value has not been initialized yet, it will be initialized using {@link #supplier}.
     *
     * @return The lazily initialized value.
     *
     * @throws NullPointerException
     *     If the value returned by the {@link #supplier} is null.
     */
    public T get()
    {
        @Nullable T tmp = this.value;
        if (tmp == null)
        {
            synchronized (this)
            {
                tmp = this.value;
                if (tmp == null)
                    this.value = tmp = Util.requireNonNull(supplier.get(), "Lazily supplied value");
            }
        }
        return tmp;
    }

    /**
     * Invalidates the lazily initialized value (if it has been initialized yet).
     *
     * @return The previously-initialized value.
     */
    public @Nullable T invalidate()
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

    @Override
    public String toString()
    {
        return "LazyValue(value=" + this.value + ")";
    }
}
