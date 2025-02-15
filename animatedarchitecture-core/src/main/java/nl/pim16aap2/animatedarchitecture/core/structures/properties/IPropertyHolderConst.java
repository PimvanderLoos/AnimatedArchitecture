package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import nl.pim16aap2.animatedarchitecture.core.util.Util;

import java.util.Collection;
import java.util.List;
import java.util.SequencedCollection;

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
     * Gets the value of a required property.
     * <p>
     * If the property has no value set or the value is {@code null}, a {@link NullPointerException} is thrown.
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
    default <T> T getRequiredPropertyValue(Property<T> property)
    {
        return Util.requireNonNull(getPropertyValue(property).value(), property.getFullKey());
    }

    /**
     * Checks if the given property has a value set.
     *
     * @param property
     *     The property to check.
     * @return {@code true} if the property has a value set, {@code false} otherwise.
     */
    boolean hasProperty(Property<?> property);

    /**
     * Checks if this property holder has all the given properties.
     *
     * @param properties
     *     The properties to check.
     * @return {@code true} if this property holder has all the given properties, {@code false} otherwise.
     */
    default boolean hasProperties(Property<?>... properties)
    {
        return hasProperties(List.of(properties));
    }

    /**
     * Checks if this property holder has all the given properties.
     *
     * @param properties
     *     The properties to check.
     * @return {@code true} if this property holder has all the given properties, {@code false} otherwise.
     */
    default boolean hasProperties(SequencedCollection<Property<?>> properties)
    {
        if (properties.isEmpty())
            return true;

        if (properties.size() == 1)
            return hasProperty(properties.getFirst());

        return hasProperties((Collection<Property<?>>) properties);
    }

    /**
     * Checks if this property holder has all the given properties.
     *
     * @param properties
     *     The properties to check.
     * @return {@code true} if this property holder has all the given properties, {@code false} otherwise.
     */
    boolean hasProperties(Collection<Property<?>> properties);
}
