package nl.pim16aap2.bigdoors.util.functional;


/**
 * Represents a consumer with 3 integer input arguments.
 *
 * @author Pim
 */
@FunctionalInterface
public interface TriIntConsumer
{
    /**
     * Applies this function to the given arguments.
     *
     * @param x
     *     The first function argument.
     * @param y
     *     The second function argument.
     * @param z
     *     The third function argument.
     */
    void accept(int x, int y, int z);
}
