package nl.pim16aap2.animatedarchitecture.core.api;

/**
 * Represents an object that has a namespaced key.
 */
public interface IKeyed
{
    /**
     * Gets the namespaced key of this object.
     *
     * @return The namespaced key of this object.
     */
    NamespacedKey getNamespacedKey();

    /**
     * Gets the fully qualified key of this object.
     * <p>
     * This is a shortcut for {@code getNamespacedKey().getFullKey()}.
     *
     * @return The fully qualified key of this object.
     */
    default String getFullKey()
    {
        return getNamespacedKey().getFullKey();
    }
}
