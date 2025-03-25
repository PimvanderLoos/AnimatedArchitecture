package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyTestUtil.PROPERTY_STRING;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IPropertyHolderConstTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPropertyHolderConst propertyHolder;

    @Test
    void getRequiredPropertyValue_shouldThrowNPEForUnsetProperty()
    {
        // Setup
        doReturn(PropertyContainer.UnsetPropertyValue.INSTANCE)
            .when(propertyHolder).getPropertyValue(PROPERTY_STRING);

        // Execute & Verify
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> propertyHolder.getRequiredPropertyValue(PROPERTY_STRING))
            .withMessage("%s must not be null!", PROPERTY_STRING.getFullKey());
    }

    @Test
    void getRequiredPropertyValue_shouldReturnValueForSetProperty()
    {
        // Setup
        final String value = "testValue";
        final var propertyValue = new PropertyContainer.ProvidedPropertyValue<>(String.class, value, true);
        doReturn(propertyValue).when(propertyHolder).getPropertyValue(PROPERTY_STRING);

        // Execute
        final var result = propertyHolder.getRequiredPropertyValue(PROPERTY_STRING);

        // Verify
        assertThat(result).isEqualTo(value);
    }

    @Test
    void hasProperties_shouldCheckAllProperties()
    {
        // Setup
        final Property<String> property0 = mock();
        final Property<String> property1 = mock();

        // Execute
        propertyHolder.hasProperties(property0, property1);

        // Verify
        verify(propertyHolder).hasProperties(List.of(property0, property1));
    }

    @Test
    void hasProperties_shouldShortCircuitWithSingleProperty()
    {
        // Setup
        final var property = Property.builder("test", "single_property", String.class)
            .withDefaultValue("default")
            .withUserAccessLevels(PropertyAccessLevel.EDIT)
            .build();
        doReturn(true).when(propertyHolder).hasProperty(property);

        // Execute
        final boolean result = propertyHolder.hasProperties(property);

        // Verify
        assertThat(result).isTrue();
        verify(propertyHolder, never()).hasProperties(anyCollection());
    }

    @Test
    void hasProperties_shouldShortCircuitWithoutAnyProperties()
    {
        // Execute
        final boolean result = propertyHolder.hasProperties();

        // Verify
        assertThat(result).isTrue();
        verify(propertyHolder, never()).hasProperties(anyCollection());
        verify(propertyHolder, never()).hasProperty(any());
    }
}
