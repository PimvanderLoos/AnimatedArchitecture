package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PropertyContainerTest
{
    private static final Property<Integer> PROPERTY_UNSET = new Property<>(
        "external",
        "unset_property",
        Integer.class,
        5
    );

    private static final String PROPERTY_STRING_DEFAULT = "default";
    private static final Property<String> PROPERTY_STRING = new Property<>(
        Constants.PLUGIN_NAME,
        "string_property",
        String.class,
        PROPERTY_STRING_DEFAULT
    );

    private static final Property<Object> PROPERTY_NULLABLE = new Property<>(
        Constants.PLUGIN_NAME,
        "nullable_property",
        Object.class,
        null
    );

    private static final List<Property<?>> PROPERTIES = List.of(
        PROPERTY_STRING,
        PROPERTY_NULLABLE
    );

    private PropertyContainer propertyContainer;

    @BeforeEach
    void beforeEach()
    {
        propertyContainer = PropertyContainer.forProperties(PROPERTIES);
    }

    @Test
    void testGetNullablePropertyValue()
    {
        final var value = propertyContainer.getPropertyValue(PROPERTY_NULLABLE);

        Assertions.assertNotNull(value);
        Assertions.assertTrue(value.isSet());
        Assertions.assertNull(value.value());
    }

    @Test
    void testGetNonnullPropertyValue()
    {
        final var value = propertyContainer.getPropertyValue(PROPERTY_STRING);

        Assertions.assertNotNull(value);
        Assertions.assertTrue(value.isSet());
        Assertions.assertEquals(PROPERTY_STRING_DEFAULT, value.value());
    }

    @Test
    void testGetUnsetProperty()
    {
        final var value = propertyContainer.getPropertyValue(PROPERTY_UNSET);

        Assertions.assertNotNull(value);
        Assertions.assertFalse(value.isSet());
        Assertions.assertNull(value.value());
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
    void testHasProperty()
    {
        Assertions.assertTrue(propertyContainer.hasProperty(PROPERTY_STRING));

        Assertions.assertFalse(propertyContainer.hasProperty(PROPERTY_UNSET));
    }

    @Test
    void testHasProperties()
    {
        Assertions.assertTrue(propertyContainer.hasProperties(
            PROPERTIES.get(0),
            PROPERTIES.get(1)
        ));

        Assertions.assertFalse(propertyContainer.hasProperties(
            PROPERTIES.get(0),
            PROPERTY_UNSET
        ));
    }

    @Test
    void testGetRequiredPropertyValue()
    {
        final var quarterCircles = propertyContainer.getRequiredPropertyValue(PROPERTY_STRING);
        Assertions.assertEquals(PROPERTY_STRING_DEFAULT, quarterCircles);
    }

    @Test
    void testGetRequiredPropertyValueThrowsException()
    {
        Assertions.assertThrows(
            NullPointerException.class,
            () -> propertyContainer.getRequiredPropertyValue(PROPERTY_UNSET)
        );
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
    void testIterator()
    {
        final var iterator = propertyContainer.iterator();

        Assertions.assertTrue(iterator.hasNext());
        var entry = iterator.next();
        Assertions.assertEquals(PROPERTY_STRING.getFullKey(), entry.getKey());
        Assertions.assertEquals(PROPERTY_STRING_DEFAULT, entry.getValue().value());

        Assertions.assertTrue(iterator.hasNext());
        entry = iterator.next();
        Assertions.assertEquals(PROPERTY_NULLABLE.getFullKey(), entry.getKey());
        Assertions.assertNull(entry.getValue().value());

        Assertions.assertFalse(iterator.hasNext());
    }

    @Test
    void testIteratorReadOnly()
    {
        final var iterator = propertyContainer.iterator();
        iterator.next();
        Assertions.assertThrows(UnsupportedOperationException.class, iterator::remove);
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
