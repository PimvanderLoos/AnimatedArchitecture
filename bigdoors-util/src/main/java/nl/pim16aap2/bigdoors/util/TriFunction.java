package nl.pim16aap2.bigdoors.util;

/**
 * Represents a function with 3 input arguments.
 *
 * @author Pim
 */
@FunctionalInterface
public interface TriFunction<T, U, V, R>
{
    R apply(T t, U u, V v);
}
