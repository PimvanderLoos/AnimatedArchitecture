package nl.pim16aap2.animatedarchitecture.core.api;

import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;

/**
 * Represents a platform-specific adapter for displaying and interacting with a property in a GUI.
 * <p>
 * This is a minimal interface that allows platform-specific implementations to provide GUI representations for
 * properties without tying the core module to any specific platform implementation.
 *
 * @param <T>
 *     The type of the property value.
 */
public interface IPropertyGuiAdapter<T>
{
    /**
     * Gets the property this adapter is for.
     *
     * @return The property.
     */
    Property<T> getProperty();
}
