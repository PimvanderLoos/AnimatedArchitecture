package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import javax.annotation.Nullable;

/**
 * Represents a property-value pair.
 *
 * @param property
 *     The property.
 * @param value
 *     The value of the property.
 * @param <T>
 *     The type of the property.
 */
public record PropertyValuePair<T>(
    Property<T> property,
    IPropertyValue<T> value)
{
    /**
     * Creates a new property-value pair.
     *
     * @param property
     *     The property.
     * @param value
     *     The value of the property.
     * @return A new property-value pair.
     *
     * @throws IllegalArgumentException
     *     If the value is not of the correct type for the property.
     */
    public static <T> PropertyValuePair<?> of(Property<T> property, IPropertyValue<?> value)
    {
        final IPropertyValue<T> castedValue = PropertyContainer.cast(property, value);
        return new PropertyValuePair<>(property, castedValue);
    }

    /**
     * Shortcut for {@link #value()}.{@link IPropertyValue#value()}.
     *
     * @return The value of the property.
     */
    public @Nullable T propertyValue()
    {
        return value.value();
    }
}
