package nl.pim16aap2.bigdoors.core.util.functional;


import java.util.function.Supplier;

/**
 * Represents a {@link Supplier} that can throw an exception.
 *
 * @param <T>
 *     The type of results supplied by this supplier.
 * @param <E>
 *     The type of the exception thrown by the function.
 * @author Pim
 */
@FunctionalInterface
public interface CheckedSupplier<T, E extends Exception>
{
    /**
     * Returns the result of the method.
     *
     * @throws E
     *     The exception that might be thrown.
     */
    T get()
        throws E;
}
