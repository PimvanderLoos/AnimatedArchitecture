package nl.pim16aap2.animatedarchitecture.core.api;

import lombok.CustomLog;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.util.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract base implementation of {@link IPropertyGuiAdapterRegistry}.
 *
 * @param <T>
 *     The type of adapter this registry manages (must extend {@link IPropertyGuiAdapter}).
 */
@CustomLog
public abstract class PropertyGuiAdapterRegistry<T extends IPropertyGuiAdapter<?>>
    implements IPropertyGuiAdapterRegistry<T>
{
    private final Map<String, T> adapters = new ConcurrentHashMap<>();

    @Override
    public void registerAdapter(T adapter)
    {
        final String propertyKey = adapter.getProperty().getFullKey();

        adapters.compute(propertyKey, (key, existingAdapter) ->
        {
            if (existingAdapter != null)
            {
                log.atWarn().log(
                    "Property GUI adapter for property '%s' is already registered. " +
                        "Replacing old adapter '%s' with new adapter '%s'.",
                    propertyKey, existingAdapter, adapter
                );
            }
            return adapter;
        });

        log.atInfo().log("Registered property GUI adapter for property '%s'", propertyKey);
    }

    @Override
    public <U> @Nullable T getAdapter(Property<U> property)
    {
        return adapters.get(property.getFullKey());
    }

    @Override
    public @Nullable String getDebugInformation()
    {
        return "Registered Property GUI Adapters: " + StringUtil.formatCollection(adapters.values());
    }
}
