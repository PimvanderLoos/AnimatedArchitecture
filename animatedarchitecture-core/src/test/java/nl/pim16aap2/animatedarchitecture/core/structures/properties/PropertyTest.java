package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import nl.pim16aap2.animatedarchitecture.core.api.NamespacedKey;
import nl.pim16aap2.util.reflection.ReflectionBuilder;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class PropertyTest
{
    private static final Property<String> CREATED_PROPERTY = new Property<>(
        NamespacedKey.of("animatedarchitecture:property_test_preexisting"),
        String.class,
        "defaultValue",
        PropertyAccessLevel.USER_EDITABLE,
        List.of(PropertyScope.REDSTONE),
        true
    );

    @Test
    @SuppressWarnings("ConstantValue")
    void cast_shouldReturnNullForNullValue()
    {
        // Execute
        final @Nullable String castedValue = CREATED_PROPERTY.cast(null);

        // Verify
        assertThat(castedValue).isNull();
    }

    @Test
    void cast_shouldReturnCastValueForValidType()
    {
        // Setup
        final Object value = "testValue";

        // Execute
        final String castedValue = CREATED_PROPERTY.cast(value);

        // Verify
        assertThat(castedValue).isEqualTo(value);
    }

    @Test
    void cast_shouldThrowExceptionForInvalidType()
    {
        // Setup
        final Object value = 123;

        // Execute & Verify
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> CREATED_PROPERTY.cast(value))
            .withMessage(
                "Provided value '%s' is not of type '%s' for property '%s'.",
                value,
                CREATED_PROPERTY.getType(),
                CREATED_PROPERTY.getFullKey()
            );
    }

    @Test
    void builder_shouldCreatePropertyWithBuilderPattern()
    {
        // Setup
        final NamespacedKey key = NamespacedKey.of("animatedarchitecture:property_builder_test");

        // Execute
        final Property<String> property = Property
            .builder(key, String.class)
            .withDefaultValue("defaultValue")
            .isHidden()
            .withPropertyScopes(PropertyScope.REDSTONE, PropertyScope.ANIMATION)
            .canBeAddedByUser()
            .build();

        // Verify
        assertThat(property.getNamespacedKey()).isEqualTo(key);
        assertThat(property.getType()).isEqualTo(String.class);
        assertThat(property.getDefaultValue()).isEqualTo("defaultValue");
        assertThat(property.getPropertyAccessLevel()).isEqualTo(PropertyAccessLevel.HIDDEN);
        assertThat(property.getPropertyScopes()).containsExactly(PropertyScope.REDSTONE, PropertyScope.ANIMATION);
        assertThat(property.canBeAddedByUser()).isTrue();
    }

    @Test
    void builder_shouldHaveDefaultValues()
    {
        // Setup
        final NamespacedKey key = NamespacedKey.of("animatedarchitecture:property_builder_default_test");

        // Execute
        final Property<String> property = Property
            .builder(key, String.class)
            .withDefaultValue("defaultValue")
            .build();

        // Verify
        assertThat(property.getNamespacedKey()).isEqualTo(key);
        assertThat(property.getType()).isEqualTo(String.class);
        assertThat(property.getDefaultValue()).isEqualTo("defaultValue");
        assertThat(property.getPropertyAccessLevel()).isEqualTo(PropertyAccessLevel.READ_ONLY);
        assertThat(property.getPropertyScopes()).isEmpty();
        assertThat(property.canBeAddedByUser()).isFalse();
    }

    @Test
    void fromKey_shouldReturnNullForNonExistentKey()
    {
        // Execute
        final Property<?> property = Property.fromKey(NamespacedKey.of("animatedarchitecture:non_existent_property"));

        // Verify
        assertThat(property).isNull();
    }

    @Test
    void fromKey_shouldReturnPropertyForExistingKey()
    {
        // Execute
        final Property<?> retrievedProperty = Property.fromKey(CREATED_PROPERTY.getNamespacedKey());

        // Verify
        assertThat(retrievedProperty).isEqualTo(CREATED_PROPERTY);
    }

    @Test
    void registry_getDebugInformation_shouldNotThrowException()
    {
        // Execute
        final String debugInfo = Property.getDebuggableRegistry().getDebugInformation();

        // Verify
        assertThat(debugInfo)
            .contains("- " + CREATED_PROPERTY.getFullKey());
    }

    @Test
    void constructor_shouldWorkForDuplicateKeyForSameProperty()
    {
        // Execute
        final Property<String> clonedProperty = Property.builder(CREATED_PROPERTY.getNamespacedKey(), String.class)
            .withDefaultValue(CREATED_PROPERTY.getDefaultValue())
            .withPropertyAccessLevel(CREATED_PROPERTY.getPropertyAccessLevel())
            .withPropertyScopes(CREATED_PROPERTY.getPropertyScopes().toArray(new PropertyScope[0]))
            .canBeAddedByUser(CREATED_PROPERTY.canBeAddedByUser())
            .build();

        // Verify
        assertThat(clonedProperty).isEqualTo(CREATED_PROPERTY);
    }

    @Test
    void constructor_shouldThrowExceptionForDuplicateKey()
    {
        // Setup
        final NamespacedKey dupeKey = NamespacedKey.of("animatedarchitecture:duplicate_property");
        final Property<String> property = new Property<>(
            dupeKey,
            String.class,
            "defaultValue",
            PropertyAccessLevel.USER_EDITABLE,
            List.of(),
            true
        );

        // Execute & Verify
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> new Property<>(
                dupeKey,
                String.class,
                "anotherDefaultValue",
                PropertyAccessLevel.USER_EDITABLE,
                List.of(),
                true))
            .withMessage(
                "Cannot register property '%s' because a property with that key already exists: %s",
                property.toString().replace("defaultValue=defaultValue", "defaultValue=anotherDefaultValue"),
                property
            );
    }

    @Test
    void constructor_shouldCreatePropertyWithValidInput()
    {
        // Setup
        final NamespacedKey testKey = NamespacedKey.of("animatedarchitecture:property_ctor_test");

        // Execute
        final Property<String> property = new Property<>(
            testKey,
            String.class,
            "defaultValue",
            PropertyAccessLevel.USER_EDITABLE,
            List.of(PropertyScope.REDSTONE),
            true
        );

        // Verify
        assertThat(property.getNamespacedKey()).isEqualTo(testKey);
        assertThat(property.getType()).isEqualTo(String.class);
        assertThat(property.getDefaultValue()).isEqualTo("defaultValue");
        assertThat(property.getPropertyAccessLevel()).isEqualTo(PropertyAccessLevel.USER_EDITABLE);
        assertThat(property.getPropertyScopes()).containsExactly(PropertyScope.REDSTONE);
        assertThat(property.canBeAddedByUser()).isTrue();
    }

    @Test
    void constructor_shouldThrowExceptionForTypeMismatch()
    {
        // Setup
        final NamespacedKey testKey = NamespacedKey.of("animatedarchitecture:property_ctor_test");

        var ctor = ReflectionBuilder
            .findConstructor()
            .inClass(Property.class)
            .withParameters(NamespacedKey.class,
                Class.class,
                Object.class,
                PropertyAccessLevel.class,
                List.class,
                boolean.class)
            .setAccessible()
            .get();

        // Execute & Verify
        assertThatExceptionOfType(InvocationTargetException.class)
            .isThrownBy(() -> ctor.newInstance(
                testKey,
                String.class,
                12,
                PropertyAccessLevel.USER_EDITABLE,
                List.of(),
                true))
            .havingCause()
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .withNoCause()
            .withMessage("Default value 12 is not of type java.lang.String");
    }

    @Test
    void constructor_shouldThrowExceptionForNullNamespacedKey()
    {
        //noinspection DataFlowIssue
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new Property<>(
                null,
                String.class,
                "defaultValue",
                PropertyAccessLevel.USER_EDITABLE,
                List.of(),
                true))
            .withMessage("NamespacedKey must not be null!");
    }

    @Test
    void constructor_shouldThrowExceptionForNullType()
    {
        // Setup
        final NamespacedKey testKey = NamespacedKey.of("animatedarchitecture:property_ctor_test");

        // Execute & Verify
        //noinspection DataFlowIssue
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new Property<>(
                testKey,
                null,
                "defaultValue",
                PropertyAccessLevel.USER_EDITABLE,
                List.of(),
                true))
            .withMessage("Property type must not be null!");
    }

    @Test
    void constructor_shouldThrowExceptionForNullDefaultValue()
    {
        // Setup
        final NamespacedKey testKey = NamespacedKey.of("animatedarchitecture:property_ctor_test");

        // Execute & Verify
        //noinspection DataFlowIssue
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new Property<>(
                testKey,
                String.class,
                null,
                PropertyAccessLevel.USER_EDITABLE,
                List.of(),
                true))
            .withMessage("Default Property value must not be null!");
    }

    @Test
    void constructor_shouldThrowExceptionForNullPropertyAccessLevel()
    {
        // Setup
        final NamespacedKey testKey = NamespacedKey.of("animatedarchitecture:property_ctor_test");

        // Execute & Verify
        //noinspection DataFlowIssue
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new Property<>(
                testKey,
                String.class,
                "defaultValue",
                null,
                List.of(),
                true))
            .withMessage("Property access level must not be null!");
    }

    @Test
    void constructor_shouldThrowExceptionForNullPropertyScopes()
    {
        // Setup
        final NamespacedKey testKey = NamespacedKey.of("animatedarchitecture:property_ctor_test");

        // Execute & Verify
        //noinspection DataFlowIssue
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new Property<>(
                testKey,
                String.class,
                "defaultValue",
                PropertyAccessLevel.USER_EDITABLE,
                null,
                true));
    }
}
