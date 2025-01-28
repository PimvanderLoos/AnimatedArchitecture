package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.util.LazyValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
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
     * A representation of all properties in this container with their values for all valid properties.
     * <p>
     * This is a lazily initialized cache that is reset whenever the property map is modified.
     */
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private final LazyValue<Set<PropertyValuePair<?>>> propertySet;

    /**
     * The map that contains the properties and their values.
     * <p>
     * The key is defined by {@link #mapKey(Property)}. The value is the value of the property.
     */
    // Exclude this map from equals and toString because we already use the unmodifiable map for those instead.
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
        this.propertySet = new LazyValue<>(() -> getNewPropertySet(this.unmodifiablePropertyMap));
    }

    /**
     * Creates a copy of the given property container.
     *
     * @param other
     *     The property container to copy.
     * @return A new property container with the same properties as the given property container.
     */
    public static PropertyContainer of(IPropertyContainerConst other)
    {
        if (other instanceof PropertyContainer propertyContainer)
            return new PropertyContainer(new HashMap<>(propertyContainer.propertyMap));
        else if (other instanceof PropertyContainerSnapshot snapshot)
            return new PropertyContainer(new HashMap<>(snapshot.getPropertyMap()));
        else
            return new PropertyContainer(
                other.stream().collect(
                    Collectors.toMap(
                        val -> mapKey(val.property()),
                        PropertyValuePair::value)));
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
        propertySet.reset();

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
        if (properties.isEmpty())
            return true;

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
            propertySet.reset();
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

    @Override
    public @NotNull Iterator<PropertyValuePair<?>> iterator()
    {
        return propertySet.get().iterator();
    }

    @Override
    public Spliterator<PropertyValuePair<?>> spliterator()
    {
        return propertySet.get().spliterator();
    }

    /**
     * Creates a new set of {@link PropertyValuePair}s from the provided property map.
     * <p>
     * Use {@link #propertySet} to get the cached value.
     *
     * @param propertyMap
     *     The property map to create the set from.
     * @return A new set of {@link PropertyValuePair}s.
     */
    static Set<PropertyValuePair<?>> getNewPropertySet(Map<String, IPropertyValue<?>> propertyMap)
    {
        return propertyMap
            .entrySet()
            .stream()
            .map(entry ->
            {
                final @Nullable Property<?> property = Property.fromName(entry.getKey());
                return property == null ? null : PropertyValuePair.of(property, entry.getValue());
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Creates a new {@link PropertyContainer} from the given properties.
     *
     * @param properties
     *     A group of interleaved property-value pairs.
     *     <p>
     *     The even indices must be non-null {@link Property}s.
     *     <p>
     *     The odd indices are the (nullable) values for the properties.
     * @return A new {@link PropertyContainer} with the given properties.
     *
     * @throws IllegalArgumentException
     *     If the properties are not provided in pairs of 2.
     *     <p>
     *     If an object at an even index is not a {@link Property}.
     * @throws NullPointerException
     *     If a property is {@code null}.
     * @throws ClassCastException
     *     If the value of a property cannot be cast to the type of the preceding property using
     *     {@link Property#cast(Object)}.
     */
    public static PropertyContainer of(@Nullable Object @Nullable ... properties)
    {
        if (properties == null)
            return new PropertyContainer(HashMap.newHashMap(0));

        if (properties.length % 2 != 0)
            throw new IllegalArgumentException("Properties must be provided in pairs of 2.");

        final PropertyContainer ret = new PropertyContainer(HashMap.newHashMap(properties.length / 2));
        for (int idx = 0; idx < properties.length; idx += 2)
        {
            final Object untypedProperty = Util.requireNonNull(
                properties[idx],
                "Property at index " + idx
            );

            final Property<?> property;
            try
            {
                property = (Property<?>) untypedProperty;
            }
            catch (ClassCastException exception)
            {
                throw new IllegalArgumentException(
                    "Expected object at index " + idx + " to be a Property, but it was " + untypedProperty.getClass(),
                    exception
                );
            }

            final @Nullable Object value = properties[idx + 1];
            ret.setUntypedPropertyValue(property, value);
        }

        return ret;
    }

    /**
     * Type-safe version of {@link #of(Object...)} for 1 property.
     */
    public static <T0> PropertyContainer of(
        Property<T0> property0, @Nullable T0 value0
    )
    {
        return of(
            property0, (Object) value0
        );
    }

    /**
     * Type-safe version of {@link #of(Object...)} for 2 properties.
     */
    public static <T0, T1> PropertyContainer of(
        Property<T0> property0, @Nullable T0 value0,
        Property<T1> property1, @Nullable T1 value1
    )
    {
        return of(
            property0, value0,
            property1, (Object) value1
        );
    }

    /**
     * Type-safe version of {@link #of(Object...)} for 3 properties.
     */
    public static <T0, T1, T2> PropertyContainer of(
        Property<T0> property0, @Nullable T0 value0,
        Property<T1> property1, @Nullable T1 value1,
        Property<T2> property2, @Nullable T2 value2
    )
    {
        return of(
            property0, value0,
            property1, value1,
            property2, (Object) value2
        );
    }

    /**
     * Type-safe version of {@link #of(Object...)} for 4 properties.
     */
    public static <T0, T1, T2, T3> PropertyContainer of(
        Property<T0> property0, @Nullable T0 value0,
        Property<T1> property1, @Nullable T1 value1,
        Property<T2> property2, @Nullable T2 value2,
        Property<T3> property3, @Nullable T3 value3
    )
    {
        return of(
            property0, value0,
            property1, value1,
            property2, value2,
            property3, (Object) value3
        );
    }

    /**
     * Type-safe version of {@link #of(Object...)} for 5 properties.
     */
    public static <T0, T1, T2, T3, T4> PropertyContainer of(
        Property<T0> property0, @Nullable T0 value0,
        Property<T1> property1, @Nullable T1 value1,
        Property<T2> property2, @Nullable T2 value2,
        Property<T3> property3, @Nullable T3 value3,
        Property<T4> property4, @Nullable T4 value4
    )
    {
        return of(
            property0, value0,
            property1, value1,
            property2, value2,
            property3, value3,
            property4, (Object) value4
        );
    }

    /**
     * Adds all properties from the given property container to this property container.
     * <p>
     * Existing properties will be overwritten.
     *
     * @param propertyContainer
     *     The property container to add the properties from.
     */
    public void addAll(IPropertyContainerConst propertyContainer)
    {
        for (PropertyValuePair<?> propertyValuePair : propertyContainer)
        {
            final Property<?> property = propertyValuePair.property();
            final IPropertyValue<?> value = propertyValuePair.value();
            setUntypedPropertyValue(property, value.value());
        }
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
        // We do not serialize the type because doing so is annoying and unnecessary, as the type
        // is provided by the property, which we can get from the key.
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
