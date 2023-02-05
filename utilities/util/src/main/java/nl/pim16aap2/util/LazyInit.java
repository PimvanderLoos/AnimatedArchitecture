package nl.pim16aap2.util;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Represents a lazily initialized object.
 * <p>
 * Initialization is thread-safe.
 *
 * @param <T>
 *     The type of the object to initialize lazily.
 * @author Pim
 */
@ToString
@EqualsAndHashCode
public final class LazyInit<T>
{
    @ToString.Exclude
    private final Supplier<T> supplier;
    private volatile @Nullable T obj;

    /**
     * @param supplier
     *     The supplier to use to create the instance of the object when needed.
     */
    public LazyInit(Supplier<T> supplier)
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
        @Nullable T tmp = obj;
        if (tmp != null)
            return tmp;

        synchronized (this)
        {
            tmp = obj;
            if (tmp == null)
                tmp = obj = supplier.get();
            return Objects.requireNonNull(tmp, "Instance obtained from supplier must not be null!");
        }
    }
}
