package nl.pim16aap2.bigdoors.util.functional;


import lombok.NonNull;

import java.util.function.Supplier;

/**
 * Represents a {@link Supplier} that can throw an exception.
 *
 * @param <T> The type of results supplied by this supplier.
 * @param <E> The type of the exception thrown by the function.
 * @author Pim
 */
@FunctionalInterface
public interface CheckedSupplier<T, E extends Exception>
{
    /**
     * Returns the result of the method.
     *
     * @throws E The exception that might be thrown.
     */
    @NonNull T get()
        throws E;
}
