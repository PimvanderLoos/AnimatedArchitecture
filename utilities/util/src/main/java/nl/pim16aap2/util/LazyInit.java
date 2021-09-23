package nl.pim16aap2.util;

import lombok.EqualsAndHashCode;
import lombok.ToString;

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
    private volatile T obj;

    /**
     * @param supplier
     *     The supplier to use to create the instance of the object when needed.
     */
    public LazyInit(Supplier<T> supplier)
    {
        this.supplier = supplier;
    }

    /**
     * Gets the lazily initialized object. If it does not exist yet, a new instance will be created using {@link
     * #supplier}.
     *
     * @return The lazily initialized object.
     */
    public T get()
    {
        T tmp = obj;
        if (tmp != null)
            return tmp;

        synchronized (this)
        {
            tmp = obj;
            if (tmp == null)
                tmp = obj = supplier.get();
            return tmp;
        }
    }
}
