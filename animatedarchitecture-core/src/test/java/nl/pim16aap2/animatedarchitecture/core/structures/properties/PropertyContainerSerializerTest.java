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
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@WithLogCapture
public class PropertyContainerSerializerTest
{
    private static final Property<Integer> PROPERTY_UNSET = new Property<>(
        "external",
        "unset_property",
        Integer.class,
        5
    );

    @Mock
    private StructureType structureType;
    private PropertyContainer propertyContainer;

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

        propertyContainer = propertyContainerFromMap(intermediateMap);

        Mockito.when(structureType.getProperties()).thenReturn(List.copyOf(intermediateMap.keySet()));
        Mockito.when(structureType.getFullNameWithVersion()).thenReturn("test:structure:1");
    }

    @Test
    void testSerializationCycle()
    {
        final String serialized = PropertyContainerSerializer.serialize(propertyContainer);

        Assertions.assertEquals(
            propertyContainer,
            PropertyContainerSerializer.deserialize(structureType, serialized)
        );
    }

    @Test
    void testSerializationCycleWithNonTypeDefinedProperties()
    {
        propertyContainer.setPropertyValue(PROPERTY_UNSET, 7);

        final String serialized = PropertyContainerSerializer.serialize(propertyContainer);

        Assertions.assertEquals(
            propertyContainer,
            PropertyContainerSerializer.deserialize(structureType, serialized)
        );
    }

    @Test
    void testSerializeAbstractStructure()
    {
        final AbstractStructure structure = Mockito.mock(AbstractStructure.class);
        Mockito.when(structure.getType()).thenReturn(structureType);

        final IPropertyContainerConst snapshot = propertyContainer.snapshot();
        Mockito.when(structure.getPropertyContainerSnapshot()).thenReturn(snapshot);

        final String serialized = PropertyContainerSerializer.serialize(structure);
        Assertions.assertEquals(
            propertyContainer,
            PropertyContainerSerializer.deserialize(structureType, serialized)
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
            PropertyContainerSerializer.updatePropertyMapEntry(
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
            PropertyContainerSerializer.updatePropertyMapEntry(
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

        final PropertyContainer propertyContainerWithUnsupportedProperty =
            propertyContainerFromMap(mapWithUnsupportedProperty);

        final String serialized = PropertyContainerSerializer.serialize(propertyContainerWithUnsupportedProperty);
        final PropertyContainer deserialized = PropertyContainerSerializer.deserialize(structureType, serialized);

        Assertions.assertTrue(deserialized.hasProperty(PROPERTY_UNSET));
    }

    // Test that for missing properties in the JSON String the following happens:
    // 1) A 'warning' log message is logged about the default value being used.
    // 2) The property is added to the property map with the default value.
    @Test
    void testMissingProperties(LogCaptor logCaptor)
    {
        final PropertyContainer propertyContainerWithMissingProperties = propertyContainerFromMap(
            createIntermediateMap(
                Property.ANIMATION_SPEED_MULTIPLIER, -1.5D,
                Property.OPEN_STATUS, true
            )
        );

        final String serialized = PropertyContainerSerializer.serialize(propertyContainerWithMissingProperties);

        final PropertyContainer deserialized = PropertyContainerSerializer.deserialize(structureType, serialized);

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
                PropertyContainer.mapKey(Property.REDSTONE_MODE),
                structureType,
                Property.REDSTONE_MODE.getDefaultValue()
            )
            .level(Level.FINER)
            .assertLogged();

        LogAssertionsUtil
            .logAssertionBuilder(logCaptor)
            .message(
                logMessage,
                PropertyContainer.mapKey(Property.ROTATION_POINT),
                structureType,
                Property.ROTATION_POINT.getDefaultValue()
            )
            .level(Level.FINER)
            .assertLogged();
    }

    @Test
    void testUndefinedProperty()
    {
        final String propertyKey = "animatedarchitecture:" + UUID.randomUUID();
        final String serialized = "{\"" + propertyKey + "\":{\"value\":5}}";

        // Use a mocked+random property key to ensure Property.fromName does not return a property.
        final var property = mockProperty(propertyKey, Integer.class, 5);

        final var deserialized = PropertyContainerSerializer.deserialize(structureType, serialized);
        Assertions.assertTrue(deserialized.hasProperty(property));

        final IPropertyValue<?> value0 = deserialized.getRawValue(propertyKey);
        Assertions.assertNotNull(value0);
        Assertions.assertInstanceOf(PropertyContainerSerializer.UndefinedPropertyValue.class, value0);
        Assertions.assertNull(value0.value());

        // Use the real 'getPropertyValue' method to ensure that the
        // 'UndefinedPropertyValue' is converted to a 'ProvidedPropertyValue',
        // now that the property is known.
        final var value1 = deserialized.getPropertyValue(property);
        Assertions.assertNotNull(value1);
        Assertions.assertEquals(5, value1.value());
        Assertions.assertInstanceOf(PropertyContainer.ProvidedPropertyValue.class, value1);

        // Ensure that the raw value has been updated.
        Assertions.assertEquals(value1, deserialized.getRawValue(propertyKey));
    }

    @Test
    void testDoubleSerialization()
    {
        Mockito.when(structureType.getProperties()).thenReturn(List.of());

        final String propertyKey = "animatedarchitecture:" + UUID.randomUUID();
        final String serialized = "{\"" + propertyKey + "\":{\"value\":11}}";
        final var property = mockProperty(propertyKey, Integer.class, 13);

        var deserialized = PropertyContainerSerializer.deserialize(structureType, serialized);
        for (int i = 0; i < 10; i++)
        {
            final String reserialized = PropertyContainerSerializer.serialize(deserialized);
            deserialized = PropertyContainerSerializer.deserialize(structureType, reserialized);
        }

        // The property was mocked and as such undefined.
        // Therefore, the raw value should be an 'UndefinedPropertyValue'.
        Assertions.assertInstanceOf(
            PropertyContainerSerializer.UndefinedPropertyValue.class,
            deserialized.getRawValue(propertyKey)
        );

        // When providing the property object, the value should be deserialized.
        final var value = deserialized.getPropertyValue(property);
        Assertions.assertInstanceOf(PropertyContainer.ProvidedPropertyValue.class, value);
        Assertions.assertEquals(11, value.value());
    }

    /**
     * Creates a new mocked property.
     *
     * @param key
     *     The key of the property.
     * @param type
     *     The type of the property.
     * @param defaultValue
     *     The default value of the property.
     * @param <T>
     *     The type of the property.
     * @return The mocked property.
     */
    private static <T> Property<T> mockProperty(String key, Class<T> type, T defaultValue)
    {
        final Property<T> property = Mockito.mock();
        Mockito.doReturn(key).when(property).getFullKey();
        Mockito.doReturn(type).when(property).getType();
        Mockito.doReturn(defaultValue).when(property).getDefaultValue();
        Mockito.doCallRealMethod().when(property).cast(Mockito.any());
        return property;
    }

    /**
     * Creates a new 'intermediate' map of properties and their values.
     * <p>
     * This map cannot be used directly to create a {@link PropertyContainer} but can be used by
     * {@link #createPropertyMap(Map)} to create a map that can be used to create a {@link PropertyContainer}.
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
                entry -> PropertyContainer.mapKey(entry.getKey()),
                entry -> PropertyContainer.mapUntypedValue(entry.getKey(), entry.getValue())
            ));
    }

    /**
     * Creates a new {@link PropertyContainer} from a map of properties and their values.
     *
     * @param map
     *     The map of properties and their values.
     * @return The new {@link PropertyContainer}.
     */
    private static PropertyContainer propertyContainerFromMap(Map<Property<?>, @Nullable Object> map)
    {
        return new PropertyContainer(new LinkedHashMap<>(createPropertyMap(map)));
    }
}
