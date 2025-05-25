package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class PropertyContainerSnapshotTest
{
    private static final String PROPERTY_VALUE = "propertyValue";
    private static final Property<String> PROPERTY = Property
        .builder(Constants.PLUGIN_NAME, "snapshot-test-property", String.class)
        .withDefaultValue(PROPERTY_VALUE)
        .build();

    private static final String UNSET_PROPERTY_VALUE = "unsetPropertyValue";
    private static final Property<String> UNSET_PROPERTY = Property
        .builder(Constants.PLUGIN_NAME, "unset-snapshot-test-property", String.class)
        .withDefaultValue(UNSET_PROPERTY_VALUE)
        .build();

    @Test
    void constructor_shouldCreateDeepCopyOfPropertyMap()
    {
        // Setup
        final String key = PROPERTY.getFullKey();
        final String initialValue = "initialValue";
        final String updatedValue = "updatedValue";

        final Map<String, IPropertyValue<?>> propertyMap = new LinkedHashMap<>();
        propertyMap.put(key, new PropertyContainer.ProvidedPropertyValue<>(String.class, initialValue, false));

        // Execute
        final var snapshot = new PropertyContainerSnapshot(propertyMap);
        propertyMap.put(key, new PropertyContainer.ProvidedPropertyValue<>(String.class, updatedValue, false));

        // Verify
        assertThat(snapshot.propertyCount()).isEqualTo(1);
        assertThat(snapshot.getMap().get(key).value()).isEqualTo(initialValue);

        assertThat(snapshot.getPropertySet().size()).isEqualTo(1);
        assertThat(snapshot.getPropertySet().iterator().next().value().value()).isEqualTo(initialValue);
    }

    @Test
    void getPropertyValue_shouldReturnCorrectValueForExistingProperty()
    {
        // Setup
        final String expectedValue = "expectedValue";
        final var snapshot = of(PROPERTY, expectedValue, false);

        // Execute
        final var actualValue = snapshot.getPropertyValue(PROPERTY).value();

        // Verify
        assertThat(actualValue).isEqualTo(expectedValue);
    }

    @Test
    void getPropertyValue_shouldReturnUnsetValueForNonExistingProperty()
    {
        // Setup
        final var snapshot = new PropertyContainerSnapshot(new LinkedHashMap<>());

        // Execute
        final var actualValue = snapshot.getPropertyValue(PROPERTY).value();

        // Verify
        assertThat(actualValue).isNull();
    }

    @Test
    void hasProperty_shouldReturnTrueForExistingProperty()
    {
        // Setup
        final var snapshot = of(PROPERTY, PROPERTY_VALUE, false);

        // Execute & Verify
        assertThat(snapshot.hasProperty(PROPERTY)).isTrue();
    }

    @Test
    void hasProperty_shouldReturnFalseForNonExistingProperty()
    {
        // Setup
        final var snapshot = of(PROPERTY, PROPERTY_VALUE, false);

        // Execute & Verify
        assertThat(snapshot.hasProperty(UNSET_PROPERTY)).isFalse();
    }

    @Test
    void hasProperties_shouldReturnTrueForAllExistingProperties()
    {
        // Setup
        final var snapshot = PropertyContainer
            .of(PROPERTY, PROPERTY_VALUE, false,
                UNSET_PROPERTY, UNSET_PROPERTY_VALUE, false)
            .snapshot();

        // Execute & Verify
        assertThat(snapshot.hasProperties(List.of(PROPERTY, UNSET_PROPERTY))).isTrue();
    }

    @Test
    void hasProperties_shouldReturnFalseIfAnyPropertyIsMissing()
    {
        // Setup
        final Property<Integer> missingProperty = Property
            .builder(Constants.PLUGIN_NAME, "missing-property", Integer.class)
            .withDefaultValue(42)
            .build();

        final var snapshot = PropertyContainer
            .of(PROPERTY, PROPERTY_VALUE, false,
                UNSET_PROPERTY, UNSET_PROPERTY_VALUE, false)
            .snapshot();

        // Execute & Verify
        assertThat(snapshot.hasProperties(List.of(PROPERTY, UNSET_PROPERTY, missingProperty))).isFalse();
    }

    @SafeVarargs
    private static <T> PropertyContainerSnapshot of(Property<T> property, T value, boolean required, T... reified)
    {
        if (reified.length > 1)
            throw new IllegalArgumentException("Do not pass any arguments to 'reified'.");

        final Map<String, IPropertyValue<?>> propertyMap = new LinkedHashMap<>();
        //noinspection unchecked
        propertyMap.put(
            property.getFullKey(),
            new PropertyContainer.ProvidedPropertyValue<>(
                (Class<T>) reified.getClass().getComponentType(), value, required)
        );
        return new PropertyContainerSnapshot(propertyMap);
    }
}
