package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.NamespacedKey;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import nl.pim16aap2.util.reflection.ReflectionBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class PropertyTest
{
    private static final Property<String> CREATED_PROPERTY = Property
        .builder(Constants.PLUGIN_NAME, "property_test_preexisting", String.class)
        .withDefaultValue("defaultValue")
        .withUserAccessLevels(PropertyAccessLevel.READ, PropertyAccessLevel.ADD, PropertyAccessLevel.EDIT)
        .withPropertyScopes(PropertyScope.REDSTONE)
        .build();

    @Test
    void userHasAccessLevel_shouldReturnTrueForGrantedAccessLevel()
    {
        // setup
        final Property<String> property = newPropertyBuilder()
            .withUserAccessLevels(PropertyAccessLevel.READ, PropertyAccessLevel.EDIT)
            .build();

        // execute & verify
        assertThat(property.userHasAccessLevel(PropertyAccessLevel.READ)).isTrue();
        assertThat(property.userHasAccessLevel(PropertyAccessLevel.EDIT)).isTrue();
    }

    @Test
    void userHasAccessLevel_shouldReturnFalseForNotGrantedAccessLevel()
    {
        // setup
        final Property<String> property = newPropertyBuilder()
            .withUserAccessLevels(PropertyAccessLevel.READ)
            .build();

        // execute & verify
        assertThat(property.userHasAccessLevel(PropertyAccessLevel.EDIT)).isFalse();
    }

    @Test
    void adminHasAccessLevel_shouldReturnTrueForGrantedAccessLevel()
    {
        // setup
        final Property<String> property = newPropertyBuilder()
            .withAdminAccessLevels(PropertyAccessLevel.EDIT, PropertyAccessLevel.ADD)
            .build();

        // execute & verify
        assertThat(property.adminHasAccessLevel(PropertyAccessLevel.EDIT)).isTrue();
        assertThat(property.adminHasAccessLevel(PropertyAccessLevel.ADD)).isTrue();
    }

    @Test
    void adminHasAccessLevel_shouldReturnFalseForNotGrantedAccessLevel()
    {
        // setup
        final Property<String> property = newPropertyBuilder()
            .withAdminAccessLevels(PropertyAccessLevel.EDIT)
            .build();

        // execute & verify
        assertThat(property.adminHasAccessLevel(PropertyAccessLevel.READ)).isFalse();
    }

    @Test
    void adminHasAccessLevel_shouldIncludeUserAccessLevel()
    {
        // setup
        final Property<String> property = newPropertyBuilder()
            .withUserAccessLevels(PropertyAccessLevel.READ)
            .withAdminAccessLevels(PropertyAccessLevel.EDIT)
            .build();

        // execute & verify
        assertThat(property.adminHasAccessLevel(PropertyAccessLevel.READ)).isTrue();
        assertThat(property.adminHasAccessLevel(PropertyAccessLevel.EDIT)).isTrue();
    }

    @Test
    void hasAccessLevel_withPermissionLevel_shouldReturnTrueForUserWithGrantedLevel()
    {
        // setup
        final Property<String> property = newPropertyBuilder()
            .withUserAccessLevels(PropertyAccessLevel.READ)
            .build();

        // execute & verify
        assertThat(property.hasAccessLevel(PermissionLevel.USER, PropertyAccessLevel.READ)).isTrue();
    }

    @Test
    void hasAccessLevel_withPermissionLevel_shouldReturnFalseForUserWithoutGrantedLevel()
    {
        // setup
        final Property<String> property = newPropertyBuilder()
            .withUserAccessLevels(PropertyAccessLevel.READ)
            .build();

        // execute & verify
        assertThat(property.hasAccessLevel(PermissionLevel.USER, PropertyAccessLevel.EDIT)).isFalse();
    }

    @Test
    void hasAccessLevel_withPermissionLevel_shouldReturnTrueForAdminWithGrantedLevel()
    {
        // setup
        final Property<String> property = newPropertyBuilder()
            .withAdminAccessLevels(PropertyAccessLevel.EDIT)
            .build();

        // execute & verify
        assertThat(property.hasAccessLevel(PermissionLevel.ADMIN, PropertyAccessLevel.EDIT)).isTrue();
    }

    @Test
    void hasAccessLevel_withPermissionLevel_shouldReturnTrueForCreatorWithGrantedLevel()
    {
        // setup
        final Property<String> property = newPropertyBuilder()
            .withAdminAccessLevels(PropertyAccessLevel.EDIT)
            .build();

        // execute & verify
        assertThat(property.hasAccessLevel(PermissionLevel.CREATOR, PropertyAccessLevel.EDIT)).isTrue();
    }

    @Test
    void hasAccessLevel_withPermissionLevel_shouldReturnTrueForCreatorWithLevelGrantedToUsers()
    {
        // setup
        final Property<String> property = newPropertyBuilder()
            .withUserAccessLevels(PropertyAccessLevel.EDIT)
            .build();

        // execute & verify
        assertThat(property.hasAccessLevel(PermissionLevel.CREATOR, PropertyAccessLevel.EDIT)).isTrue();
    }

    @Test
    void hasAccessLevel_withPermissionLevel_shouldReturnFalseForNoAccessPermissionLevel()
    {
        // setup
        final Property<String> property = newPropertyBuilder()
            .withUserAccessLevels(PropertyAccessLevel.READ)
            .withAdminAccessLevels(PropertyAccessLevel.EDIT)
            .build();

        // execute & verify
        assertThat(property.hasAccessLevel(PermissionLevel.NO_PERMISSION, PropertyAccessLevel.READ)).isFalse();
        assertThat(property.hasAccessLevel(PermissionLevel.NO_PERMISSION, PropertyAccessLevel.EDIT)).isFalse();
        assertThat(property.hasAccessLevel(PermissionLevel.NO_PERMISSION, PropertyAccessLevel.ADD)).isFalse();
        assertThat(property.hasAccessLevel(PermissionLevel.NO_PERMISSION, PropertyAccessLevel.REMOVE)).isFalse();
    }

    @Test
    void getAccessLevel_shouldReturnAdminAccessLevelForAdmin()
    {
        // setup
        final Property<String> property = newPropertyBuilder()
            .withUserAccessLevels(PropertyAccessLevel.READ)
            .withAdminAccessLevels(PropertyAccessLevel.EDIT)
            .build();

        // execute
        final int accessLevel = property.getAccessLevel(PermissionLevel.ADMIN);

        // verify
        assertThat(accessLevel).isEqualTo(property.getAdminAccessLevel());
    }

    @Test
    void getAccessLevel_shouldReturnAdminAccessLevelForCreator()
    {
        // setup
        final Property<String> property = newPropertyBuilder()
            .withUserAccessLevels(PropertyAccessLevel.READ)
            .withAdminAccessLevels(PropertyAccessLevel.EDIT)
            .build();

        // execute
        final int accessLevel = property.getAccessLevel(PermissionLevel.CREATOR);

        // verify
        assertThat(accessLevel).isEqualTo(property.getAdminAccessLevel());
    }

    @Test
    void getAccessLevel_shouldReturnUserAccessLevelForUser()
    {
        // setup
        final Property<String> property = newPropertyBuilder()
            .withUserAccessLevels(PropertyAccessLevel.READ)
            .withAdminAccessLevels(PropertyAccessLevel.EDIT)
            .build();

        // execute
        final int accessLevel = property.getAccessLevel(PermissionLevel.USER);

        // verify
        assertThat(accessLevel).isEqualTo(property.getUserAccessLevel());
    }

    @Test
    void getAccessLevel_shouldReturnZeroForNoPermission()
    {
        // setup
        final Property<String> property = newPropertyBuilder()
            .withUserAccessLevels(PropertyAccessLevel.READ)
            .withAdminAccessLevels(PropertyAccessLevel.EDIT)
            .build();

        // execute
        final int accessLevel = property.getAccessLevel(PermissionLevel.NO_PERMISSION);

        // verify
        assertThat(accessLevel).isEqualTo(0);
    }

    @Test
    @SuppressWarnings("ConstantValue")
    void cast_shouldReturnNullForNullValue()
    {
        // execute
        final String castedValue = CREATED_PROPERTY.cast(null);

        // Verify
        assertThat(castedValue).isNull();
    }

    @Test
    void cast_shouldReturnCastValueForValidType()
    {
        // setup
        final Object value = "testValue";

        // execute
        final String castedValue = CREATED_PROPERTY.cast(value);

        // Verify
        assertThat(castedValue).isEqualTo(value);
    }

    @Test
    void cast_shouldThrowExceptionForInvalidType()
    {
        // setup
        final Object value = 123;

        // execute & verify
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
        // setup
        final NamespacedKey key = NamespacedKey.of("animatedarchitecture:property_builder_test");
        final PropertyAccessLevel[] levels = {
            PropertyAccessLevel.READ,
            PropertyAccessLevel.ADD,
            PropertyAccessLevel.EDIT
        };

        // execute
        final Property<String> property = Property
            .builder(key, String.class)
            .withDefaultValue("defaultValue")
            .withUserAccessLevels(levels)
            .withPropertyScopes(PropertyScope.REDSTONE, PropertyScope.ANIMATION)
            .build();

        // Verify
        assertThat(property.getNamespacedKey()).isEqualTo(key);
        assertThat(property.getType()).isEqualTo(String.class);
        assertThat(property.getDefaultValue()).isEqualTo("defaultValue");
        assertThat(property.getAdminAccessLevel()).isEqualTo(PropertyAccessLevel.getFlagOf(levels));
        assertThat(property.getUserAccessLevel()).isEqualTo(PropertyAccessLevel.getFlagOf(levels));
        assertThat(property.getPropertyScopes()).containsExactly(PropertyScope.REDSTONE, PropertyScope.ANIMATION);
    }

    @Test
    void builder_shouldHaveDefaultValues()
    {
        // setup
        final NamespacedKey key = NamespacedKey.of("animatedarchitecture:property_builder_default_test");

        // execute
        final Property<String> property = Property
            .builder(key, String.class)
            .withDefaultValue("defaultValue")
            .build();

        // Verify
        assertThat(property.getNamespacedKey()).isEqualTo(key);
        assertThat(property.getType()).isEqualTo(String.class);
        assertThat(property.getDefaultValue()).isEqualTo("defaultValue");
        assertThat(property.getUserAccessLevel()).isEqualTo(0);
        assertThat(property.getAdminAccessLevel()).isEqualTo(0);
        assertThat(property.getPropertyScopes()).isEmpty();
    }

    @Test
    void fromKey_shouldReturnNullForNonExistentKey()
    {
        // execute
        final Property<?> property = Property.fromKey(NamespacedKey.of("animatedarchitecture:non_existent_property"));

        // Verify
        assertThat(property).isNull();
    }

    @Test
    void fromKey_shouldReturnPropertyForExistingKey()
    {
        // execute
        final Property<?> retrievedProperty = Property.fromKey(CREATED_PROPERTY.getNamespacedKey());

        // Verify
        assertThat(retrievedProperty).isEqualTo(CREATED_PROPERTY);
    }

    @Test
    void registry_getDebugInformation_shouldNotThrowException()
    {
        // execute
        final String debugInfo = Property.getDebuggableRegistry().getDebugInformation();

        // Verify
        assertThat(debugInfo)
            .contains("- " + CREATED_PROPERTY.getFullKey());
    }

    @Test
    void constructor_shouldWorkForDuplicateKeyForSameProperty()
    {
        // execute
        final Property<String> clonedProperty = Property
            .builder(CREATED_PROPERTY.getNamespacedKey(), String.class)
            .withDefaultValue(CREATED_PROPERTY.getDefaultValue())
            .withUserAccessLevels(CREATED_PROPERTY.getUserAccessLevel())
            .withAdminAccessLevels(CREATED_PROPERTY.getAdminAccessLevel())
            .withPropertyScopes(CREATED_PROPERTY.getPropertyScopes().toArray(new PropertyScope[0]))
            .build();

        // Verify
        assertThat(clonedProperty).isEqualTo(CREATED_PROPERTY);
    }

    @Test
    void constructor_shouldThrowExceptionForDuplicateKey()
    {
        // setup
        final NamespacedKey dupeKey = NamespacedKey.of("animatedarchitecture:duplicate_property");
        final Property<String> property = new Property<>(
            dupeKey,
            String.class,
            "defaultValue",
            0, 0,
            List.of(),
            iLoc -> ""
        );

        // execute & verify
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> new Property<>(
                dupeKey,
                String.class,
                "anotherDefaultValue",
                0, 0,
                List.of(),
                iLoc -> ""
            ))
            .withMessage(
                "Cannot register property '%s' because a property with that key already exists: %s",
                property.toString().replace("defaultValue=defaultValue", "defaultValue=anotherDefaultValue"),
                property
            );
    }

    @Test
    void constructor_shouldCreatePropertyWithValidInput()
    {
        // setup
        final NamespacedKey testKey = NamespacedKey.of("animatedarchitecture:property_ctor_test");
        final int inputUserLevel = PropertyAccessLevel.getFlagOf(PropertyAccessLevel.READ);
        final int inputAdminLevel = PropertyAccessLevel.getFlagOf(PropertyAccessLevel.EDIT, PropertyAccessLevel.ADD);
        final int combinedLevel = inputUserLevel | inputAdminLevel;

        // execute
        final Property<String> property = new Property<>(
            testKey,
            String.class,
            "defaultValue",
            inputUserLevel,
            inputAdminLevel,
            List.of(PropertyScope.REDSTONE),
            iLoc -> ""
        );

        // Verify
        assertThat(property.getNamespacedKey()).isEqualTo(testKey);
        assertThat(property.getType()).isEqualTo(String.class);
        assertThat(property.getDefaultValue()).isEqualTo("defaultValue");
        assertThat(property.getUserAccessLevel()).isEqualTo(inputUserLevel);
        assertThat(property.getAdminAccessLevel()).isEqualTo(combinedLevel);
        assertThat(property.getPropertyScopes()).containsExactly(PropertyScope.REDSTONE);
    }

    @Test
    void constructor_shouldThrowExceptionForTypeMismatch()
    {
        // setup
        final NamespacedKey testKey = NamespacedKey.of("animatedarchitecture:property_ctor_test");

        var ctor = ReflectionBuilder
            .findConstructor()
            .inClass(Property.class)
            .withParameters(NamespacedKey.class,
                Class.class,
                Object.class,
                int.class,
                int.class,
                List.class,
                Function.class
            )
            .setAccessible()
            .get();

        // execute & verify
        assertThatExceptionOfType(InvocationTargetException.class)
            .isThrownBy(() -> ctor.newInstance(
                testKey,
                String.class,
                12,
                0, 0,
                List.of(),
                Function.identity()
            ))
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
                0, 0,
                List.of(),
                iLoc -> ""
            ))
            .withMessage("NamespacedKey must not be null!");
    }

    @Test
    void constructor_shouldThrowExceptionForNullType()
    {
        // setup
        final NamespacedKey testKey = NamespacedKey.of("animatedarchitecture:property_ctor_test");

        // execute & verify
        //noinspection DataFlowIssue
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new Property<>(
                testKey,
                null,
                "defaultValue",
                0, 0,
                List.of(),
                iLoc -> ""
            ))
            .withMessage("Property type must not be null!");
    }

    @Test
    void constructor_shouldThrowExceptionForNullDefaultValue()
    {
        // setup
        final NamespacedKey testKey = NamespacedKey.of("animatedarchitecture:property_ctor_test");

        // execute & verify
        //noinspection DataFlowIssue
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new Property<>(
                testKey,
                String.class,
                null,
                0, 0,
                List.of(),
                iLoc -> ""
            ))
            .withMessage("Default Property value must not be null!");
    }

    @Test
    void constructor_shouldThrowExceptionForNullPropertyScopes()
    {
        // setup
        final NamespacedKey testKey = NamespacedKey.of("animatedarchitecture:property_ctor_test");

        // execute & verify
        //noinspection DataFlowIssue
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> new Property<>(
                testKey,
                String.class,
                "defaultValue",
                0, 0,
                null,
                iLoc -> ""
            ));
    }

    @Test
    void getTitle_shouldReturnCorrectTitleWhenSetFromKey()
    {
        // setup
        final String titleKey = "gui.property.test_title_key.title";
        final Property<?> property = newPropertyBuilder()
            .withTitleKey(titleKey)
            .build();

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        // execute
        final String result = property.getTitle(localizer);

        // verify
        assertThat(result).isEqualTo(titleKey);
    }

    @Test
    void getTitle_shouldReturnCorrectTitleWhenSetAsFunction()
    {
        // setup
        final String titleKey = "gui.property.test_title_key.title";
        final Property<?> property = newPropertyBuilder()
            .withTitleFunction(iLoc -> titleKey)
            .build();

        final ILocalizer localizer = mock();

        // execute
        final String result = property.getTitle(localizer);

        // verify
        assertThat(result).isEqualTo(titleKey);
        verifyNoInteractions(localizer);
    }

    @Test
    void getTitle_shouldReturnNullWhenNoTitleSet()
    {
        // setup
        final Property<?> property = newPropertyBuilder().build();
        final ILocalizer localizer = mock();

        // execute
        final String result = property.getTitle(localizer);

        // verify
        assertThat(result).isNull();
        verifyNoInteractions(localizer);
    }

    private static Property.PropertyBuilder<String> newPropertyBuilder()
    {
        return Property
            .builder(NamespacedKey.of("animatedarchitecture:test-" + UUID.randomUUID()), String.class)
            .withDefaultValue("defaultValue");
    }
}
