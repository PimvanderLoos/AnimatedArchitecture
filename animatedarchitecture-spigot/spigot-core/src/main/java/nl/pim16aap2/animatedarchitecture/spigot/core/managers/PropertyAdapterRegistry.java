package nl.pim16aap2.animatedarchitecture.spigot.core.managers;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.CustomLog;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.util.StringUtil;
import nl.pim16aap2.animatedarchitecture.spigot.core.propertyAdapter.AbstractPropertyAdapter;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a registry for property adapters.
 */
@CustomLog
@Singleton
public final class PropertyAdapterRegistry implements IDebuggable
{
    private final Map<String, AbstractPropertyAdapter<?>> adapters = new ConcurrentHashMap<>();

    @Inject
    public PropertyAdapterRegistry(DebuggableRegistry debuggableRegistry)
    {
        debuggableRegistry.registerDebuggable(this);
    }

    /**
     * Registers property adapters.
     *
     * @param adapters
     *     The property adapters to register.
     */
    public void registerAdapters(AbstractPropertyAdapter<?>... adapters)
    {
        for (AbstractPropertyAdapter<?> adapter : adapters)
        {
            registerAdapter(adapter);
        }
    }

    /**
     * Registers a property adapter.
     *
     * @param adapter
     *     The property adapter to register.
     */
    public void registerAdapter(AbstractPropertyAdapter<?> adapter)
    {
        final String propertyKey = adapter.getProperty().getFullKey();

        adapters.compute(propertyKey, (key, existingAdapter) ->
        {
            if (existingAdapter != null)
            {
                log.atWarn().log(
                    "Property GUI adapter for property '%s' is already registered. " +
                        "Replacing old adapter '%s' with new adapter '%s'.",
                    propertyKey,
                    existingAdapter,
                    adapter
                );
            }
            return adapter;
        });

        log.atInfo().log("Registered property GUI adapter for property '%s'", propertyKey);
    }

    /**
     * Gets the property adapter for the given property.
     *
     * @param property
     *     The property to get the adapter for.
     * @param <T>
     *     The type of the property value.
     * @return The property adapter, or null if no adapter is registered for the property.
     */
    public <T> @Nullable AbstractPropertyAdapter<T> getAdapter(Property<T> property)
    {
        //noinspection unchecked
        return (AbstractPropertyAdapter<T>) adapters.get(property.getFullKey());
    }

    @Override
    public String getDebugInformation()
    {
        return "Registered Property Adapters: " + StringUtil.formatCollection(adapters.values());
    }
}
