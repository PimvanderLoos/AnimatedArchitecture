package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Represents a read-only snapshot of a property container.
 * <p>
 * Instances of this class are immutable.
 * <p>
 * New instances can be created using {@link PropertyContainer#snapshot()}.
 */
@ToString
@EqualsAndHashCode
@ThreadSafe
public final class PropertyContainerSnapshot implements IPropertyHolderConst, IPropertyContainerConst
{
    private final Map<String, IPropertyValue<?>> propertyMap;

    PropertyContainerSnapshot(Map<String, IPropertyValue<?>> propertyMap)
    {
        this.propertyMap = Map.copyOf(propertyMap);
    }

    @Override
    public <T> IPropertyValue<T> getPropertyValue(Property<T> property)
    {
        final String key = PropertyContainer.mapKey(property);
        final var rawValue = propertyMap.getOrDefault(key, PropertyContainer.UnsetPropertyValue.INSTANCE);
        return PropertyContainer.cast(property, rawValue);
    }

    @Override
    public boolean hasProperty(Property<?> property)
    {
        return propertyMap.containsKey(PropertyContainer.mapKey(property));
    }

    @Override
    public boolean hasProperties(Collection<Property<?>> properties)
    {
        return PropertyContainer.hasProperties(propertyMap, properties);
    }

    /**
     * Gets the map of properties.
     *
     * @return An unmodifiable map of properties.
     */
    public Map<String, IPropertyValue<?>> getMap()
    {
        return propertyMap;
    }

    @Override
    public @NotNull Iterator<Map.Entry<String, IPropertyValue<?>>> iterator()
    {
        return propertyMap.entrySet().iterator();
    }
}
