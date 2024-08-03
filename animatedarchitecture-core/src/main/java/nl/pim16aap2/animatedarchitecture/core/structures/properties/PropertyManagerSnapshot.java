package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Map;

/**
 * Represents a read-only snapshot of a property manager.
 * <p>
 * Instances of this class are immutable.
 * <p>
 * New instances can be created using {@link PropertyManager#snapshot()}.
 */
@ToString
@EqualsAndHashCode
@ThreadSafe
public final class PropertyManagerSnapshot implements IPropertyHolderConst, IPropertyManagerConst
{
    private final Map<String, IPropertyValue<?>> propertyMap;

    PropertyManagerSnapshot(Map<String, IPropertyValue<?>> propertyMap)
    {
        this.propertyMap = Map.copyOf(propertyMap);
    }

    @Override
    public <T> IPropertyValue<T> getPropertyValue(Property<T> property)
    {
        return PropertyManager.getValue(propertyMap, property);
    }

    @Override
    public boolean hasProperty(Property<?> property)
    {
        return propertyMap.containsKey(PropertyManager.mapKey(property));
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
