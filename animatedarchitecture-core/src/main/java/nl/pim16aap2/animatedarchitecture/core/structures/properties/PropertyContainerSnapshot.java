package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;

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
    public static final PropertyContainerSnapshot EMPTY = new PropertyContainerSnapshot(Map.of());

    @Getter(AccessLevel.PACKAGE)
    private final Map<String, IPropertyValue<?>> propertyMap;
    @Getter(AccessLevel.PACKAGE)
    private final Set<PropertyValuePair<?>> propertySet;

    public PropertyContainerSnapshot(Map<String, IPropertyValue<?>> propertyMap)
    {
        this.propertyMap = Collections.unmodifiableMap(new LinkedHashMap<>(propertyMap));
        this.propertySet = PropertyContainer.getNewPropertySet(this.propertyMap);
    }

    public PropertyContainerSnapshot(Collection<PropertyValuePair<?>> properties)
    {
        final Map<String, IPropertyValue<?>> map = LinkedHashMap.newLinkedHashMap(properties.size());
        for (final var pair : properties)
        {
            map.put(PropertyContainer.mapKey(pair.property()), pair.value());
        }
        this.propertyMap = Collections.unmodifiableMap(map);
        this.propertySet = Set.copyOf(properties);
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

    @Override
    public int propertyCount()
    {
        return propertyMap.size();
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
    public Iterator<PropertyValuePair<?>> iterator()
    {
        return propertySet.iterator();
    }

    @Override
    public Spliterator<PropertyValuePair<?>> spliterator()
    {
        return propertySet.spliterator();
    }
}
