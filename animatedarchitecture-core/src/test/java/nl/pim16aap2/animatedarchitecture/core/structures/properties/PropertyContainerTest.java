package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Objects;
import java.util.UUID;

import static nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyTestUtil.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PropertyContainerTest
{
    private PropertyContainer propertyContainer;

    @BeforeEach
    void beforeEach()
    {
        propertyContainer = PropertyContainer.forProperties(PROPERTIES);
    }


    @Test
    void testSetProperty()
    {
        final String newValue = "new_" + PROPERTY_STRING_DEFAULT;

        // Verify that the property has the default value (false).
        Assertions.assertEquals(
            PROPERTY_STRING_DEFAULT,
            propertyContainer.getPropertyValue(PROPERTY_STRING).value()
        );

        // Verify that the property provides the old value (false) when setting a new value (true).
        Assertions.assertEquals(
            PROPERTY_STRING_DEFAULT,
            propertyContainer.setPropertyValue(PROPERTY_STRING, newValue).value()
        );

        // Verify that the property has been set to the new value (true).
        Assertions.assertEquals(
            newValue,
            propertyContainer.getPropertyValue(PROPERTY_STRING).value()
        );
    }

    @Test
    void testSetMissingProperty()
    {
        Assertions.assertFalse(propertyContainer.getPropertyValue(PROPERTY_UNSET).isSet());

        final int newValue = Objects.requireNonNullElse(PROPERTY_UNSET.getDefaultValue(), 7) + 11;

        propertyContainer.setPropertyValue(PROPERTY_UNSET, newValue);
        Assertions.assertEquals(newValue, propertyContainer.getPropertyValue(PROPERTY_UNSET).value());
        Assertions.assertTrue(propertyContainer.hasProperty(PROPERTY_UNSET));
    }

    @Test
    void testSetValidUntypedPropertyValue()
    {
        Assertions.assertDoesNotThrow(
            () -> propertyContainer.setUntypedPropertyValue(PROPERTY_STRING, "value")
        );
    }

    @Test
    void testSetInvalidUntypedPropertyValue()
    {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> propertyContainer.setUntypedPropertyValue(PROPERTY_STRING, new Object())
        );
    }

    @Test
    void testSetMissingUntypedPropertyValue()
    {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> propertyContainer.setUntypedPropertyValue(PROPERTY_UNSET, new Object())
        );
    }

    @Test
    void testValidMapUntypedValue()
    {
        Assertions.assertDoesNotThrow(
            () -> PropertyContainer.mapUntypedValue(PROPERTY_STRING, "value")
        );
    }

    @Test
    void testInvalidMapUntypedValue()
    {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> PropertyContainer.mapUntypedValue(PROPERTY_STRING, new Object())
        );
    }

    @Test
    void testSnapshot()
    {
        final String stringValue = UUID.randomUUID().toString();
        final Object objectValue = new Object();

        propertyContainer.setPropertyValue(PROPERTY_STRING, stringValue);
        propertyContainer.setPropertyValue(PROPERTY_NULLABLE, objectValue);

        final var snapshot = propertyContainer.snapshot();

        Assertions.assertEquals(stringValue, snapshot.getPropertyValue(PROPERTY_STRING).value());
        Assertions.assertEquals(objectValue, snapshot.getPropertyValue(PROPERTY_NULLABLE).value());

        propertyContainer.setPropertyValue(PROPERTY_STRING, UUID.randomUUID().toString());
        Assertions.assertEquals(stringValue, snapshot.getPropertyValue(PROPERTY_STRING).value());
    }

    @Test
    void testForType()
    {
        final StructureType mockStructureType = Mockito.mock();
        Mockito.when(mockStructureType.getProperties()).thenReturn(PROPERTIES);

        final var container = PropertyContainer.forType(mockStructureType);
        Assertions.assertEquals(propertyContainer, container);
    }

    @Test
    void testEqualsAndHashCode()
    {
        final var otherContainer = PropertyContainer.forProperties(PROPERTIES);

        Assertions.assertEquals(propertyContainer, otherContainer);
        Assertions.assertEquals(propertyContainer.hashCode(), otherContainer.hashCode());

        otherContainer.setPropertyValue(PROPERTY_STRING, UUID.randomUUID().toString());

        Assertions.assertNotEquals(propertyContainer, otherContainer);
        Assertions.assertNotEquals(propertyContainer.hashCode(), otherContainer.hashCode());
    }
}
