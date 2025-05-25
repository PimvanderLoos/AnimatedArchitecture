package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;

import javax.annotation.Nullable;

public interface IPropertyHolder extends IPropertyHolderConst
{
    /**
     * Sets the value of the given property.
     *
     * @param property
     *     The property to set the value for.
     * @param value
     *     The value to set.
     *     <p>
     *     If this is {@code null}, the property will be removed.
     * @param <T>
     *     The type of the property.
     * @return The previous value of the property, or {@code null} if the property had no value set.
     *
     * @throws IllegalArgumentException
     *     If the value is null and the property cannot be removed (i.e. it is specified by
     *     {@link StructureType#getProperties()}).
     */
    <T> IPropertyValue<T> setPropertyValue(Property<T> property, @Nullable T value);

    /**
     * Checks if a property can be removed from this property holder.
     * <p>
     * A property can not be removed under the following conditions:
     * <ul>
     *     <li>
     *         The property is required (e.g. defined by {@link StructureType#getProperties()}).
     *     </li>
     *     <li>
     *         The property does not exist in this property holder.
     *     </li>
     * </ul>
     *
     * @param property
     *     The property to check.
     * @return {@code true} if the property can be removed from this property holder, {@code false} otherwise.
     */
    boolean canRemoveProperty(Property<?> property);

    /**
     * Removes the given property from this property container.
     * <p>
     * Properties cannot be removed under certain conditions. When in doubt, check
     * {@link #canRemoveProperty(Property)}.
     *
     * @param property
     *     The property to remove.
     * @param <T>
     *     The type of the property.
     * @return The previous value of the property, or {@link PropertyContainer.UnsetPropertyValue#INSTANCE} if the
     * property was not set.
     *
     * @throws IllegalArgumentException
     *     Thrown if;
     *     <ul>
     *         <li>
     *             the property cannot be removed (i.e. it is specified by {@link StructureType#getProperties()}).
     *         </li>
     *         <li>
     *             the type of the removed property does not match the type of the property specified by the property.
     *         </li>
     *     </ul>
     */
    <T> IPropertyValue<T> removeProperty(Property<T> property)
        throws IllegalArgumentException;
}
