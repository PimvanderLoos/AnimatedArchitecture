package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
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
        return PropertyContainer.getValue(propertyMap, property);
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
}
