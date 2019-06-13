package nl.pim16aap2.bigdoors.util;

import java.util.Objects;
import java.util.function.Function;

/**
 * Represents a function with 3 input arguments.
 *
 * @author Pim
 */
@FunctionalInterface
public interface TriFunction<A, B, C, R>
{
    R apply(A a, B b, C c);

    default <V> TriFunction<A, B, C, V> andThen(Function<? super R, ? extends V> after)
    {
        Objects.requireNonNull(after);
        return (A a, B b, C c) -> after.apply(apply(a, b, c));
    }
}
