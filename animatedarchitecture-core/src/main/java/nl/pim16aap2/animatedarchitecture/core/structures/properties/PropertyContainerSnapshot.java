package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

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
    @Getter(AccessLevel.PACKAGE)
    private final Map<String, IPropertyValue<?>> propertyMap;
    private final Set<PropertyValuePair<?>> propertySet;

    PropertyContainerSnapshot(Map<String, IPropertyValue<?>> propertyMap)
    {
        this.propertyMap = Collections.unmodifiableMap(new LinkedHashMap<>(propertyMap));
        this.propertySet = PropertyContainer.getNewPropertySet(propertyMap);
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
    public boolean canRemoveProperty(Property<?> property)
    {
        return false;
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
    public @NotNull Iterator<PropertyValuePair<?>> iterator()
    {
        return propertySet.iterator();
    }

    @Override
    public Spliterator<PropertyValuePair<?>> spliterator()
    {
        return propertySet.spliterator();
    }
}
