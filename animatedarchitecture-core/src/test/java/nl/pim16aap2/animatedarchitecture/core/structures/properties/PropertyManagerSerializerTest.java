package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import com.alibaba.fastjson2.JSON;
import nl.altindag.log.LogCaptor;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.testing.logging.LogAssertionsUtil;
import nl.pim16aap2.testing.logging.WithLogCapture;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@WithLogCapture
public class PropertyManagerSerializerTest
{
    private static final Property<Integer> PROPERTY_UNSET = new Property<>(
        "external",
        "unset_property",
        Integer.class,
        5
    );

    @Mock
    private StructureType structureType;
    private PropertyManager propertyManager;

    private Map<Property<?>, @Nullable Object> intermediateMap;

    @BeforeEach
    void setUp()
    {
        intermediateMap = createIntermediateMap(
            Property.ANIMATION_SPEED_MULTIPLIER, 1.5D,
            Property.OPEN_STATUS, null,
            Property.ROTATION_POINT, new Vector3Di(3, 1, 4),
            Property.REDSTONE_MODE, null
        );

        propertyManager = propertyManagerFromMap(intermediateMap);

        Mockito.when(structureType.getProperties()).thenReturn(List.copyOf(intermediateMap.keySet()));
        Mockito.when(structureType.getFullNameWithVersion()).thenReturn("test:structure:1");
    }

    @Test
    void testSerializationCycle()
    {
        final String serialized = PropertyManagerSerializer.serialize(propertyManager);

        Assertions.assertEquals(
            propertyManager,
            PropertyManagerSerializer.deserialize(structureType, serialized)
        );
    }

    @Test
    void testSerializationCycleWithNonTypeDefinedProperties()
    {
        propertyManager.setPropertyValue(PROPERTY_UNSET, 7);

        final String serialized = PropertyManagerSerializer.serialize(propertyManager);

        Assertions.assertEquals(
            propertyManager,
            PropertyManagerSerializer.deserialize(structureType, serialized)
        );
    }

    @Test
    void testSerializeAbstractStructure()
    {
        final AbstractStructure structure = Mockito.mock(AbstractStructure.class);
        Mockito.when(structure.getType()).thenReturn(structureType);

        final IPropertyManagerConst snapshot = propertyManager.snapshot();
        Mockito.when(structure.getPropertyManagerSnapshot()).thenReturn(snapshot);

        final String serialized = PropertyManagerSerializer.serialize(structure);
        Assertions.assertEquals(
            propertyManager,
            PropertyManagerSerializer.deserialize(structureType, serialized)
        );
    }

    @Test
    void testUpdatePropertyMapEntry()
    {
        final Map<String, IPropertyValue<?>> entries = new HashMap<>(createPropertyMap(
            Property.ANIMATION_SPEED_MULTIPLIER, 1.5D
        ));

        // "ANIMATION_SPEED_MULTIPLIER" exists, so it should be updated.
        Assertions.assertTrue(
            PropertyManagerSerializer.updatePropertyMapEntry(
                structureType,
                entries,
                Property.ANIMATION_SPEED_MULTIPLIER.getFullKey(),
                JSON.parseObject("{\"value\": 2.5}")
            )
        );

        Assertions.assertEquals(
            2.5D,
            (double) entries.get(Property.ANIMATION_SPEED_MULTIPLIER.getFullKey()).value(),
            0.0001D
        );


        // "OPEN_STATUS" does not exist, so it should not be updated.
        Assertions.assertFalse(
            PropertyManagerSerializer.updatePropertyMapEntry(
                structureType,
                entries,
                Property.OPEN_STATUS.getFullKey(),
                JSON.parseObject("{\"value\": 2.5}")
            )
        );
        Assertions.assertFalse(entries.containsKey(Property.OPEN_STATUS.getFullKey()));

        Assertions.assertEquals(1, entries.size());
    }

    @Test
    void testNonDefaultProperties()
    {
        final var mapWithUnsupportedProperty = new HashMap<>(intermediateMap);
        mapWithUnsupportedProperty.put(PROPERTY_UNSET, 7);

        final PropertyManager propertyManagerWithUnsupportedProperty =
            propertyManagerFromMap(mapWithUnsupportedProperty);

        final String serialized = PropertyManagerSerializer.serialize(propertyManagerWithUnsupportedProperty);
        final PropertyManager deserialized = PropertyManagerSerializer.deserialize(structureType, serialized);

        Assertions.assertTrue(deserialized.hasProperty(PROPERTY_UNSET));
    }

    @Test
    void testNonExistingProperties(LogCaptor logCaptor)
    {
        final String nonExistingProperty = "animatedarchitecture:non_existing_property";

        final String serialized =
            new StringBuilder(PropertyManagerSerializer.serialize(propertyManager))
                .insert(1, "\"" + nonExistingProperty + "\":{\"value\":5},")
                .toString();

        final PropertyManager deserialized = PropertyManagerSerializer.deserialize(structureType, serialized);

        // After deserialization, the PropertyManager should not contain the non-existing property.
        Assertions.assertEquals(propertyManager, deserialized);

        LogAssertionsUtil
            .logAssertionBuilder(logCaptor)
            .message(
                "Discarding property '%s' with value '%s' for structure type '%s' as it is not supported.",
                nonExistingProperty,
                "{\"value\":5}",
                structureType)
            .level(Level.SEVERE)
            .assertLogged();
    }

