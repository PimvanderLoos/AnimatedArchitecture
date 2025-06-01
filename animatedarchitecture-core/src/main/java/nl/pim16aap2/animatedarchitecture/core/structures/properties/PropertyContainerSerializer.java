package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.TypeReference;
import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.writer.ObjectWriter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureConst;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Serializes and deserializes {@link PropertyContainer} instances to and from JSON.
 */
@Flogger
public final class PropertyContainerSerializer
{
    static
    {
        JSON.register(UndefinedPropertyValue.class, new UndefinedPropertyValueSerializer());
    }

    /**
     * The type reference for the intermediate map used during deserialization.
     * <p>
     * This map is used to deserialize the JSON string to a map of {@link String} to {@link JSONObject}.
     * <p>
     * The {@link JSONObject}s are then deserialized to {@link PropertyContainer.ProvidedPropertyValue}s.
     */
    private static final TypeReference<Map<String, JSONObject>> INTERMEDIATE_MAP_TYPE_REFERENCE =
        new TypeReference<>() {};

    /**
     * Serializes the given {@link IPropertyContainerConst} to a JSON string.
     *
     * @param propertyContainerConst
     *     The {@link IPropertyContainerConst} to serialize.
     * @return The JSON string representing the {@link IPropertyContainerConst}.
     */
    public static String serialize(IPropertyContainerConst propertyContainerConst)
    {
        final Map<String, IPropertyValue<?>> map = switch (propertyContainerConst)
        {
            case PropertyContainer propertyContainer -> propertyContainer.getMap();
            case PropertyContainerSnapshot propertyContainerSnapshot -> propertyContainerSnapshot.getMap();
        };
        return JSON.toJSONString(map);
    }

    /**
     * Serializes the {@link PropertyContainer} of the given {@link Structure} to a JSON string.
     *
     * @param structure
     *     The structure whose {@link PropertyContainer} to serialize.
     * @return The JSON string representing the {@link PropertyContainer}.
     */
    public static String serialize(IStructureConst structure)
    {
        return serialize(structure.getPropertyContainerSnapshot());
    }

    /**
     * Deserializes an {@link IPropertyValue} from the given {@link JSONObject}.
     *
     * @param jsonObject
     *     The {@link JSONObject} to deserialize.
     * @param type
     *     The type of the value.
     * @param removable
     *     Whether the property is removable.
     * @param <T>
     *     The type of the value.
     * @return The deserialized {@link IPropertyValue}.
     */
    private static <T> IPropertyValue<T> deserializePropertyValue(
        JSONObject jsonObject,
        Class<T> type,
        boolean removable)
    {
        return new PropertyContainer.ProvidedPropertyValue<>(
            type,
            jsonObject.getObject("value", type),
            removable
        );
    }

    /**
     * Updates the given {@link Map} with the deserialized value of the given key.
     *
     * @param structureType
     *     The structure type to update the property map for. Used to provide context in log messages.
     * @param propertyMap
     *     The property map to update.
     *     <p>
     *     This map is updated in-place with the deserialized value of the given key if applicable.
     * @param key
     *     The key of the property to update.
     * @param jsonObject
     *     The {@link JSONObject} that represents the value of the property. This object should contain a "value" field
     *     that represents the value of the property.
     *     <p>
     *     The object is not deserialized if the key does not exist in the default map.
     * @return True if the key exists in the default map and the value was deserialized and updated, false otherwise.
     */
    static boolean updatePropertyMapEntry(
        StructureType structureType,
        Map<String, IPropertyValue<?>> propertyMap,
        String key,
        JSONObject jsonObject)
    {
        return propertyMap.compute(key, (k, existingEntry) ->
        {
            // If the key exists in the default map, deserialize the value and replace the entry.
            if (existingEntry != null)
                return deserializePropertyValue(jsonObject, existingEntry.type(), existingEntry.isRemovable());

            // If the key does not exist in the default map,
            // we can conclude that the structure type does not support this property.
            log.atSevere().log(
                "Discarding property '%s' with value '%s' for structure type '%s' as it is not supported.",
                key,
                jsonObject,
                structureType
            );
            return null;
        }) != null;
    }

