package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.Util;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the properties of a structure.
 * <p>
 * New instances of this class can be created using {@link #forType(StructureType)}.
 * <p>
 * A property manager is created for a specific structure type. It contains all properties that are defined for that
 * type as defined by {@link StructureType#getProperties()}.
 * <p>
 * It is not possible to set a property that is not defined for the structure type this property manager was created
 * for. Attempting to do so will result in an {@link IllegalArgumentException}.
 */
@NotThreadSafe
@ToString
@Flogger
@EqualsAndHashCode
public final class PropertyManager implements IPropertyHolder, IPropertyManagerConst
{
    /**
     * The default property maps for each structure type.
     * <p>
     * The key is the name of the structure type. The value is a map containing the properties and their default
     * values.
     */
    private static final Map<StructureType, Map<String, IPropertyValue<?>>> DEFAULT_PROPERTY_MAPS =
        new ConcurrentHashMap<>();

    /**
     * The map that contains the properties and their values.
     * <p>
     * The key is defined by {@link #mapKey(Property)}. The value is the value of the property.
     */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private final Map<String, IPropertyValue<?>> propertyMap;

    /**
     * An unmodifiable view of {@link #propertyMap}.
     */
    private final Map<String, IPropertyValue<?>> unmodifiablePropertyMap;

    /**
     * Creates a new property manager with the given properties.
     *
     * @param propertyMap
     *     The properties to set.
     */
    PropertyManager(Map<String, IPropertyValue<?>> propertyMap)
    {
        this.propertyMap = propertyMap;
        this.unmodifiablePropertyMap = Collections.unmodifiableMap(propertyMap);
    }

    /**
     * Sets the value of the given property.
     *
     * @param property
     *     The property to set the value for.
     * @param value
     *     The value to set. May be {@code null} if the property is nullable.
     * @param <T>
     *     The type of the property.
     * @throws IllegalArgumentException
     *     If the property is not valid for the structure type this property manager was created for.
     */
    @Override
    public <T> void setPropertyValue(Property<T> property, @Nullable T value)
    {
        final String key = mapKey(property);
        if (!propertyMap.containsKey(key))
            throw new IllegalArgumentException("Property " + key + " is not valid for this structure type.");

        propertyMap.put(key, mapValue(property, value));
    }

    @Override
    public <T> IPropertyValue<T> getPropertyValue(Property<T> property)
    {
        return getValue(propertyMap, property);
    }

    @Override
    public boolean hasProperty(Property<?> property)
    {
        return propertyMap.containsKey(mapKey(property));
    }

    /**
     * Creates a new snapshot of this property manager.
     *
     * @return A new snapshot of this property manager.
     */
    public PropertyManagerSnapshot snapshot()
    {
        return new PropertyManagerSnapshot(unmodifiablePropertyMap);
    }

    /**
     * Creates a new property manager for the given structure type.
     *
     * @param structureType
     *     The structure type to create the property manager for.
     * @return A new property manager for the given structure type.
     */
    public static PropertyManager forType(StructureType structureType)
    {
        return new PropertyManager(new HashMap<>(getDefaultPropertyMap(structureType)));
    }

    /**
     * Gets the default property map for the given structure type.
     * <p>
     * Note that the returned map is unmodifiable.
     *
     * @param structureType
     *     The structure type to get the default property map for.
     * @return The default (unmodifiable) property map for the given structure type.
     */
    static Map<String, IPropertyValue<?>> getDefaultPropertyMap(StructureType structureType)
    {
        return DEFAULT_PROPERTY_MAPS.computeIfAbsent(
            Util.requireNonNull(structureType, "StructureType"),
            PropertyManager::newDefaultPropertyMap
        );
    }

    /**
     * Creates a new property map with the default properties for the given structure type.
     * <p>
     * Each property is set to its default value. See {@link Property#getDefaultValue()}.
     *
     * @param structureType
     *     The structure type to get the default properties for.
     * @return A new (unmodifiable) property map with the default properties for the given structure type.
     */
    static Map<String, IPropertyValue<?>> newDefaultPropertyMap(StructureType structureType)
    {
        final var typeProperties = structureType.getProperties();

        final Map<String, IPropertyValue<?>> defaultProperties = HashMap.newHashMap(typeProperties.size());
        structureType
            .getProperties()
            .forEach(property ->
                defaultProperties.put(
                    mapKey(property),
                    defaultMapValue(property)
                ));

        return Collections.unmodifiableMap(defaultProperties);
    }

    /**
     * Gets the value of the given property.
     *
     * @param propertyMap
     *     The map containing the properties.
     * @param property
     *     The property to get the value of.
     * @param <T>
     *     The type of the property.
     * @return The value of the property.
     */
    static <T> IPropertyValue<T> getValue(Map<String, IPropertyValue<?>> propertyMap, Property<T> property)
    {
        final @Nullable IPropertyValue<?> rawValue = getRawValue(propertyMap, property);
        //noinspection unchecked
        return (IPropertyValue<T>) Objects.requireNonNullElse(rawValue, UnsetPropertyValue.INSTANCE);
    }

    /**
     * Gets the raw value of the given property.
     *
     * @param propertyMap
     *     The map containing the properties.
     * @param property
     *     The property to get the raw value of.
     * @return The raw value of the property or {@code null} if the property is not set.
     */
    private static @Nullable IPropertyValue<?> getRawValue(
        Map<String, IPropertyValue<?>> propertyMap,
        Property<?> property)
    {
        return propertyMap.get(mapKey(property));
    }

    /**
     * Gets an unmodifiable view of the property map.
     *
     * @return An unmodifiable view of the property map.
     */
    Map<String, IPropertyValue<?>> getMap()
    {
        return unmodifiablePropertyMap;
    }

    /**
     * Gets the key for the given property.
     * <p>
     * The key is the name of the property's type.
     *
     * @param property
     *     The property to get the key for.
     * @return The key for the property.
     */
    static String mapKey(Property<?> property)
    {
        return property.getFullKey();
    }

    /**
     * Gets the default value for the given property.
     *
     * @param property
     *     The property to get the default value for.
     * @param <T>
     *     The type of the property.
     * @return The default value for the property.
     */
    static <T> ProvidedPropertyValue<T> defaultMapValue(Property<T> property)
    {
        return mapValue(property, property.getDefaultValue());
    }

    /**
     * Gets the map value for the given value.
     * <p>
     * If the value is {@code null}, this method returns {@link UnsetPropertyValue#INSTANCE}.
     *
     * @param value
     *     The value to convert.
     * @return {@link UnsetPropertyValue#INSTANCE} if the value is {@code null}, otherwise the value wrapped in a
     * {@link ProvidedPropertyValue}.
     */
    static <T> ProvidedPropertyValue<T> mapValue(Property<T> property, @Nullable T value)
    {
        return new ProvidedPropertyValue<>(property.getType(), value);
    }

    /**
     * Gets the map value for the given untyped value.
     * <p>
     * If the value is {@code null}, this method returns {@link UnsetPropertyValue#INSTANCE}.
     * <p>
     * If possible, consider using {@link #mapValue(Property, Object)} instead for better type safety.
     *
     * @param property
     *     The property to get the value for.
     * @param value
     *     The value to convert.
     * @param <T>
     *     The type of the property.
     * @return The value wrapped in a {@link ProvidedPropertyValue}.
     */
    static <T> ProvidedPropertyValue<T> mapUntypedValue(Property<T> property, @Nullable Object value)
    {
        return mapValue(property, property.cast(value));
    }

    /**
     * Represents a property value that is set.
     *
     * @param value
     *     The value of the property.
     * @param <T>
     *     The type of the property.
     */
    record ProvidedPropertyValue<T>(
        @JSONField(serialize = false) Class<T> type,
        @Nullable T value)
        implements IPropertyValue<T>
    {
        @JSONField(serialize = false)
        @Override
        public boolean isSet()
        {
            return true;
        }
    }

    /**
     * Represents a property value that is not set.
     */
    record UnsetPropertyValue() implements IPropertyValue<Object>
    {
        private static final UnsetPropertyValue INSTANCE = new UnsetPropertyValue();

        @Override
        public boolean isSet()
        {
            return false;
        }

        @Override
        public @Nullable Object value()
        {
            return null;
        }

        @Override
        public Class<Object> type()
        {
            return Object.class;
        }
    }
}
