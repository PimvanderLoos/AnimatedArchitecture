package nl.pim16aap2.animatedarchitecture.core.util.functional;

/**
 * Represents an operation that accepts a single {@code boolean}-valued argument and returns no result.
 */
@FunctionalInterface
public interface BooleanConsumer
{
    /**
     * Performs this operation on the given argument.
     *
     * @param value
     *     The input argument.
     */
    void accept(boolean value);
}
