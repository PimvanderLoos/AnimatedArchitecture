package nl.pim16aap2.bigdoors.util.Functional;

/**
 * Represents a consumer with 3 input arguments.
 *
 * @author Pim
 */
@FunctionalInterface
public interface TriConsumer<T, U, V>
{
    /**
     * Applies this function to the given arguments.
     *
     * @param t The first function argument.
     * @param u The second function argument.
     * @param v The third function argument.
     */
    void accept(T t, U u, V v);
}