    /**
     * Adds a non-default property to the given {@link Map}.
     *
     * @param propertyMap
     *     The property map to add the property to. This map is updated in-place with the deserialized
     * @param property
     *     The property to add to the map.
     * @param key
     *     The key of the property to add.
     *     <p>
     *     This is used
     * @param jsonObject
     *     The {@link JSONObject} that represents the value of the property. This object should contain a "value" field
     *     that represents the value of the property.
     */
    private static void addNonDefaultProperty(
        Map<String, IPropertyValue<?>> propertyMap,
        Property<?> property,
        String key,
        JSONObject jsonObject)
    {
        propertyMap.put(key, deserializePropertyValue(jsonObject, property.getType(), true));
    }

    /**
     * Adds an undefined property to the given {@link Map}.
     *
     * @param structureType
     *     The structure type to add the property for. Used to provide context in log messages.
     * @param propertyMap
     *     The property map to add the property to. This map is updated in-place with the value for the given key.
     * @param key
     *     The key of the property to add.
     * @param jsonObject
     *     The {@link JSONObject} that represents the value of the property. This object should contain a "value" field
     *     that represents the value of the property.
     */
    private static void addUndefinedProperty(
        StructureType structureType,
        Map<String, IPropertyValue<?>> propertyMap,
        String key,
        JSONObject jsonObject)
    {
        log.atInfo().log(
            "Property '%s' is not defined for structure type '%s'.",
            key,
            structureType
        );

        propertyMap.put(key, new UndefinedPropertyValue(key, jsonObject, true));
    }

    /**
     * Deserializes a map to a {@link PropertyContainer}.
     * <p>
     * This method will go over the entries in the deserialized map and apply the values to the default property map of
     * the given structure type (See {@link PropertyContainer#getDefaultPropertyMap(StructureType)}).
     * <p>
     * If a property in the deserialized map is not supported by the structure type (i.e. the key does not exist in the
     * default property map), a warning will be logged and the property will be discarded.
     * <p>
     * Conversely, if a property in the default property map is not present in the deserialized map, a warning will be
     * logged and the default value will be used (See {@link Property#getDefaultValue()}).
     *
     * @param structureType
     *     The structure type to deserialize the {@link PropertyContainer} for.
     *     <p>
     *     The structure type is used to obtain the default property map for the given structure type and to provide
     *     context in log messages.
     * @param deserializedMap
     *     The map to deserialize.
     *     <p>
     *     This map should contain the keys of the properties mapped to {@link JSONObject}s that represent the
     *     serialized values of the properties. Only supported properties will be deserialized.
     * @return The deserialized {@link PropertyContainer}.
     */
    static PropertyContainer deserialize(
        StructureType structureType,
        Map<String, JSONObject> deserializedMap)
    {
        final Map<String, IPropertyValue<?>> propertyMap =
            new HashMap<>(PropertyContainer.getDefaultPropertyMap(structureType));

        int updatedProperties = 0;
        for (final var entry : deserializedMap.entrySet())
        {
            final String key = entry.getKey();
            final JSONObject jsonObject = entry.getValue();

            if (updatePropertyMapEntry(structureType, propertyMap, key, jsonObject))
            {
                updatedProperties++;
                continue;
            }

            final @Nullable Property<?> property = Property.fromName(key);
            if (property != null)
                addNonDefaultProperty(propertyMap, property, key, jsonObject);
            else
                addUndefinedProperty(structureType, propertyMap, key, jsonObject);
        }

        if (updatedProperties != propertyMap.size())
            logMissingProperties(structureType, propertyMap, deserializedMap);

        return new PropertyContainer(propertyMap);
    }

