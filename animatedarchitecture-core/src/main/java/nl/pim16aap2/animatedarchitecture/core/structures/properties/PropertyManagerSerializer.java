package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Serializes and deserializes {@link PropertyManager} instances to and from JSON.
 */
public final class PropertyManagerSerializer
{
    /**
     * The type reference for the intermediate map used during deserialization.
     * <p>
     * This map is used to deserialize the JSON string to a map of {@link String} to {@link JSONObject}.
     * <p>
     * The {@link JSONObject}s are then deserialized to {@link PropertyManager.ProvidedPropertyValue}s.
     */
    private static final TypeReference<Map<String, JSONObject>> INTERMEDIATE_MAP_TYPE_REFERENCE =
        new TypeReference<>() {};

    /**
     * Serializes the given {@link IPropertyManagerConst} to a JSON string.
     *
     * @param propertyManagerConst
     *     The {@link IPropertyManagerConst} to serialize.
     * @return The JSON string representing the {@link IPropertyManagerConst}.
     */
    public static String serialize(IPropertyManagerConst propertyManagerConst)
    {
        final Map<String, IPropertyValue<?>> map = switch (propertyManagerConst)
        {
            case PropertyManager propertyManager -> propertyManager.getMap();
            case PropertyManagerSnapshot propertyManagerSnapshot -> propertyManagerSnapshot.getMap();
        };
        return JSON.toJSONString(map);
    }

    /**
     * Serializes the {@link PropertyManager} of the given {@link AbstractStructure} to a JSON string.
     *
     * @param structure
     *     The structure whose {@link PropertyManager} to serialize.
     * @return The JSON string representing the {@link PropertyManager}.
     */
    public static String serialize(AbstractStructure structure)
    {
        return serialize(structure.getPropertyManagerSnapshot());
    }

    private static PropertyManager.ProvidedPropertyValue<?> deserializeMapValue(
        String propertyKey,
        JSONObject jsonObject)
    {
        final @Nullable Property<?> property = Property.fromName(propertyKey);
        if (property == null)
        {
            throw new IllegalArgumentException(
                "Could not deserialize PropertyManager: Unknown property key: '" + propertyKey + "'");
        }

        final @Nullable Object value = jsonObject.getObject("value", property.getType());
        return PropertyManager.mapValue(value);
    }

    private static PropertyManager deserialize(
        StructureType structureType,
        Map<String, JSONObject> map)
    {
        final Map<String, PropertyManager.ProvidedPropertyValue<?>> propertyMap = HashMap.newHashMap(map.size());

        for (final var entry : map.entrySet())
        {
            final String propertyKey = entry.getKey();
            final JSONObject jsonObject = entry.getValue();

            final PropertyManager.ProvidedPropertyValue<?> providedPropertyValue = deserializeMapValue(
                propertyKey,
                jsonObject);
            propertyMap.put(propertyKey, providedPropertyValue);
        }

        return PropertyManager.forType(structureType, propertyMap);
    }

    /**
     * Deserializes a {@link PropertyManager} from a JSON string.
     *
     * @param structureType
     *     The structure type to deserialize the {@link PropertyManager} for.
     * @param json
     *     The JSON string to deserialize.
     * @return The deserialized {@link PropertyManager}.
     */
    public static PropertyManager deserialize(StructureType structureType, String json)
    {
        try
        {
            return deserialize(structureType, JSON.parseObject(json, INTERMEDIATE_MAP_TYPE_REFERENCE));
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Could not deserialize PropertyManager from JSON: " + json, e);
        }
    }
}
