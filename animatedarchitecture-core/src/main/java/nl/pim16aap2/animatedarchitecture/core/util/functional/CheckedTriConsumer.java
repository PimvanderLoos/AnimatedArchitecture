package nl.pim16aap2.animatedarchitecture.core.util.functional;


/**
 * Represents a consumer with 3 input arguments.
 *
 * @param <T>
 *     The type of the first argument to the operation
 * @param <U>
 *     The type of the second argument to the operation
 * @param <V>
 *     The type of the third argument to the operation
 * @param <E>
 *     The type of the exception thrown by the function.
 */
@FunctionalInterface
public interface CheckedTriConsumer<T, U, V, E extends Exception>
{
    /**
     * Applies this function to the given arguments.
     *
     * @param t
     *     The first function argument.
     * @param u
     *     The second function argument.
     * @param v
     *     The third function argument.
     * @throws E
     *     An exception that occurred while performing the operation.
     */
    void accept(T t, U u, V v)
        throws E;
}
