package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import org.jetbrains.annotations.VisibleForTesting;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the properties of a structure.
 * <p>
 * New instances of this class can be created using {@link #forType(StructureType)} and
 * {@link #forType(StructureType, Map)}.
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
public final class PropertyManager implements IPropertyManagerConst
{
    /**
     * The default property maps for each structure type.
     * <p>
     * The key is the name of the structure type. The value is a map containing the properties and their default
     * values.
     */
    private static final Map<StructureType, Map<String, Object>> DEFAULT_PROPERTY_MAPS = new ConcurrentHashMap<>();

    /**
     * The value that is used to represent a {@code null} value.
     * <p>
     * This is used to differentiate between a {@code null} value and a value that is not set.
     */
    @VisibleForTesting
    static final NullObject NULL_VALUE = NullObject.INSTANCE;

    /**
     * The map that contains the properties and their values.
     * <p>
     * The key is defined by {@link #mapKey(Property)}. The value is the value of the property.
     */
    private final Map<String, Object> propertyMap;

    /**
     * An unmodifiable view of {@link #propertyMap}.
     */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final Map<String, Object> unmodifiablePropertyMap;

    /**
     * Creates a new property manager with the given properties.
     *
     * @param propertyMap
     *     The properties to set.
     */
    private PropertyManager(Map<String, Object> propertyMap)
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
    public <T> void setProperty(Property<T> property, @Nullable T value)
    {
        final String key = mapKey(property);
        if (!propertyMap.containsKey(key))
            throw new IllegalArgumentException("Property " + key + " is not valid for this structure type.");

        propertyMap.put(key, mapValue(value));
    }

    @Override
    public <T> @Nullable T getPropertyValue(Property<T> property)
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
        return new PropertyManagerSnapshot(propertyMap);
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
     * Creates a new property manager with the given properties.
     * <p>
     * The returned property manager will contain all properties that are defined in the given structure type.
     * <p>
     * If the provided map does not contain a value for a property, the default value of the property will be used and a
     * warning will be logged.
     * <p>
     * If the provided map contains a value for a property that is not defined in the structure type, a warning will be
     * logged and the property will be ignored.
     *
     * @param structureType
     *     The structure type to get the properties for.
     * @param valueMap
     *     The map containing the values for the properties.
     * @return A new property manager with all properties defined in the structure type and the values from the provided
     * map if available.
     */
    public static PropertyManager forType(StructureType structureType, Map<String, Object> valueMap)
    {
        return new PropertyManager(getPropertiesMap(structureType, valueMap));
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
    static Map<String, Object> getDefaultPropertyMap(StructureType structureType)
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
    static Map<String, Object> newDefaultPropertyMap(StructureType structureType)
    {
        final var typeProperties = structureType.getProperties();

        final Map<String, Object> defaultProperties = HashMap.newHashMap(typeProperties.size());
        structureType
            .getProperties()
            .forEach(property ->
                defaultProperties.put(
                    mapKey(property),
                    mapValue(property.getDefaultValue())
                ));

        return Collections.unmodifiableMap(defaultProperties);
    }

    /**
     * Creates a new property map with the given properties.
     * <p>
     * The returned map will contain all properties that are defined in the given structure type.
     * <p>
     * If the provided map does not contain a value for a property, the default value of the property will be used and a
     * warning will be logged.
     * <p>
     * If the provided map contains a value for a property that is not defined in the structure type, a warning will be
     * logged and the property will be ignored.
     * <p>
     * If the provided values map contains all properties defined in the structure type, the provided map will be
     * returned as-is.
     *
     * @param structureType
     *     The structure type to get the properties for.
     * @param providedValues
     *     The map containing the values for the properties.
     * @return A property map with all properties defined in the structure type and the values from the provided map if
     * available.
     */
    static Map<String, Object> getPropertiesMap(StructureType structureType, Map<String, Object> providedValues)
    {
        final Map<String, Object> defaultProperties = getDefaultPropertyMap(structureType);
        if (defaultProperties.keySet().equals(providedValues.keySet()))
            return providedValues;

        defaultProperties
            .keySet()
            .stream()
            .filter(key -> !providedValues.containsKey(key))
            .forEach(key -> log.atWarning().log(
                "Property %s was not supplied for structure type %s, using default value.",
                key,
                structureType
            ));

        final Map<String, Object> mergedProperties = new HashMap<>(defaultProperties);

        for (final var entry : providedValues.entrySet())
        {
            final var key = entry.getKey();
            final var value = entry.getValue();

            if (defaultProperties.containsKey(key))
                mergedProperties.put(key, value);
            else
            {
                log.atWarning().log(
                    "Property %s is not supported by structure type %s, ignoring it.",
                    key,
                    structureType
                );
            }
        }

        return mergedProperties;
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
     * @return The value of the property. May be {@code null} if the property is nullable.
     */
    static <T> @Nullable T getValue(Map<String, Object> propertyMap, Property<T> property)
    {
        final @Nullable Object rawValue = getRawValue(propertyMap, property);
        if (rawValue == null)
            throw new IllegalArgumentException("Requesting value for unset property: " + mapKey(property));

        if (rawValue == NULL_VALUE)
            return null;

        return property.cast(rawValue);
    }

    /**
     * Gets the raw value of the given property.
     *
     * @param propertyMap
     *     The map containing the properties.
     * @param property
     *     The property to get the raw value of.
     * @return The raw value of the property or {@code null} if the property is not set. May be {@link #NULL_VALUE} if
     * the property is nullable and set to {@code null}.
     */
    private static @Nullable Object getRawValue(Map<String, Object> propertyMap, Property<?> property)
    {
        return propertyMap.get(mapKey(property));
    }

    /**
     * Gets an unmodifiable view of the property map.
     *
     * @return An unmodifiable view of the property map.
     */
    Map<String, Object> getMap()
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
        return property.getType().getName();
    }

    /**
     * Gets the map value for the given value.
     * <p>
     * If the value is {@code null}, this method returns {@link #NULL_VALUE}. Otherwise, it returns the value itself.
     *
     * @param value
     *     The value to convert.
     * @return {@link #NULL_VALUE} if the value is {@code null}, otherwise the value itself.
     */
    private static Object mapValue(@Nullable Object value)
    {
        return Objects.requireNonNullElse(value, NULL_VALUE);
    }

    /**
     * Represents a {@code null} object that can be used to represent a {@code null} value in a map.
     * <p>
     * Used to differentiate between a {@code null} value and a value that is not set.
     */
    static final class NullObject implements Serializable
    {
        public static final NullObject INSTANCE = new NullObject();

        private NullObject()
        {
            // Private constructor to prevent instantiation
        }

        @Override
        public String toString()
        {
            return "NullObject";
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj instanceof NullObject;
        }

        @Override
        public int hashCode()
        {
            return 0;
        }
    }
}
