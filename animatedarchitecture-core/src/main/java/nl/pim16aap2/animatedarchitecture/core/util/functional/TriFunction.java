package nl.pim16aap2.animatedarchitecture.core.util.functional;


/**
 * Represents a function with 3 input arguments.
 */
@FunctionalInterface
public interface TriFunction<T, U, V, R>
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
     * @return The function result.
     */
    R apply(T t, U u, V v);
}
