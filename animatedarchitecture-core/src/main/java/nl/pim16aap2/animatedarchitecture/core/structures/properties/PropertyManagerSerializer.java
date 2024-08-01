package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import com.alibaba.fastjson2.JSON;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;

import java.util.HashMap;
import java.util.Map;

/**
 * Serializes and deserializes {@link PropertyManager} instances to and from JSON.
 */
public final class PropertyManagerSerializer
{
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
            //noinspection unchecked
            return PropertyManager.forType(
                structureType,
                (Map<String, IPropertyValue<?>>) JSON.parseObject(json, HashMap.class)
            );
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Could not deserialize PropertyManager from JSON: " + json, e);
        }
    }
}
