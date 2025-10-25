package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.util.LazyValue;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

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
import java.util.concurrent.atomic.AtomicReference;
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
@CustomLog
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
     * Creates a new empty property container.
     * <p>
     * It is generally recommended to use {@link #forType(StructureType)} instead to create a property container with
     * the default properties for a specific structure type. This ensures that the required properties for that
     * structure type are correctly marked as required and have their default values set.
     */
    public PropertyContainer()
    {
        this(new HashMap<>());
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
        switch (other)
        {
            case PropertyContainer propertyContainer ->
            {
                // If the other is a PropertyContainer, we can just copy the map.
                return new PropertyContainer(new HashMap<>(propertyContainer.propertyMap));
            }
            case PropertyContainerSnapshot snapshot ->
            {
                // If the other is a PropertyContainerSnapshot, we can copy the map from it.
                return new PropertyContainer(new HashMap<>(snapshot.getPropertyMap()));
            }
        }
    }

    /**
     * Sets the value of the given property.
     *
     * @param property
     *     The property to set the value for.
     * @param value
     *     The value to set.
     * @param <T>
     *     The type of the property.
     * @return The previous value of the property, or {@link UnsetPropertyValue#INSTANCE} if the property was not set.
     */
    private <T> IPropertyValue<?> setPropertyValue0(
        Property<T> property,
        @Nullable T value)
    {
        if (value == null)
            return removeProperty0(property);

        final AtomicReference<@Nullable IPropertyValue<?>> oldValueRef = new AtomicReference<>();
        final IPropertyValue<?> newValue = propertyMap.compute(
            mapKey(property),
            (key, oldValue) ->
            {
                final boolean required = oldValue != null && oldValue.isRequired();
                oldValueRef.set(oldValue);
                return new ProvidedPropertyValue<>(property.getType(), value, required);
            }
        );
        propertySet.reset();

        final IPropertyValue<?> oldValue = oldValueRef.get();

        if (oldValue instanceof PropertyContainerSerializer.UndefinedPropertyValue undefinedPropertyValue)
        {
            log.atWarn().log(
                "Property '%s' was previously undefined. It was overwritten with new value '%s'.",
                property.getFullKey(),
                newValue.value()
            );
            return undefinedPropertyValue.deserializeValue(property);
        }
        return oldValue == null ? UnsetPropertyValue.INSTANCE : oldValue;
    }

    private <T> IPropertyValue<?> removeProperty0(Property<T> property)
    {
        final AtomicReference<@Nullable IPropertyValue<?>> oldValueRef = new AtomicReference<>();
        propertyMap.computeIfPresent(
            mapKey(property),
            (key, oldValue) ->
            {
                if (oldValue.isRequired())
                    throw new IllegalArgumentException(
                        String.format("Property '%s' cannot be removed!", property.getFullKey()));
                oldValueRef.set(oldValue);
                return null;
            }
        );

        propertySet.reset();
        return Objects.requireNonNullElse(oldValueRef.get(), UnsetPropertyValue.INSTANCE);
    }

    @Override
    public <T> IPropertyValue<T> setPropertyValue(Property<T> property, @Nullable T value)
    {
        final IPropertyValue<?> prev = setPropertyValue0(property, value);
        try
        {
            return cast(property, prev);
        }
        catch (IllegalArgumentException exception)
        {
            throw new IllegalArgumentException(
                String.format(
                    "Old value '%s' for property '%s' is not of correct type '%s'! It has now been replaced by '%s'.",
                    prev,
                    property.getFullKey(),
                    property.getType(),
                    value
                ),
                exception
            );
        }
    }

    @Override
    public <T> IPropertyValue<T> removeProperty(Property<T> property)
    {
        final IPropertyValue<?> prev = removeProperty0(property);

        try
        {
            return cast(property, prev);
        }
        catch (IllegalArgumentException exception)
        {
            throw new IllegalArgumentException(
                String.format(
                    "Old value '%s' for property '%s' is not of type '%s'! It has been removed regardless.",
                    prev,
                    property.getFullKey(),
                    property.getType()
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
     *     The value to set.
     * @return The previous value of the property, or {@link UnsetPropertyValue#INSTANCE} if the property was not set.
     *
     * @throws ClassCastException
     *     If the value cannot be cast to the type of the property.
     */
    public <T> IPropertyValue<?> setUntypedPropertyValue(Property<T> property, @Nullable Object value)
    {
        if (value == null)
            return setPropertyValue0(property, null);
        return setPropertyValue0(property, property.cast(value));
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

    @Override
    public boolean canRemoveProperty(Property<?> property)
    {
        final IPropertyValue<?> value = propertyMap.get(mapKey(property));
        return value == null || !value.isRequired();
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
     * @param required
     *     Whether the properties are required.
     * @return A new property container for the given properties.
     */
    @VisibleForTesting
    public static PropertyContainer forProperties(List<Property<?>> properties, boolean required)
    {
        return new PropertyContainer(toPropertyMap(properties, required));
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
    private static Map<String, IPropertyValue<?>> newDefaultPropertyMap(StructureType structureType)
    {
        return Collections.unmodifiableMap(toPropertyMap(structureType.getProperties(), true));
    }

    /**
     * Creates a property map from the given list of properties.
     * <p>
     * Each property is set to its default value. See {@link Property#getDefaultValue()}.
     *
     * @param properties
     *     The properties to create the map from.
     * @param required
     *     Whether the properties are required.
     * @return A new property map with the default properties for the given structure type.
     */
    static Map<String, IPropertyValue<?>> toPropertyMap(List<Property<?>> properties, boolean required)
    {
        return properties
            .stream()
            .collect(Collectors.toMap(
                PropertyContainer::mapKey,
                property -> defaultMapValue(property, required),
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
    @VisibleForTesting
    static <T> IPropertyValue<T> cast(Property<T> property, IPropertyValue<?> value)
    {
        if (value instanceof PropertyContainerSerializer.UndefinedPropertyValue undefinedPropertyValue)
            return undefinedPropertyValue.deserializeValue(property);

        if (value instanceof UnsetPropertyValue)
            // Safe to cast because it always returns null.
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
    private <T> IPropertyValue<T> getValue(Property<T> property)
    {
        final String key = mapKey(property);
        final IPropertyValue<?> rawValue = propertyMap.getOrDefault(key, UnsetPropertyValue.INSTANCE);

        if (rawValue instanceof PropertyContainerSerializer.UndefinedPropertyValue undefinedPropertyValue)
        {
            final var newValue =
                undefinedPropertyValue.deserializeValue(property);

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
     * @param required
     *     Whether the property is required.
     * @param <T>
     *     The type of the property.
     * @return The default value for the property.
     */
    static <T> ProvidedPropertyValue<T> defaultMapValue(Property<T> property, boolean required)
    {
        return mapValue(property, property.getDefaultValue(), required);
    }

    /**
     * Gets the map value for the given value.
     * <p>
     * If the value is {@code null}, this method returns {@link UnsetPropertyValue#INSTANCE}.
     *
     * @param property
     *     The property to map the value for.
     * @param value
     *     The value to convert.
     * @param required
     *     Whether the property is required.
     * @return {@link UnsetPropertyValue#INSTANCE} if the value is {@code null}, otherwise the value wrapped in a
     * {@link ProvidedPropertyValue}.
     */
    private static <T> ProvidedPropertyValue<T> mapValue(Property<T> property, T value, boolean required)
    {
        return new ProvidedPropertyValue<>(property.getType(), value, required);
    }

    /**
     * Gets the map value for the given untyped value.
     * <p>
     * If the value is {@code null}, this method returns {@link UnsetPropertyValue#INSTANCE}.
     * <p>
     * If possible, consider using {@link #mapValue(Property, Object, boolean)} instead for better type safety.
     *
     * @param property
     *     The property to get the value for.
     * @param value
     *     The value to convert.
     * @param removable
     *     Whether the property is removable.
     * @param <T>
     *     The type of the property.
     * @return The value wrapped in a {@link ProvidedPropertyValue}.
     *
     * @throws ClassCastException
     *     If the value cannot be cast to the type of the property.
     */
    @VisibleForTesting
    static <T> ProvidedPropertyValue<T> mapUntypedValue(Property<T> property, Object value, boolean removable)
    {
        return mapValue(property, property.cast(value), removable);
    }

    @Override
    public Iterator<PropertyValuePair<?>> iterator()
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
                final Property<?> property = Property.fromName(entry.getKey());
                return property == null ? null : PropertyValuePair.of(property, entry.getValue());
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Adds all properties from the given property container to this property container.
     * <p>
     * Existing properties will be overwritten.
     * <p>
     * A property will be marked as required only if it is already required in this property container.
     *
     * @param propertyContainer
     *     The property container to add the properties from.
     */
    public void addAll(IPropertyContainerConst propertyContainer)
    {
        propertyContainer.forEach(this::addPropertyValuePair);
    }

    @VisibleForTesting
    <T> void addPropertyValuePair(PropertyValuePair<T> pair)
    {
        final Property<T> property = pair.property();
        final IPropertyValue<T> value = pair.value();

        propertyMap.compute(
            mapKey(property),
            (key, oldValue) ->
            {
                final boolean required = oldValue != null && oldValue.isRequired();
                try
                {
                    return mapValue(property, Objects.requireNonNull(value.value()), required);
                }
                catch (Exception exception)
                {
                    throw new RuntimeException(
                        String.format(
                            "Failed to add property '%s' with value '%s'!",
                            property.getFullKey(),
                            value.value()),
                        exception
                    );
                }
            }
        );
    }

    @Override
    public int propertyCount()
    {
        return propertyMap.size();
    }

    /**
     * Creates a new {@link PropertyContainer} from the given properties.
     *
     * @param properties
     *     A group of interleaved property-value-isRequired triplets.
     * @return A new {@link PropertyContainer} with the given properties.
     *
     * @throws IllegalArgumentException
     *     If the properties are not provided in pairs of 3.
     *     <p>
     *     If an object at an even index is not a {@link Property}.
     * @throws NullPointerException
     *     If any value is {@code null}.
     * @throws ClassCastException
     *     If the value of a property cannot be cast to the type of the preceding property using
     *     {@link Property#cast(Object)}.
     */
    public static PropertyContainer ofAll(Object... properties)
    {
        if (properties.length == 0)
            return new PropertyContainer(HashMap.newHashMap(0));

        if (properties.length % 3 != 0)
            throw new IllegalArgumentException("Properties must be provided in pairs of (Property, value, isRequired)");

        final PropertyContainer ret = new PropertyContainer(HashMap.newHashMap(properties.length / 3));
        for (int idx = 0; idx < properties.length; idx += 3)
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

            final Object value = Util.requireNonNull(
                properties[idx + 1],
                "Value at index " + (idx + 1) + " for property '" + property.getFullKey() + "'"
            );
            final boolean required = (boolean) Util.requireNonNull(
                properties[idx + 2],
                "Required flag at index " + (idx + 2) + " for property '" + property.getFullKey() + "'"
            );

            ret.propertyMap.put(
                mapKey(property),
                mapUntypedValue(property, value, required)
            );
        }

        ret.propertySet.reset();

        return ret;
    }

    /**
     * Type-safe version of {@link #ofAll(Object...)} for 1 property.
     */
    public static <A> PropertyContainer of(
        Property<A> property0, A value0, boolean required0
    )
    {
        return ofAll(
            property0, value0, required0
        );
    }

    /**
     * Type-safe version of {@link #ofAll(Object...)} for 2 properties.
     */
    public static <A, B> PropertyContainer of(
        Property<A> property0, A value0, boolean required0,
        Property<B> property1, B value1, boolean required1
    )
    {
        return ofAll(
            property0, value0, required0,
            property1, value1, required1
        );
    }

    /**
     * Type-safe version of {@link #ofAll(Object...)} for 3 properties.
     */
    public static <A, B, C> PropertyContainer of(
        Property<A> property0, A value0, boolean required0,
        Property<B> property1, B value1, boolean required1,
        Property<C> property2, C value2, boolean required2
    )
    {
        return ofAll(
            property0, value0, required0,
            property1, value1, required1,
            property2, value2, required2
        );
    }

    /**
     * Type-safe version of {@link #ofAll(Object...)} for 4 properties.
     */
    public static <A, B, C, D> PropertyContainer of(
        Property<A> property0, A value0, boolean required0,
        Property<B> property1, B value1, boolean required1,
        Property<C> property2, C value2, boolean required2,
        Property<D> property3, D value3, boolean required3
    )
    {
        return ofAll(
            property0, value0, required0,
            property1, value1, required1,
            property2, value2, required2,
            property3, value3, required3
        );
    }

    /**
     * Type-safe version of {@link #ofAll(Object...)} for 5 properties.
     */
    public static <A, B, C, D, E> PropertyContainer of(
        Property<A> property0, A value0, boolean required0,
        Property<B> property1, B value1, boolean required1,
        Property<C> property2, C value2, boolean required2,
        Property<D> property3, D value3, boolean required3,
        Property<E> property4, E value4, boolean required4
    )
    {
        return ofAll(
            property0, value0, required0,
            property1, value1, required1,
            property2, value2, required2,
            property3, value3, required3,
            property4, value4, required4
        );
    }

    /**
     * Represents a property value that is set.
     *
     * @param type
     *     The type of the property value.
     * @param value
     *     The value of the property.
     * @param isRequired
     *     Whether the property is removable.
     *     <p>
     *     When set to {@code false}, the property cannot be removed from the property container and will throw an
     *     exception if an attempt is made to do so.
     * @param <T>
     *     The type of the property.
     */
    record ProvidedPropertyValue<T>(
        // We do not serialize the type because doing so is annoying and unnecessary, as the type
        // is provided by the property, which we can get from the key.
        @JSONField(serialize = false) Class<T> type,
        T value,
        @JSONField(serialize = false) boolean isRequired)
        implements IPropertyValue<T>
    {
        ProvidedPropertyValue
        {
            Objects.requireNonNull(type, "Type cannot be null");
            Objects.requireNonNull(value, "Value cannot be null");
        }

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
        public boolean isRequired()
        {
            return false; // Essentially a no-op.
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
