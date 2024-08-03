package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import nl.pim16aap2.animatedarchitecture.core.util.Util;

/**
 * Represents a read-only property holder.
 */
public interface IPropertyHolderConst
{
    /**
     * Gets the value of the given property.
     *
     * @param property
     *     The property to get the value of.
     * @param <T>
     *     The type of the property.
     * @return The value of the property.
     */
    <T> IPropertyValue<T> getPropertyValue(Property<T> property);

    /**
     * Gets the raw value of the given property.
     *
     * @param property
     *     The property to get the value of.
     * @param <T>
     *     The type of the property.
     * @return The raw value of the property.
     *
     * @throws NullPointerException
     *     If the property has no value set or the value is {@code null}.
     */
    default <T> T getRawPropertyValue(Property<T> property)
    {
        return Util.requireNonNull(getPropertyValue(property).value(), "Raw Value of Property " + property);
    }

    /**
     * Checks if the given property has a value set.
     *
     * @param property
     *     The property to check.
     * @return {@code true} if the property has a value set, {@code false} otherwise.
     */
    boolean hasProperty(Property<?> property);
}