    /**
     * Logs a warning for each property in the default property map that is not present in the deserialized map.
     *
     * @param structureType
     *     The structure type whose property container is being deserialized. Used to provide context in log messages.
     * @param propertyMap
     *     The default property map of the structure type.
     * @param deserializedMap
     *     The deserialized map.
     */
    private static void logMissingProperties(
        StructureType structureType,
        Map<String, IPropertyValue<?>> propertyMap,
        Map<String, JSONObject> deserializedMap)
    {
        propertyMap
            .entrySet()
            .stream()
            .filter(entry -> !deserializedMap.containsKey(entry.getKey()))
            .forEach(entry -> log.atFiner().log(
                "Property '%s' was not supplied for structure type '%s', using default value '%s'.",
                entry.getKey(),
                structureType,
                entry.getValue().value()
            ));
    }

    /**
     * Deserializes a {@link PropertyContainer} from a JSON string.
     *
     * @param structureType
     *     The structure type to deserialize the {@link PropertyContainer} for.
     * @param json
     *     The JSON string to deserialize.
     * @return The deserialized {@link PropertyContainer}.
     */
    public static PropertyContainer deserialize(StructureType structureType, String json)
    {
        try
        {
            return deserialize(structureType, JSON.parseObject(json, INTERMEDIATE_MAP_TYPE_REFERENCE));
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Could not deserialize PropertyContainer from JSON: " + json, e);
        }
    }

    /**
     * Represents a property value for an undefined property.
     * <p>
     * This class stores the raw JSONObject value of the property.
     * <p>
     * For deserialization, the value uses the raw JSONObject value.
     *
     * @param propertyKey
     *     The key of the property.
     * @param serializedValue
     *     The serialized value of the property.
     * @param isRemovable
     *     Whether the property is removable.
     *     <p>
     *     When set to {@code false}, the property cannot be removed from the property container and will throw an
     *     exception if an attempt is made to do so.
     */
    record UndefinedPropertyValue(
        @JSONField(serialize = false) String propertyKey,
        @JSONField(serialize = false) JSONObject serializedValue,
        @JSONField(serialize = false) boolean isRemovable)
        implements IPropertyValue<Object>
    {
        @JSONField(serialize = false)
        @Override
        public boolean isSet()
        {
            return false;
        }

        @JSONField(serialize = false)
        @Override
        public @Nullable Object value()
        {
            return null;
        }

        @JSONField(serialize = false)
        @Override
        public Class<Object> type()
        {
            return Object.class;
        }

        /**
         * Deserializes the value of the property.
         * <p>
         * At the time this object was created, the property did not exist yet.
         * <p>
         * However, the property object is required to call this method. Therefore, we know that the value can finally
         * be deserialized.
         *
         * @param property
         *     The property to deserialize the value for.
         * @param <T>
         *     The type of the property.
         * @return The deserialized value of the property.
         */
        public <T> IPropertyValue<T> deserializeValue(Property<T> property)
        {
            final String mappedKey = PropertyContainer.mapKey(property);

            if (!mappedKey.equals(propertyKey))
                throw new IllegalArgumentException(String.format(
                    "Property key mismatch: Expected '%s', got '%s'",
                    mappedKey,
                    propertyKey
                ));

            return deserializePropertyValue(serializedValue, property.getType(), isRemovable);
        }
    }

    /**
     * 'Serializes' an {@link UndefinedPropertyValue} for JSON serialization.
     * <p>
     * This class simply writes {@link UndefinedPropertyValue#serializedValue} to the JSON writer.
     */
    public static class UndefinedPropertyValueSerializer implements ObjectWriter<UndefinedPropertyValue>
    {
        @Override
        public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features)
        {
            if (!(object instanceof UndefinedPropertyValue undefinedPropertyValue))
                throw new IllegalArgumentException("Object is not an instance of UndefinedPropertyValue");
            jsonWriter.write(undefinedPropertyValue.serializedValue);
        }
    }
}
