package nl.pim16aap2.bigdoors.util.functional;


import org.jetbrains.annotations.NotNull;

/**
 * Represents a function that throws an exception.
 *
 * @param <T> The type of the input to the function
 * @param <R> The type of the result of the function
 * @param <E> The type of the exception thrown by the function.
 * @author Pim
 */
@FunctionalInterface
public interface CheckedFunction<T, R, E extends Exception>
{
    /**
     * Applies this function to the given arguments.
     *
     * @param t The first function argument.
     * @return The function result.
     *
     * @throws E
     */
    @NotNull R apply(T t)
        throws E;
}
