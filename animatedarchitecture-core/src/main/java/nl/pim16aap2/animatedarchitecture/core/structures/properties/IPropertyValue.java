package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import javax.annotation.Nullable;

/**
 * Represents the value of a property.
 *
 * @param <T>
 *     The type of the value.
 */
public interface IPropertyValue<T>
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
    T get();
}
