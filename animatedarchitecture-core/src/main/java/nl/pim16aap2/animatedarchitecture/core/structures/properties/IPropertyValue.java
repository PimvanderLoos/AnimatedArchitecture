package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import javax.annotation.Nullable;

/**
 * Represents the value of a property.
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
     * @return True if the value is set, false otherwise.
     */
    boolean isSet();

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