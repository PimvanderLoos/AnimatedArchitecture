package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import javax.annotation.Nullable;

/**
 * Represents the immutable value of a property.
 *
 * @param <T>
 *     The type of the value.
 */
public sealed interface IPropertyValue<T>
    permits
    PropertyContainer.ProvidedPropertyValue,
    PropertyContainer.UnsetPropertyValue,
    PropertyContainerSerializer.UndefinedPropertyValue
{
    /**
     * Checks whether the value is set for this property.
     *
     * @return {@code true} if the value is set, {@code false} otherwise.
     */
    boolean isSet();

    /**
     * Checks if the property is removable.
     * <p>
     * When set to {@code false}, the property cannot be removed from the property container and will throw an exception
     * if an attempt is made to do so.
     *
     * @return {@code true} if the property is removable, {@code false} otherwise.
     */
    boolean isRemovable();

    /**
     * Gets the value of this property.
     * <p>
     * The value may be null if the property is not set or if the value is null.
     *
     * @return The value of this property.
     */
    @Nullable
    T value();

    /**
     * Gets the type of the value.
     * <p>
     * If the value is not set (see {@link #isSet()}), the type defaults to {@link Object}.
     *
     * @return The type of the value.
     */
    Class<T> type();
}
