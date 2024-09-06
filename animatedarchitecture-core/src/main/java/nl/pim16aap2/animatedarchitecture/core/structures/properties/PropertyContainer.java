package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import org.jetbrains.annotations.VisibleForTesting;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages the properties of a structure.
 * <p>
 * New instances of this class can be created using {@link #forType(StructureType)}.
 * <p>
 * A property container is created for a specific structure type. It contains all properties that are defined for that
 * type as defined by {@link StructureType#getProperties()}.
 * <p>
 * It is not possible to set a property that is not defined for the structure type this property container was created
 * for. Attempting to do so will result in an {@link IllegalArgumentException}.
 */
@NotThreadSafe
@ToString
@Flogger
@EqualsAndHashCode
public final class PropertyContainer implements IPropertyHolder, IPropertyContainerConst
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
     * Creates a new property container with the given properties.
     *
     * @param propertyMap
     *     The properties to set.
     */
    PropertyContainer(Map<String, IPropertyValue<?>> propertyMap)
    {
        this.propertyMap = propertyMap;
        this.unmodifiablePropertyMap = Collections.unmodifiableMap(propertyMap);
    }

    /**
     * Sets the value of the given property.
     *
     * @param property
     *     The property to set the value for.
     * @param providedPropertyValue
     *     The value to set. May be {@code null} if the property is nullable.
     * @param <T>
     *     The type of the property.
     * @return The previous value of the property, or {@link UnsetPropertyValue#INSTANCE} if the property was not set.
     */
    private <T> IPropertyValue<?> setPropertyValue0(
        Property<T> property,
        ProvidedPropertyValue<T> providedPropertyValue)
    {
        final @Nullable IPropertyValue<?> prev = propertyMap.put(mapKey(property), providedPropertyValue);
        if (prev instanceof PropertyContainerSerializer.UndefinedPropertyValue undefinedPropertyValue)
        {
            log.atWarning().log(
                "Property '%s' was previously undefined. Overwriting with new value '%s'.",
                property.getFullKey(),
                providedPropertyValue.value()
            );
            return undefinedPropertyValue.deserializeValue(property);
        }
        return prev == null ? UnsetPropertyValue.INSTANCE : prev;
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
     *     If the property is not valid for the structure type this property container was created for.
     */
    @Override
    public <T> IPropertyValue<T> setPropertyValue(Property<T> property, @Nullable T value)
    {
        final IPropertyValue<?> prev = setPropertyValue0(property, mapValue(property, value));
        try
        {
            return cast(property, prev);
        }
        catch (IllegalArgumentException exception)
        {
            throw new IllegalArgumentException(
                String.format(
                    "Old value '%s' for property '%s' is not of the correct type! It has now been replaced by '%s'.",
                    prev,
                    property.getFullKey(),
                    value
                ),
                exception
            );
        }
    }

    /**
     * Sets the value of the given property without type checking.
     * <p>
     * If possible, consider using {@link #setPropertyValue(Property, Object)} instead for better type safety.
     *
     * @param property
     *     The property to set the value for.
     * @param value
     *     The value to set. May be {@code null} if the property is nullable.
     * @throws ClassCastException
     *     If the value cannot be cast to the type of the property.
     */
    public <T> IPropertyValue<?> setUntypedPropertyValue(Property<T> property, @Nullable Object value)
    {
        return setPropertyValue0(property, mapUntypedValue(property, value));
    }

    @Override
    public <T> IPropertyValue<T> getPropertyValue(Property<T> property)
    {
        return getValue(property);
    }

    @Override
    public boolean hasProperty(Property<?> property)
    {
        return propertyMap.containsKey(mapKey(property));
    }

    @Override
    public boolean hasProperties(Collection<Property<?>> properties)
    {
        return hasProperties(propertyMap, properties);
    }

    /**
     * Checks if the given property map has all the given properties.
     *
     * @param propertyMap
     *     The map to check.
     * @param properties
     *     The properties to check.
     * @return {@code true} if the property map has all the given properties, {@code false} otherwise.
     */
    static boolean hasProperties(Map<String, IPropertyValue<?>> propertyMap, Collection<Property<?>> properties)
    {
        return propertyMap
            .keySet()
            .containsAll(properties.stream().map(PropertyContainer::mapKey).collect(Collectors.toSet()));
    }

    /**
     * Creates a new snapshot of this property container.
     *
     * @return A new snapshot of this property container.
     */
    public PropertyContainerSnapshot snapshot()
    {
        return new PropertyContainerSnapshot(unmodifiablePropertyMap);
    }

    /**
     * Creates a new property container for the given structure type.
     *
     * @param structureType
     *     The structure type to create the property container for.
     * @return A new property container for the given structure type.
     */
    public static PropertyContainer forType(StructureType structureType)
    {
        return new PropertyContainer(new HashMap<>(getDefaultPropertyMap(structureType)));
    }

    /**
     * Creates a new property container for the given properties.
     * <p>
     * This method is intended for testing purposes only.
     * <p>
     * For production code, use {@link #forType(StructureType)} instead.
     *
     * @param properties
     *     The properties to create the property container for.
     * @return A new property container for the given properties.
     */
    @VisibleForTesting
    public static PropertyContainer forProperties(List<Property<?>> properties)
    {
        return new PropertyContainer(toPropertyMap(properties));
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
            PropertyContainer::newDefaultPropertyMap
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
        return Collections.unmodifiableMap(toPropertyMap(structureType.getProperties()));
    }

    /**
     * Creates a property map from the given list of properties.
     * <p>
     * Each property is set to its default value. See {@link Property#getDefaultValue()}.
     *
     * @param properties
     *     The properties to create the map from.
     * @return A new property map with the default properties for the given structure type.
     */
    static Map<String, IPropertyValue<?>> toPropertyMap(List<Property<?>> properties)
    {
        return properties
            .stream()
            .collect(Collectors.toMap(
                PropertyContainer::mapKey,
                PropertyContainer::defaultMapValue,
                (prev, next) -> next,
                HashMap::new)
            );
    }

    /**
     * Casts the given value to the type of the property.
     *
     * @param property
     *     The property to cast the value for.
     * @param value
     *     The value to cast.
     * @param <T>
     *     The type of the property value.
     * @return The value cast to the type of the property.
     *
     * @throws IllegalArgumentException
     *     If the provided value is not of the correct type for the property.
     */
    static <T> IPropertyValue<T> cast(Property<T> property, IPropertyValue<?> value)
    {
        if (value instanceof PropertyContainerSerializer.UndefinedPropertyValue undefinedPropertyValue)
            return undefinedPropertyValue.deserializeValue(property);

        if (value instanceof UnsetPropertyValue)
            // Safe to case because it always returns null.
            //noinspection unchecked
            return (IPropertyValue<T>) UnsetPropertyValue.INSTANCE;

        if (value.type() != property.getType())
        {
            throw new IllegalArgumentException(String.format(
                "Property '%s' is of type '%s', but the value is of type '%s'.",
                property.getFullKey(),
                property.getType().getSimpleName(),
                value.type().getSimpleName()
            ));
        }
        // Safe to case because the type is checked above.
        //noinspection unchecked
        return (IPropertyValue<T>) value;
    }

    /**
     * Gets the value of the given property.
     * <p>
     * If the property is not set, this method returns {@link UnsetPropertyValue#INSTANCE}.
     * <p>
     * If the key maps to an {@link PropertyContainerSerializer.UndefinedPropertyValue}, this method will deserialize
     * the value and replace the value in the map.
     *
     * @param property
     *     The property to get the value of.
     * @param <T>
     *     The type of the property.
     * @return The value of the property.
     */
    <T> IPropertyValue<T> getValue(Property<T> property)
    {
        final String key = mapKey(property);
        final IPropertyValue<?> rawValue = propertyMap.getOrDefault(key, UnsetPropertyValue.INSTANCE);

        if (rawValue instanceof PropertyContainerSerializer.UndefinedPropertyValue undefinedPropertyValue)
        {
            final var newValue = undefinedPropertyValue.deserializeValue(property);
            propertyMap.put(key, newValue);
            return newValue;
        }

        return cast(property, Objects.requireNonNull(rawValue));
    }

    /**
     * Gets the raw value mapped to the given key.
     * <p>
     * This method is intended for testing purposes only. Use {@link #getValue(Property)} instead.
     *
     * @param key
     *     The key to get the value for.
     * @return The raw value mapped to the given key, or {@code null} if no value is mapped to the key.
     */
    @VisibleForTesting
    @Nullable
    IPropertyValue<?> getRawValue(String key)
    {
        return propertyMap.get(key);
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
     *
     * @throws ClassCastException
     *     If the value cannot be cast to the type of the property.
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
        static final UnsetPropertyValue INSTANCE = new UnsetPropertyValue();

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
