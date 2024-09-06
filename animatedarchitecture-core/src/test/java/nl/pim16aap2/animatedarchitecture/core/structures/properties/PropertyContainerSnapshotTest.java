package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class PropertyContainerSnapshotTest
{
    private static final Property<Object> PROPERTY_UNSET = new Property<>(
        "external",
        "unset",
        Object.class,
        null
    );

    private static final String PROPERTY_STRING_DEFAULT = "default";
    private static final Property<String> PROPERTY_STRING = new Property<>(
        Constants.PLUGIN_NAME,
        "string",
        String.class,
        PROPERTY_STRING_DEFAULT
    );

    private static final Property<Object> PROPERTY_NULLABLE = new Property<>(
        Constants.PLUGIN_NAME,
        "nullable",
        Object.class,
        null
    );

    private static final List<Property<?>> PROPERTIES = List.of(
        PROPERTY_STRING,
        PROPERTY_NULLABLE
    );

    Map<String, IPropertyValue<?>> propertyMap;
    PropertyContainerSnapshot propertyContainerSnapshot;

    @BeforeEach
    void beforeEach()
    {
        propertyMap = Map.of(
            PROPERTY_STRING.getFullKey(), providedPropertyValue(PROPERTY_STRING),
            PROPERTY_NULLABLE.getFullKey(), providedPropertyValue(PROPERTY_NULLABLE)
        );
        propertyContainerSnapshot = new PropertyContainerSnapshot(propertyMap);
    }

    @Test
    void testGetValidPropertyValue()
    {

    }

    /**
     * Creates a new property value for the given property.
     * <p>
     * The value is set to the default value of the property.
     *
     * @param property
     *     The property to create a value for.
     * @param <T>
     *     The type of the property.
     * @return The created property value.
     */
    private static <T> IPropertyValue<T> providedPropertyValue(Property<T> property)
    {
        return new PropertyContainer.ProvidedPropertyValue<>(property.getType(), property.getDefaultValue());
    }
}
