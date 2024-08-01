package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import javax.annotation.Nullable;

/**
 * Represents a read-only property manager.
 * <p>
 * This interface is used to get the values of properties from a property manager.
 */
public sealed interface IPropertyManagerConst permits PropertyManager, PropertyManagerSnapshot
{
    /**
     * Gets the value of the given property.
     *
     * @param property
     *     The property to get the value of.
     * @param <T>
     *     The type of the property.
     * @return The value of the property. May be {@code null} if the property is nullable.
     *
     * @throws IllegalArgumentException
     *     If the property is not set.
     */
    <T> @Nullable T getPropertyValue(Property<T> property);

    /**
     * Checks if the given property has a value set.
     *
     * @param property
     *     The property to check.
     * @return {@code true} if the property has a value set, {@code false} otherwise.
     */
    boolean hasProperty(Property<?> property);
}