    // Test that for missing properties in the JSON String the following happens:
    // 1) A 'warning' log message is logged about the default value being used.
    // 2) The property is added to the property map with the default value.
    @Test
    void testMissingProperties(LogCaptor logCaptor)
    {
        final PropertyManager propertyManagerWithMissingProperties = propertyManagerFromMap(
            createIntermediateMap(
                Property.ANIMATION_SPEED_MULTIPLIER, -1.5D,
                Property.OPEN_STATUS, true
            )
        );

        final String serialized = PropertyManagerSerializer.serialize(propertyManagerWithMissingProperties);

        final PropertyManager deserialized = PropertyManagerSerializer.deserialize(structureType, serialized);

        //noinspection DataFlowIssue
        Assertions.assertEquals(
            -1.5D,
            deserialized.getPropertyValue(Property.ANIMATION_SPEED_MULTIPLIER).value(),
            0.0001D
        );
        Assertions.assertEquals(true, deserialized.getPropertyValue(Property.OPEN_STATUS).value());
        Assertions.assertEquals(
            Property.ROTATION_POINT.getDefaultValue(),
            deserialized.getPropertyValue(Property.ROTATION_POINT).value()
        );
        Assertions.assertEquals(
            Property.REDSTONE_MODE.getDefaultValue(),
            deserialized.getPropertyValue(Property.REDSTONE_MODE).value()
        );

        final String logMessage = "Property '%s' was not supplied for structure type '%s', using default value '%s'.";

        LogAssertionsUtil
            .logAssertionBuilder(logCaptor)
            .message(
                logMessage,
                PropertyManager.mapKey(Property.REDSTONE_MODE),
                structureType,
                Property.REDSTONE_MODE.getDefaultValue()
            )
            .level(Level.FINER)
            .assertLogged();

        LogAssertionsUtil
            .logAssertionBuilder(logCaptor)
            .message(
                logMessage,
                PropertyManager.mapKey(Property.ROTATION_POINT),
                structureType,
                Property.ROTATION_POINT.getDefaultValue()
            )
            .level(Level.FINER)
            .assertLogged();
    }

    /**
     * Creates a new 'intermediate' map of properties and their values.
     * <p>
     * This map cannot be used directly to create a {@link PropertyManager} but can be used by
     * {@link #createPropertyMap(Map)} to create a map that can be used to create a {@link PropertyManager}.
     *
     * @param input
     *     The properties and their values. The properties and their values are expected to be interleaved.
     *     <p>
     *     Example: {@code Property.ANIMATION_SPEED_MULTIPLIER, 1.5D, Property.OPEN_STATUS, null}
     * @return The intermediate map of properties and their values.
     */
    private static Map<Property<?>, @Nullable Object> createIntermediateMap(@Nullable Object... input)
    {
        if (input.length % 2 != 0)
            throw new IllegalArgumentException("Input must be a multiple of 2.");

        final Map<Property<?>, @Nullable Object> entries = LinkedHashMap.newLinkedHashMap(input.length / 2);

        for (int i = 0; i < input.length; i += 2)
        {
            final @Nullable Object key = input[i];

            if (!(key instanceof Property<?> property))
                throw new IllegalArgumentException(
                    "Argument " + i + " must be a Property but was " + (key == null ? "null" : key.getClass()));

            entries.put(property, input[i + 1]);
        }
        return entries;
    }

    /**
     * Creates a new map of properties and their values from an array of properties and their values.
     *
     * @param input
     *     The properties and their values. The properties and their values are expected to be interleaved.
     *     <p>
     *     Example: {@code Property.ANIMATION_SPEED_MULTIPLIER, 1.5D, Property.OPEN_STATUS, null}
     * @return The map of property names and their property values.
     */
    private static Map<String, IPropertyValue<?>> createPropertyMap(@Nullable Object... input)
    {
        return createPropertyMap(createIntermediateMap(input));
    }

    /**
     * Creates a new map of properties and their values from an intermediate map of properties and their values.
     *
     * @param input
     *     The intermediate map of properties and their values.
     * @return The map of property names and their property values.
     */
    private static Map<String, IPropertyValue<?>> createPropertyMap(Map<Property<?>, @Nullable Object> input)
    {
        return input
            .entrySet()
            .stream()
            .collect(Collectors.toUnmodifiableMap(
                entry -> PropertyManager.mapKey(entry.getKey()),
                entry -> PropertyManager.mapUntypedValue(entry.getKey(), entry.getValue())
            ));
    }

    /**
     * Creates a new {@link PropertyManager} from a map of properties and their values.
     *
     * @param map
     *     The map of properties and their values.
     * @return The new {@link PropertyManager}.
     */
    private static PropertyManager propertyManagerFromMap(Map<Property<?>, @Nullable Object> map)
    {
        return new PropertyManager(new LinkedHashMap<>(createPropertyMap(map)));
    }
}
