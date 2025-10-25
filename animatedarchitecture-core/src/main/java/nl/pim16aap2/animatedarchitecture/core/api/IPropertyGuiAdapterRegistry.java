package nl.pim16aap2.animatedarchitecture.core.api;

import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import org.jspecify.annotations.Nullable;

/**
 * Registry for property GUI adapters.
 * <p>
 * This allows platform-specific code and external plugins to register adapters that provide GUI representations for
 * properties.
 *
 * @param <T>
 *     The type of adapter this registry manages (must extend {@link IPropertyGuiAdapter}).
 */
public interface IPropertyGuiAdapterRegistry<T extends IPropertyGuiAdapter<?>> extends IDebuggable
{
    /**
     * Registers a GUI adapter for a property.
     *
     * @param adapter
     *     The adapter to register.
     */
    void registerAdapter(T adapter);

    /**
     * Gets the GUI adapter for the given property.
     *
     * @param property
     *     The property to get the adapter for.
     * @param <U>
     *     The type of the property value.
     * @return The adapter for the property, or null if no adapter is registered.
     */
    <U> @Nullable T getAdapter(Property<U> property);
}
