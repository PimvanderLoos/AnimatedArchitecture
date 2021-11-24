package nl.pim16aap2.bigDoors.reflection;

/**
 * Represents a type of retrieval with certain limits on the number of items to be retrieved.
 *
 * @param <T> The type of the items to retrieve.
 * @param <U> The type of retriever.
 */
public interface IBoundedRetriever<T, U>
{
    /**
     * Configures the lower bound number of fields that have to be found.
     * <p>
     * For example, when this is set to 2 and only 1 field could be found with the current configuration, {@link
     * ReflectionFinder#get()} will either return null (when {@link ReflectionFinder#setNullable()} was used) or throw a
     * {@link IllegalStateException} (default).
     *
     * @param val
     *     The minimum number of fields that have to be found for this finder to be able to complete successfully.
     * @return The instance of the current finder.
     */
    U atLeast(int val);

    /**
     * Configures the upper bound number of fields that have to be found.
     * <p>
     * For example, when this is set to 2 and 3 fields could be found with the current configuration, {@link
     * ReflectionFinder#get()} will either return null (when {@link ReflectionFinder#setNullable()} was used) or throw a
     * {@link IllegalStateException} (default).
     *
     * @param val
     *     The maximum number of fields that can be found for this finder to be able to complete successfully.
     * @return The instance of the current finder.
     */
    U atMost(int val);

    /**
     * Configures the exact number of fields that have to be found.
     * <p>
     * For example, when this is set to 2 and 1 or 3 fields could be found with the current configuration, {@link
     * ReflectionFinder#get()} will either return null (when {@link ReflectionFinder#setNullable()} was used) or throw a
     * {@link IllegalStateException} (default).
     *
     * @param val
     *     The exact number of fields that must be found for this finder to be able to complete successfully.
     * @return The instance of the current finder.
     */
    U exactCount(int val);
}
