package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class IPropertyHolderConstTest
{
    private static final Property<String> PROPERTY_STRING = new Property<>(
        Constants.PLUGIN_NAME,
        "string",
        String.class,
        "default"
    );

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    IPropertyHolderConst mock;

    @Test
    void testGetRequiredPropertyValueProvidedNonNull()
    {
        final String value = UUID.randomUUID().toString();
        final var providedPropertyValue = new PropertyContainer.ProvidedPropertyValue<>(String.class, value);
        Mockito.doReturn(providedPropertyValue).when(mock).getPropertyValue(PROPERTY_STRING);

        Assertions.assertEquals(value, mock.getRequiredPropertyValue(PROPERTY_STRING));
    }

    @Test
    void testGetRequiredPropertyValueProvidedNull()
    {
        final var providedPropertyValue = new PropertyContainer.ProvidedPropertyValue<>(String.class, null);
        Mockito.doReturn(providedPropertyValue).when(mock).getPropertyValue(PROPERTY_STRING);

        assertGetRequiredPropertyValueNPE(mock, PROPERTY_STRING);
    }

    // Generally, an undefined property should not be exposed outside the property container.
    // However, if it is, it should be treated as if it were null.
    @Test
    void testGetRequiredPropertyValueUndefined()
    {
        final var propertyValue = new PropertyContainerSerializer.UndefinedPropertyValue(
            PROPERTY_STRING.getFullKey(),
            Mockito.mock()
        );

        Mockito.doReturn(propertyValue).when(mock).getPropertyValue(PROPERTY_STRING);
        assertGetRequiredPropertyValueNPE(mock, PROPERTY_STRING);
    }

    @Test
    void testGetRequiredPropertyValueNull()
    {
        Mockito.doReturn(PropertyContainer.UnsetPropertyValue.INSTANCE).when(mock).getPropertyValue(PROPERTY_STRING);
        assertGetRequiredPropertyValueNPE(mock, PROPERTY_STRING);
    }

    /**
     * Asserts that {@link IPropertyHolderConst#getRequiredPropertyValue(Property)} throws a
     * {@link NullPointerException}.
     *
     * @param mock
     *     The mock whose {@link IPropertyHolderConst#getRequiredPropertyValue(Property)} should be called.
     * @param property
     *     The property to pass to {@link IPropertyHolderConst#getRequiredPropertyValue(Property)}.
     */
    private static void assertGetRequiredPropertyValueNPE(IPropertyHolderConst mock, Property<String> property)
    {
        try
        {
            final var value = mock.getRequiredPropertyValue(property);
            Assertions.fail("Expected NullPointerException, but got value: " + value);
        }
        catch (NullPointerException ex)
        {
            Assertions.assertEquals(property.getFullKey() + " must not be null!", ex.getMessage());
        }
    }
}
