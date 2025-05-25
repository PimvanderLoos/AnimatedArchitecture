package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import nl.pim16aap2.util.LazyValue;
import nl.pim16aap2.util.reflection.ReflectionBuilder;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyContainer.UnsetPropertyValue;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class PropertyContainerTest
{
    private static final String REQUIRED_PROPERTY_VALUE = "required_value";
    private static final Property<String> REQUIRED_PROPERTY = Property
        .builder(Constants.PLUGIN_NAME, "required_property", String.class)
        .withDefaultValue(REQUIRED_PROPERTY_VALUE)
        .isEditable()
        .build();

    private static final String OPTIONAL_PROPERTY_VALUE = "optional_value";
    private static final Property<String> OPTIONAL_PROPERTY = Property
        .builder(Constants.PLUGIN_NAME, "optional_property", String.class)
        .withDefaultValue(OPTIONAL_PROPERTY_VALUE)
        .isEditable()
        .build();

    private static final String UNSET_PROPERTY_VALUE = "unset_value";
    private static final Property<String> UNSET_PROPERTY = Property
        .builder(Constants.PLUGIN_NAME, "unset_property", String.class)
        .withDefaultValue(UNSET_PROPERTY_VALUE)
        .build();

    private PropertyContainer propertyContainer;

    @BeforeEach
    void setUp()
    {
        propertyContainer = PropertyContainer
            .of(REQUIRED_PROPERTY, REQUIRED_PROPERTY_VALUE, true,
                OPTIONAL_PROPERTY, OPTIONAL_PROPERTY_VALUE, false);
    }

    @Test
    void removeProperty_shouldRemoveExistingProperty()
    {
        // Execute
        final var value = propertyContainer.removeProperty(OPTIONAL_PROPERTY);

        // Verify
        assertThat(value.value()).isEqualTo(OPTIONAL_PROPERTY_VALUE);
        assertThat(propertyContainer.hasProperty(OPTIONAL_PROPERTY)).isFalse();
    }

    @Test
    void removeProperty_shouldThrowExceptionForRequiredProperty()
    {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> propertyContainer.removeProperty(REQUIRED_PROPERTY))
            .withMessage("Property '%s' cannot be removed!", REQUIRED_PROPERTY.getFullKey());
    }

    @Test
    void removeProperty_shouldReturnUnsetValueForUnsetProperty()
    {
        // Execute
        final IPropertyValue<?> value = propertyContainer.removeProperty(UNSET_PROPERTY);

        // Verify
        assertThat(value).isEqualTo(UnsetPropertyValue.INSTANCE);
        assertThat(propertyContainer.hasProperty(UNSET_PROPERTY)).isFalse();
    }

    @Test
    void removeProperty_shouldThrowExceptionWhenRemovedValueIsOfIncorrectType()
    {
        // Setup
        final Property<Integer> integerProperty = spy(Property
            .builder(Constants.PLUGIN_NAME, UUID.randomUUID().toString(), Integer.class)
            .withDefaultValue(42)
            .isEditable()
            .build());

        // Hack to be able to insert a value for the given key while bypassing the property registry.
        when(integerProperty.getFullKey()).thenReturn(OPTIONAL_PROPERTY.getFullKey());

        // Execute & Verify
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> propertyContainer.removeProperty(integerProperty))
            .withMessage("Old value '%s' for property '%s' is not of type '%s'! It has been removed regardless.",
                new PropertyContainer.ProvidedPropertyValue<>(String.class, OPTIONAL_PROPERTY_VALUE, false),
                integerProperty.getFullKey(),
                Integer.class
            );
    }

    @Test
    void setPropertyValue_shouldThrowExceptionForNullValueForRequiredProperty()
    {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> propertyContainer.setPropertyValue(REQUIRED_PROPERTY, null))
            .withMessage("Property '%s' cannot be removed!", REQUIRED_PROPERTY.getFullKey());
    }

    @Test
    void setPropertyValue_shouldRemoveOptionalPropertyForNullValue()
    {
        // Execute
        final var value = propertyContainer.setPropertyValue(OPTIONAL_PROPERTY, null);

        // Verify
        assertThat(value.value()).isEqualTo(OPTIONAL_PROPERTY_VALUE);
        assertThat(propertyContainer.hasProperty(OPTIONAL_PROPERTY)).isFalse();
    }

    @Test
    void setPropertyValue_shouldAddUnsetProperty()
    {
        // Setup
        final PropertyContainer emptyContainer = new PropertyContainer();

        // Execute
        final IPropertyValue<?> value = emptyContainer.setPropertyValue(UNSET_PROPERTY, "newValue");

        // Verify
        assertThat(value).isEqualTo(UnsetPropertyValue.INSTANCE);
        assertThat(emptyContainer.hasProperty(UNSET_PROPERTY)).isTrue();
    }

    @Test
    void setPropertyValue_shouldNotAddUnsetPropertyForNullValue()
    {
        // Execute
        final IPropertyValue<?> value = propertyContainer.setPropertyValue(UNSET_PROPERTY, null);

        // Verify
        assertThat(value).isEqualTo(UnsetPropertyValue.INSTANCE);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"new_value"})
    @SuppressWarnings("ReturnValueIgnored")
    void setPropertyValue_shouldResetCachedProperties(@Nullable String value)
    {
        // Setup
        final LazyValue<Set<PropertyValuePair<?>>> cachedProperties = getCachedProperties(propertyContainer);
        propertyContainer.iterator(); // Ensure that the cache is initialized

        // Execute
        propertyContainer.setPropertyValue(OPTIONAL_PROPERTY, value);

        // Verify
        assertThat(cachedProperties.isInitialized()).isFalse();
    }

    @Test
    void setPropertyValue_shouldSetRequiredFromPreviousValue()
    {
        // Setup
        final String newValue = "new_value";

        // Execute
        final var previousValue = propertyContainer.setPropertyValue(REQUIRED_PROPERTY, newValue);
        final var currentValue = propertyContainer.getRawValue(REQUIRED_PROPERTY.getFullKey());

        // Verify
        assertThat(previousValue.value()).isEqualTo(REQUIRED_PROPERTY_VALUE);

        assertThat(currentValue)
            .isNotNull()
            .satisfies(
                value -> assertThat(value.isSet()).isTrue(),
                value -> assertThat(value.value()).isEqualTo(newValue),
                value -> assertThat(value.value()).isEqualTo(newValue)
            );
    }

    @Test
    void setPropertyValue_shouldThrowExceptionWhenPreviousValueIsOfIncorrectType()
    {
        // Setup
        final int newValue = 100;
        final Property<Integer> integerProperty = spy(Property
            .builder(Constants.PLUGIN_NAME, UUID.randomUUID().toString(), Integer.class)
            .withDefaultValue(42)
            .isEditable()
            .build());

        // Hack to be able to insert a value for the given key while bypassing the property registry.
        when(integerProperty.getFullKey()).thenReturn(OPTIONAL_PROPERTY.getFullKey());

        // Execute & Verify
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> propertyContainer.setPropertyValue(integerProperty, newValue))
            .withMessage(
                "Old value '%s' for property '%s' is not of correct type '%s'! It has now been replaced by '%s'.",
                new PropertyContainer.ProvidedPropertyValue<>(String.class, OPTIONAL_PROPERTY_VALUE, false),
                integerProperty.getFullKey(),
                Integer.class,
                newValue
            );
    }

    @Test
    void setPropertyValue_shouldDeserializeUndefinedPreviousProperty()
    {
        final String initialValue = "initial_value";
        final IPropertyValue<String> deserializedValue =
            new PropertyContainer.ProvidedPropertyValue<>(String.class, initialValue, true);

        final var undefinedPropertyValue = newUndefinedPropertyValue(REQUIRED_PROPERTY, deserializedValue);
        final var containerWithUndefinedValue = of(REQUIRED_PROPERTY, undefinedPropertyValue);

        // Execute
        final var previousValue = containerWithUndefinedValue.setPropertyValue(
            REQUIRED_PROPERTY,
            REQUIRED_PROPERTY_VALUE
        );

        // Verify
        assertThat(previousValue).isEqualTo(deserializedValue);
        verify(undefinedPropertyValue).deserializeValue(REQUIRED_PROPERTY);
    }

    @Test
    void setUntypedPropertyValue_shouldRemovePropertyForNullValue()
    {
        // Execute
        final var value = propertyContainer.setUntypedPropertyValue(OPTIONAL_PROPERTY, null);

        // Verify
        assertThat(value.value()).isEqualTo(OPTIONAL_PROPERTY_VALUE);
        assertThat(propertyContainer.hasProperty(OPTIONAL_PROPERTY)).isFalse();
    }

    @Test
    void setUntypedPropertyValue_shouldSetValueForExistingProperty()
    {
        // Setup
        final String newValue = "new_value";

        // Execute
        final var previousValue = propertyContainer.setUntypedPropertyValue(OPTIONAL_PROPERTY, newValue);

        // Verify
        assertThat(previousValue.value()).isEqualTo(OPTIONAL_PROPERTY_VALUE);
        assertThat(propertyContainer.getPropertyValue(OPTIONAL_PROPERTY).value()).isEqualTo(newValue);
    }

    @Test
    void setUntypedPropertyValue_shouldThrowExceptionForTypeMismatch()
    {
        // Setup
        final Object invalidValue = new Object();

        // Execute & Verify
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> propertyContainer.setUntypedPropertyValue(OPTIONAL_PROPERTY, invalidValue))
            .withMessage("Provided value '%s' is not of type '%s' for property '%s'.",
                invalidValue,
                String.class,
                OPTIONAL_PROPERTY.getFullKey()
            );
    }

    @Test
    void getPropertyValue_shouldReturnUnsetValueForUnsetProperty()
    {
        // Execute
        final IPropertyValue<?> value = propertyContainer.getPropertyValue(UNSET_PROPERTY);

        // Verify
        assertThat(value)
            .satisfies(
                v -> assertThat(v).isInstanceOf(UnsetPropertyValue.class),
                v -> assertThat(v.isSet()).isFalse(),
                v -> assertThat(v.isRequired()).isFalse(),
                v -> assertThat(v.value()).isNull(),
                v -> assertThat(v.type()).isEqualTo(Object.class)
            );
    }

    @Test
    void getPropertyValue_shouldDeserializeAndUpdateUndefinedProperty()
    {
        // Setup
        final String initialValue = "initial_value";
        final IPropertyValue<String> deserializedValue =
            new PropertyContainer.ProvidedPropertyValue<>(String.class, initialValue, true);

        final var undefinedPropertyValue = newUndefinedPropertyValue(REQUIRED_PROPERTY, deserializedValue);
        final var containerWithUndefinedValue = of(REQUIRED_PROPERTY, undefinedPropertyValue);

        // Execute
        final var currentValue = containerWithUndefinedValue.getPropertyValue(REQUIRED_PROPERTY);

        // Verify
        assertThat(currentValue).isEqualTo(deserializedValue);
        verify(undefinedPropertyValue).deserializeValue(REQUIRED_PROPERTY);
    }

    @Test
    void snapshot_shouldCreateSnapshotOfCurrentProperties()
    {
        // Execute
        final PropertyContainerSnapshot snapshot = propertyContainer.snapshot();

        // Verify
        assertThat(snapshot.propertyCount()).isEqualTo(2);
        assertPropertyValue(
            snapshot.getPropertyValue(REQUIRED_PROPERTY), REQUIRED_PROPERTY_VALUE, true, String.class);
        assertPropertyValue(
            snapshot.getPropertyValue(OPTIONAL_PROPERTY), OPTIONAL_PROPERTY_VALUE, false, String.class);
    }

    @Test
    void addAll_shouldAddAllPropertiesFromAnotherContainer()
    {
        // Setup
        final String replacementValue = "replacement_value";

        final PropertyContainer firstContainer = PropertyContainer
            .of(REQUIRED_PROPERTY, REQUIRED_PROPERTY_VALUE, true,
                OPTIONAL_PROPERTY, OPTIONAL_PROPERTY_VALUE, false);

        final PropertyContainer otherContainer = PropertyContainer
            .of(UNSET_PROPERTY, UNSET_PROPERTY_VALUE, false,
                OPTIONAL_PROPERTY, replacementValue, true);

        // Execute
        firstContainer.addAll(otherContainer);

        // Verify
        assertThat(firstContainer.propertyCount()).isEqualTo(3);
        assertThat(firstContainer.getPropertyValue(REQUIRED_PROPERTY))
            .satisfies(
                value -> assertThat(value.value()).isEqualTo(REQUIRED_PROPERTY_VALUE),
                value -> assertThat(value.isSet()).isTrue(),
                value -> assertThat(value.isRequired()).isTrue(),
                value -> assertThat(value.type()).isEqualTo(String.class)
            );
        assertThat(firstContainer.getPropertyValue(OPTIONAL_PROPERTY))
            .satisfies(
                value -> assertThat(value.value()).isEqualTo(replacementValue),
                value -> assertThat(value.isSet()).isTrue(),
                value -> assertThat(value.isRequired()).isFalse(),
                value -> assertThat(value.type()).isEqualTo(String.class)
            );
        assertThat(firstContainer.getPropertyValue(UNSET_PROPERTY))
            .satisfies(
                value -> assertThat(value.value()).isEqualTo(UNSET_PROPERTY_VALUE),
                value -> assertThat(value.isSet()).isTrue(),
                value -> assertThat(value.isRequired()).isFalse(),
                value -> assertThat(value.type()).isEqualTo(String.class)
            );
    }

    @Test
    void hasProperties_shouldReturnTrueIfAllPropertiesArePresent()
    {
        assertThat(propertyContainer.hasProperties(Set.of(REQUIRED_PROPERTY, OPTIONAL_PROPERTY))).isTrue();
    }

    @Test
    void hasProperties_shouldReturnFalseIfAnyPropertyIsMissing()
    {
        assertThat(propertyContainer.hasProperties(Set.of(REQUIRED_PROPERTY, UNSET_PROPERTY))).isFalse();
    }

    @Test
    void hasProperties_shouldReturnTrueForEmptyCollection()
    {
        assertThat(propertyContainer.hasProperties(Set.of())).isTrue();
    }

    @Test
    void canRemoveProperty_shouldReturnTrueForOptionalProperty()
    {
        assertThat(propertyContainer.canRemoveProperty(OPTIONAL_PROPERTY)).isTrue();
    }

    @Test
    void canRemoveProperty_shouldReturnTrueForMissingProperty()
    {
        assertThat(propertyContainer.canRemoveProperty(UNSET_PROPERTY)).isTrue();
    }

    @Test
    void canRemoveProperty_shouldReturnFalseForRequiredProperty()
    {
        assertThat(propertyContainer.canRemoveProperty(REQUIRED_PROPERTY)).isFalse();
    }

    @Test
    void cast_shouldDeserializeUndefinedProperty()
    {
        // Setup
        final String initialValue = "initial_value";
        final IPropertyValue<String> deserializedValue =
            new PropertyContainer.ProvidedPropertyValue<>(String.class, initialValue, true);

        final var undefinedPropertyValue = newUndefinedPropertyValue(REQUIRED_PROPERTY, deserializedValue);

        // Execute
        final var result = PropertyContainer.cast(REQUIRED_PROPERTY, undefinedPropertyValue);

        // Verify
        assertThat(result.value()).isEqualTo(initialValue);
        verify(undefinedPropertyValue).deserializeValue(REQUIRED_PROPERTY);
    }

    @Test
    void cast_shouldReturnUnsetValueForUnsetProperty()
    {
        // Setup
        final var unsetValue = new PropertyContainer.UnsetPropertyValue();

        // Execute
        final IPropertyValue<?> result = PropertyContainer.cast(UNSET_PROPERTY, unsetValue);

        // Verify
        assertThat(result).isEqualTo(unsetValue);
    }

    @Test
    void forType_shouldReturnCorrectContainer()
    {
        // Setup
        final StructureType mockStructureType = Mockito.mock();

        Mockito.when(mockStructureType.getProperties()).thenReturn(List.of(REQUIRED_PROPERTY));

        // Execute
        final var container = PropertyContainer.forType(mockStructureType);

        // Verify
        assertThat(container.propertyCount()).isEqualTo(1);
        assertThat(container.getPropertyValue(REQUIRED_PROPERTY))
            .satisfies(
                value -> assertThat(value.value()).isEqualTo(REQUIRED_PROPERTY_VALUE),
                value -> assertThat(value.isSet()).isTrue(),
                value -> assertThat(value.isRequired()).isTrue(),
                value -> assertThat(value.type()).isEqualTo(String.class)
            );
    }

    @Test
    void equals_shouldMatchCopy()
    {
        // Setup
        final var otherContainer = PropertyContainer.of(propertyContainer);

        // Execute & Verify
        assertThat(otherContainer).isEqualTo(propertyContainer);
    }

    @Test
    void equals_shouldNotMatchDifferentValues()
    {
        // Setup
        final var otherContainer = PropertyContainer.of(propertyContainer);
        otherContainer.setPropertyValue(UNSET_PROPERTY, UUID.randomUUID().toString());

        // Execute & Verify
        assertThat(otherContainer).isNotEqualTo(propertyContainer);
    }

    @Test
    void hashCode_shouldMatchCopy()
    {
        // Setup
        final var otherContainer = PropertyContainer.of(propertyContainer);

        // Execute & Verify
        assertThat(otherContainer.hashCode()).isEqualTo(propertyContainer.hashCode());
    }

    @Test
    void hashCode_shouldNotMatchDifferentValues()
    {
        // Setup
        final var otherContainer = PropertyContainer.of(propertyContainer);
        otherContainer.setPropertyValue(UNSET_PROPERTY, UUID.randomUUID().toString());

        // Execute & Verify
        assertThat(otherContainer.hashCode()).isNotEqualTo(propertyContainer.hashCode());
    }

    @Test
    void of_copy_shouldMakeSimpleCopyOfContainer()
    {
        // Setup
        final var originalContainer = spy(PropertyContainer.of(REQUIRED_PROPERTY, REQUIRED_PROPERTY_VALUE, true));

        // Execute
        final var copiedContainer = PropertyContainer.of(originalContainer);

        // Verify
        assertThat(copiedContainer).isEqualTo(originalContainer);
        assertThat(copiedContainer.propertyCount()).isEqualTo(1);
        assertPropertyValue(
            copiedContainer.getPropertyValue(REQUIRED_PROPERTY), REQUIRED_PROPERTY_VALUE, true, String.class);
        verify(originalContainer, never()).stream();
    }

    @Test
    void of_copy_shouldMakeSimpleCopyOfSnapshot()
    {
        // Setup
        final var originalContainer = spy(new PropertyContainerSnapshot(
            Map.of(
                REQUIRED_PROPERTY.getFullKey(),
                new PropertyContainer.ProvidedPropertyValue<>(
                    REQUIRED_PROPERTY.getType(), REQUIRED_PROPERTY_VALUE, true))));

        // Execute
        final var copiedContainer = PropertyContainer.of(originalContainer);

        // Verify
        assertThat(copiedContainer.getMap()).isEqualTo(originalContainer.getMap());
        assertThat(copiedContainer.propertyCount()).isEqualTo(1);
        assertPropertyValue(
            copiedContainer.getPropertyValue(REQUIRED_PROPERTY), REQUIRED_PROPERTY_VALUE, true, String.class);
        verify(originalContainer, never()).stream();
    }

    @Test
    void of_1set_shouldCreateContainerWithProperties()
    {
        // Execute
        final var container = PropertyContainer.of(REQUIRED_PROPERTY, REQUIRED_PROPERTY_VALUE, true);

        // Verify
        assertThat(container.propertyCount()).isEqualTo(1);
        assertPropertyValue(
            container.getPropertyValue(REQUIRED_PROPERTY), REQUIRED_PROPERTY_VALUE, true, String.class);
    }

    @Test
    void of_2set_shouldCreateContainerWithProperties()
    {
        // Execute
        final var container = PropertyContainer
            .of(REQUIRED_PROPERTY, REQUIRED_PROPERTY_VALUE, true,
                OPTIONAL_PROPERTY, OPTIONAL_PROPERTY_VALUE, false);

        // Verify
        assertThat(container.propertyCount()).isEqualTo(2);
        assertPropertyValue(
            container.getPropertyValue(REQUIRED_PROPERTY), REQUIRED_PROPERTY_VALUE, true, String.class);
        assertPropertyValue(
            container.getPropertyValue(OPTIONAL_PROPERTY), OPTIONAL_PROPERTY_VALUE, false, String.class);
    }

    @Test
    void of_3set_shouldCreateContainerWithProperties()
    {
        // Execute
        final var container = PropertyContainer
            .of(REQUIRED_PROPERTY, REQUIRED_PROPERTY_VALUE, true,
                OPTIONAL_PROPERTY, OPTIONAL_PROPERTY_VALUE, false,
                UNSET_PROPERTY, UNSET_PROPERTY_VALUE, false);

        // Verify
        assertThat(container.propertyCount()).isEqualTo(3);
        assertPropertyValue(
            container.getPropertyValue(REQUIRED_PROPERTY), REQUIRED_PROPERTY_VALUE, true, String.class);
        assertPropertyValue(
            container.getPropertyValue(OPTIONAL_PROPERTY), OPTIONAL_PROPERTY_VALUE, false, String.class);
        assertPropertyValue(
            container.getPropertyValue(UNSET_PROPERTY), UNSET_PROPERTY_VALUE, false, String.class);
    }

    @Test
    void of_4set_shouldCreateContainerWithProperties()
    {
        // Setup
        final List<Property<String>> properties = createProperties(4);

        // Execute
        final var container = PropertyContainer.of(
            properties.get(0), "value_0", true,
            properties.get(1), "value_1", true,
            properties.get(2), "value_2", false,
            properties.get(3), "value_3", false
        );

        // Verify
        assertThat(container.propertyCount()).isEqualTo(4);
        assertPropertyValue(
            container.getPropertyValue(properties.get(0)), "value_0", true, String.class);
        assertPropertyValue(
            container.getPropertyValue(properties.get(1)), "value_1", true, String.class);
        assertPropertyValue(
            container.getPropertyValue(properties.get(2)), "value_2", false, String.class);
        assertPropertyValue(
            container.getPropertyValue(properties.get(3)), "value_3", false, String.class);
    }

    @Test
    void of_5set_shouldCreateContainerWithProperties()
    {
        // Setup
        final List<Property<String>> properties = createProperties(5);

        // Execute
        final var container = PropertyContainer.of(
            properties.get(0), "value_0", true,
            properties.get(1), "value_1", true,
            properties.get(2), "value_2", false,
            properties.get(3), "value_3", false,
            properties.get(4), "value_4", true
        );

        // Verify
        assertThat(container.propertyCount()).isEqualTo(5);
        assertPropertyValue(
            container.getPropertyValue(properties.get(0)), "value_0", true, String.class);
        assertPropertyValue(
            container.getPropertyValue(properties.get(1)), "value_1", true, String.class);
        assertPropertyValue(
            container.getPropertyValue(properties.get(2)), "value_2", false, String.class);
        assertPropertyValue(
            container.getPropertyValue(properties.get(3)), "value_3", false, String.class);
        assertPropertyValue(
            container.getPropertyValue(properties.get(4)), "value_4", true, String.class);
    }

    @Test
    void ofAll_shouldCreateContainerWithProperties()
    {
        // Setup
        final List<Property<String>> properties = createProperties(6);

        // Execute
        final var container = PropertyContainer.ofAll(
            properties.get(0), "value_0", true,
            properties.get(1), "value_1", true,
            properties.get(2), "value_2", false,
            properties.get(3), "value_3", false,
            properties.get(4), "value_4", true,
            properties.get(5), "value_5", false
        );

        // Verify
        assertThat(container.propertyCount()).isEqualTo(6);
        assertPropertyValue(
            container.getPropertyValue(properties.get(0)), "value_0", true, String.class);
        assertPropertyValue(
            container.getPropertyValue(properties.get(1)), "value_1", true, String.class);
        assertPropertyValue(
            container.getPropertyValue(properties.get(2)), "value_2", false, String.class);
        assertPropertyValue(
            container.getPropertyValue(properties.get(3)), "value_3", false, String.class);
        assertPropertyValue(
            container.getPropertyValue(properties.get(4)), "value_4", true, String.class);
        assertPropertyValue(
            container.getPropertyValue(properties.get(5)), "value_5", false, String.class);
    }

    @Test
    void ofAll_shouldReturnEmptyContainerForEmptyInput()
    {
        // Execute
        final var container = PropertyContainer.ofAll();

        // Verify
        assertThat(container.propertyCount()).isEqualTo(0);
    }

    @Test
    void ofAll_shouldThrowExceptionForIncorrectInputFormat()
    {
        // Execute & Verify
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> PropertyContainer.ofAll(REQUIRED_PROPERTY, REQUIRED_PROPERTY_VALUE, true, "extra"))
            .withMessage("Properties must be provided in pairs of (Property, value, isRequired)");
    }

    @Test
    void ofAll_shouldThrowExceptionForNonPropertyInPropertySlot()
    {
        // Execute & Verify
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> PropertyContainer.ofAll("not_a_property", REQUIRED_PROPERTY_VALUE, true))
            .withMessage("Expected object at index %d to be a Property, but it was %s", 0, String.class);
    }

    private static List<Property<String>> createProperties(int count)
    {
        final List<Property<String>> properties = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
        {
            properties.add(Property
                .builder(Constants.PLUGIN_NAME, "property_" + i, String.class)
                .withDefaultValue("value_" + i)
                .isEditable()
                .build());
        }
        return properties;
    }

    private static void assertPropertyValue(
        IPropertyValue<?> propertyValue,
        String expectedValue,
        boolean isRequired,
        Class<?> type)
    {
        assertThat(propertyValue)
            .satisfies(
                val -> assertThat(val.value()).isEqualTo(expectedValue),
                val -> assertThat(val.isSet()).isEqualTo(true),
                val -> assertThat(val.isRequired()).isEqualTo(isRequired),
                val -> assertThat(val.type()).isEqualTo(type)
            );
    }

    @SuppressWarnings("unchecked")
    private static LazyValue<Set<PropertyValuePair<?>>> getCachedProperties(PropertyContainer propertyContainer)
    {
        try
        {
            return (LazyValue<Set<PropertyValuePair<?>>>)
                ReflectionBuilder
                    .findField()
                    .inClass(PropertyContainer.class)
                    .withName("propertySet")
                    .setAccessible()
                    .get()
                    .get(propertyContainer);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static <T> PropertyContainerSerializer.UndefinedPropertyValue newUndefinedPropertyValue(
        Property<T> property,
        IPropertyValue<T> deserializedValue)
    {
        // Create a spy to verify the deserializeValue method is called
        final var undefinedPropertyValue = spy(new PropertyContainerSerializer.UndefinedPropertyValue(
            property.getFullKey(),
            mock(),
            true
        ));
        when(undefinedPropertyValue.deserializeValue(property)).thenReturn(deserializedValue);
        return undefinedPropertyValue;
    }

    private static PropertyContainer of(Property<?> property, IPropertyValue<?> value)
    {
        final Map<String, IPropertyValue<?>> map = HashMap.newHashMap(1);
        map.put(property.getFullKey(), value);
        return new PropertyContainer(map);
    }
}
