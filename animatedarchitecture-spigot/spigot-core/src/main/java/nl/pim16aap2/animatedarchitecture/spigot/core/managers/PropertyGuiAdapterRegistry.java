package nl.pim16aap2.animatedarchitecture.spigot.core.managers;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.CustomLog;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.util.StringUtil;
import nl.pim16aap2.animatedarchitecture.spigot.core.propertyadapters.guiadapters.AbstractPropertyGuiAdapter;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a registry for property GUI adapters.
 */
@CustomLog
@Singleton
public final class PropertyGuiAdapterRegistry implements IDebuggable
{
    private final Map<String, AbstractPropertyGuiAdapter<?>> adapters = new ConcurrentHashMap<>();

    @Inject
    PropertyGuiAdapterRegistry(DebuggableRegistry debuggableRegistry)
    {
        debuggableRegistry.registerDebuggable(this);
    }

    /**
     * Registers property GUI adapters.
     *
     * @param adapters
     *     The property GUI adapters to register.
     */
    public void registerGuiAdapters(AbstractPropertyGuiAdapter<?>... adapters)
    {
        for (final AbstractPropertyGuiAdapter<?> adapter : adapters)
        {
            registerGuiAdapter(adapter);
        }
    }

    /**
     * Registers a property GUI adapter.
     *
     * @param adapter
     *     The property GUI adapter to register.
     */
    public void registerGuiAdapter(AbstractPropertyGuiAdapter<?> adapter)
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
     * Gets the property GUI adapter for the given property.
     *
     * @param property
     *     The property to get the GUI adapter for.
     * @param <T>
     *     The type of the property value.
     * @return The property GUI adapter, or null if no adapter is registered for the property.
     */
    public <T> @Nullable AbstractPropertyGuiAdapter<T> getGuiAdapter(Property<T> property)
    {
        //noinspection unchecked
        return (AbstractPropertyGuiAdapter<T>) adapters.get(property.getFullKey());
    }

    @Override
    public String getDebugInformation()
    {
        return "Registered Property Adapters: " + StringUtil.formatCollection(adapters.values());
    }
}
