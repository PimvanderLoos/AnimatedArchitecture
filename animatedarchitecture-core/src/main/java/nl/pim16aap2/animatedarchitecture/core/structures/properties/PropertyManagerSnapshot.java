package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.annotation.Nullable;
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
public final class PropertyManagerSnapshot implements IPropertyManagerConst
{
    private final Map<String, Object> propertyMap;

    PropertyManagerSnapshot(Map<String, Object> propertyMap)
    {
        this.propertyMap = Map.copyOf(propertyMap);
    }

    @Override
    public <T> @Nullable T getPropertyValue(Property<T> property)
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
    public Map<String, Object> getMap()
    {
        return propertyMap;
    }
}
