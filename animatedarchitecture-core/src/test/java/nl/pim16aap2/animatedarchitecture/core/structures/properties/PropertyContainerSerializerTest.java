package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import nl.altindag.log.LogCaptor;
import nl.pim16aap2.animatedarchitecture.core.structures.RedstoneMode;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.testing.annotations.WithLogCapture;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyContainerSerializer.UndefinedPropertyValue;
import static nl.pim16aap2.testing.assertions.LogCaptorAssert.assertThatLogCaptor;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@WithLogCapture
class PropertyContainerSerializerTest
{
    private static final Property<Integer> OPTIONAL_PROPERTY = Property
        .builder("external", "optional_property", Integer.class)
        .withDefaultValue(5)
        .isEditable()
        .build();

    @Mock
    private StructureType structureType;

    private PropertyContainer propertyContainer;

    private Map<Property<?>, Object> requiredProperties;

    private static final String SERIALIZED =
        "{\"animatedarchitecture:animation_speed_multiplier\":{\"value\":1.5}," +
            "\"animatedarchitecture:open_status\":{\"value\":true}," +
            "\"animatedarchitecture:redstone_mode\":{\"value\":\"POWERED_OPEN\"}," +
            "\"animatedarchitecture:rotation_point\":{\"value\":{\"x\":3,\"y\":1,\"z\":4}}," +
            "\"external:optional_property\":{\"value\":-6}}";

    @BeforeEach
    void setUp()
    {
        requiredProperties = Map.of(
            Property.ANIMATION_SPEED_MULTIPLIER, 1.5D,
            Property.OPEN_STATUS, true,
            Property.ROTATION_POINT, new Vector3Di(3, 1, 4),
            Property.REDSTONE_MODE, RedstoneMode.DEFAULT
        );

        final Map<Property<?>, Object> optionalProperties = Map.of(
            OPTIONAL_PROPERTY, -6
        );

        propertyContainer = propertyContainerFromMap(requiredProperties, optionalProperties);
    }

    @Test
    void serialize_shouldReturnCorrectJson()
    {
        // execute
        final String serialized = PropertyContainerSerializer.serialize(propertyContainer);

        // verify
        assertThat(serialized).isEqualTo(SERIALIZED);
    }

    @Test
    void serialize_shouldReturnCorrectJsonForStructureInput()
    {
        // setup
        final Structure structure = mock();

        when(structure.getPropertyContainerSnapshot()).thenReturn(propertyContainer.snapshot());

        // execute
        final String serialized = PropertyContainerSerializer.serialize(structure);

        // verify
        assertThat(serialized).isEqualTo(SERIALIZED);
    }

    @Test
    void serialize_shouldAvoidDoubleSerializationForUndefinedPropertyEntries()
    {
        // setup
        final String propertyKey = "animatedarchitecture:" + "undefined_property_" + UUID.randomUUID();
        final String valueJson = "{\"value\":5}";
        final String inputJson = "{\"" + propertyKey + "\":" + valueJson + "}";
        final JSONObject jsonObject = JSON.parseObject(valueJson);

        final var undefinedPropertyValue = new UndefinedPropertyValue(propertyKey, jsonObject, false);
        final PropertyContainer toSerialize = new PropertyContainer(Map.of(propertyKey, undefinedPropertyValue));

        // execute
        final String serialized = PropertyContainerSerializer.serialize(toSerialize);

        // verify
        assertThat(serialized).isEqualTo(inputJson);
    }

    @Test
    void deserialize_shouldReturnCorrectPropertyContainer()
    {
        // setup
        when(structureType.getProperties()).thenReturn(List.copyOf(requiredProperties.keySet()));

        // execute
        final PropertyContainer deserialized = PropertyContainerSerializer.deserialize(structureType, SERIALIZED);

        // verify
        assertThat(deserialized.getMap()).isEqualTo(propertyContainer.getMap());
    }

    @Test
    void deserialize_shouldCreateUndefinedPropertyValueForNonExistingProperty()
    {
        // setup
        final String propertyKey = "animatedarchitecture:" + "undefined_property_" + UUID.randomUUID();
        final String serialized = "{\"" + propertyKey + "\":{\"value\":5}}";

        // execute
        final PropertyContainer deserialized = PropertyContainerSerializer.deserialize(structureType, serialized);

        // verify
        //noinspection DataFlowIssue
        assertThat(deserialized.getRawValue(propertyKey))
            .asInstanceOf(InstanceOfAssertFactories.type(UndefinedPropertyValue.class))
            .satisfies(
                value -> assertThat(value.isSet()).isFalse(),
                value -> assertThat(value.value()).isNull(),
                value -> assertThat(value.type()).isEqualTo(Object.class)
            );
    }

    @Test
    void deserialize_shouldDeserializeDelayedUndefinedPropertyValue()
    {
        // setup
        final String propertyKey = "undefined_property_" + UUID.randomUUID();
        final String serialized = "{\"animatedarchitecture:" + propertyKey + "\":{\"value\":5}}";

        // execute
        final PropertyContainer deserialized = PropertyContainerSerializer.deserialize(structureType, serialized);
        final Property<Integer> property = Property
            .builder("animatedarchitecture", propertyKey, Integer.class)
            .withDefaultValue(0)
            .isEditable()
            .build();
        final IPropertyValue<?> value = deserialized.getPropertyValue(property);

        // verify
        assertThat(value)
            .isInstanceOf(PropertyContainer.ProvidedPropertyValue.class)
            .extracting(IPropertyValue::value)
            .isEqualTo(5);
    }

