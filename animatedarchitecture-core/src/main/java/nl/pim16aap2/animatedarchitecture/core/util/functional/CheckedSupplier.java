package nl.pim16aap2.animatedarchitecture.core.util.functional;


import java.util.function.Supplier;

/**
 * Represents a {@link Supplier} that can throw an exception.
 *
 * @param <T>
 *     The type of results supplied by this supplier.
 * @param <E>
 *     The type of the exception thrown by the function.
 */
@FunctionalInterface
public interface CheckedSupplier<T, E extends Exception>
{
    /**
     * Returns the result of the method.
     *
     * @throws E
     *     An exception that occurred while performing the operation.
     */
    T get()
        throws E;
}
