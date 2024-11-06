package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import javax.annotation.Nullable;

public interface IPropertyHolder extends IPropertyHolderConst
{
    /**
     * Sets the value of the given property.
     *
     * @param property
     *     The property to set the value for.
     * @param value
     *     The value to set. May be {@code null} if the property is nullable.
     * @param <T>
     *     The type of the property.
     * @return The previous value of the property, or {@code null} if the property had no value set.
     *
     * @throws IllegalArgumentException
     *     If the property is not valid for the structure type this property container was created for.
     */
    <T> IPropertyValue<T> setPropertyValue(Property<T> property, @Nullable T value);
}