    @Test
    void deserialize_shouldLogMissingProperties(LogCaptor logCaptor)
    {
        logCaptor.setLogLevelToTrace();

        // setup
        final String serialized = "{\"animatedarchitecture:animation_speed_multiplier\":{\"value\":1.5}}";

        when(structureType.getProperties())
            .thenReturn(List.of(Property.ANIMATION_SPEED_MULTIPLIER, Property.OPEN_STATUS));

        // execute
        final PropertyContainer deserialized = PropertyContainerSerializer.deserialize(structureType, serialized);

        // verify
        assertThat(deserialized.hasProperty(Property.ANIMATION_SPEED_MULTIPLIER)).isTrue();
        assertThat(deserialized.hasProperty(Property.OPEN_STATUS)).isTrue();


        logCaptor.getTraceLogs().forEach(System.out::println);


        assertThatLogCaptor(logCaptor)
            .atFiner()
            .singleWithMessageExactly(
                "Property '%s' was not supplied for structure type '%s', using default value '%s'.",
                PropertyContainer.mapKey(Property.OPEN_STATUS),
                structureType,
                Property.OPEN_STATUS.getDefaultValue()
            );
    }

    @Test
    void deserializeValue_shouldThrowExceptionForPropertyKeyMismatch()
    {
        // setup
        final String propertyKey = "animatedarchitecture:" + "undefined_property_" + UUID.randomUUID();
        final String serialized = "{\"" + propertyKey + "\":{\"value\":5}}";
        final PropertyContainer deserialized = PropertyContainerSerializer.deserialize(structureType, serialized);
        final var value = Objects.requireNonNull((UndefinedPropertyValue) deserialized.getRawValue(propertyKey));

        // execute & Verify
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> value.deserializeValue(mock()))
            .withMessageContaining("Property key mismatch: Expected '%s', got '%s'", propertyKey, null);
    }

    @Test
    void deserialize_shouldThrowExceptionForInvalidJson()
    {
        // setup
        final String invalidJson = "asdfasdfasdf";

        // execute & Verify
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> PropertyContainerSerializer.deserialize(structureType, invalidJson))
            .withMessageContaining("Could not deserialize PropertyContainer from JSON: '%s'", invalidJson);
    }

    @Nested
    class UndefinedPropertyValueSerializerTest
    {
        @Mock
        private JSONWriter jsonWriter;

        private PropertyContainerSerializer.UndefinedPropertyValueSerializer undefinedPropertyValueSerializer;

        private String propertyKey;

        @BeforeEach
        void setUp()
        {
            propertyKey = "animatedarchitecture:" + "undefined_property_" + UUID.randomUUID();
            undefinedPropertyValueSerializer = new PropertyContainerSerializer.UndefinedPropertyValueSerializer();
        }

        @AfterEach
        void tearDown()
        {
            verifyNoMoreInteractions(jsonWriter);
        }

        @Test
        void write_shouldNotReserializeUndefinedProperties()
        {
            // setup
            final String valueJson = "{\"value\":5}";
            final var entry = new UndefinedPropertyValue(propertyKey, JSON.parseObject(valueJson), false);

            // execute
            undefinedPropertyValueSerializer.write(jsonWriter, entry);

            // verify
            verify(jsonWriter).write(entry.serializedValue());
        }

        @Test
        void write_shouldThrowExceptionForNotUndefinedPropertyValue()
        {
            // setup
            final PropertyContainer.ProvidedPropertyValue<String> providedValue =
                new PropertyContainer.ProvidedPropertyValue<>(String.class, "test", true);

            // execute & Verify
            assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> undefinedPropertyValueSerializer.write(jsonWriter, providedValue))
                .withMessage("Object '%s' is not an instance of UndefinedPropertyValue", providedValue);
        }
    }

    /**
     * Creates a new map of properties and their values from an intermediate map of properties and their values.
     *
     * @param input
     *     The intermediate map of properties and their values.
     * @param required
     *     Whether the properties are required or optional.
     * @return The map of property names and their property values.
     */
    private static Map<String, IPropertyValue<?>> createPropertyMap(Map<Property<?>, Object> input, boolean required)
    {
        return input
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                entry -> PropertyContainer.mapKey(entry.getKey()),
                entry -> PropertyContainer.mapUntypedValue(entry.getKey(), entry.getValue(), required)
            ));
    }

    /**
     * Creates a new {@link PropertyContainer} from a map of properties and their values.
     *
     * @param requiredProperties
     *     The map of required properties and their values.
     * @param optionalProperties
     *     The map of optional properties and their values.
     * @return The new {@link PropertyContainer}.
     */
    private static PropertyContainer propertyContainerFromMap(
        Map<Property<?>, Object> requiredProperties,
        Map<Property<?>, Object> optionalProperties)
    {
        final LinkedHashMap<String, IPropertyValue<?>> mapped =
            LinkedHashMap.newLinkedHashMap(requiredProperties.size() + optionalProperties.size());

        mapped.putAll(createPropertyMap(requiredProperties, true));
        mapped.putAll(createPropertyMap(optionalProperties, false));

        // Sort the map by key to ensure consistent ordering to make the tests deterministic.
        final LinkedHashMap<String, IPropertyValue<?>> sorted = mapped
            .entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .collect(Collectors
                .toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (x, y) -> y,
                    LinkedHashMap::new
                ));

        return new PropertyContainer(sorted);
    }
}
