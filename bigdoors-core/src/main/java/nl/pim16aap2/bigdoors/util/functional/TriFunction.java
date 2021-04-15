package nl.pim16aap2.bigdoors.util.functional;


import lombok.NonNull;

/**
 * Represents a function with 3 input arguments.
 *
 * @author Pim
 */
@FunctionalInterface
public interface TriFunction<T, U, V, R>
{
    /**
     * Applies this function to the given arguments.
     *
     * @param t The first function argument.
     * @param u The second function argument.
     * @param v The third function argument.
     * @return The function result.
     */
    @NonNull
    R apply(T t, U u, V v);
}
