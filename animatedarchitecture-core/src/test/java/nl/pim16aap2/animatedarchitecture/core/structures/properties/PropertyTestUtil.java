package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import nl.pim16aap2.animatedarchitecture.core.util.Constants;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class PropertyTestUtil
{
    static final Property<Integer> PROPERTY_UNSET = Property
        .builder("external", "unset_property", Integer.class)
        .withDefaultValue(5)
        .withUserAccessLevels(PropertyAccessLevel.READ, PropertyAccessLevel.EDIT)
        .withAdminAccessLevels(PropertyAccessLevel.ADD, PropertyAccessLevel.REMOVE)
        .build();

    static final String PROPERTY_STRING_DEFAULT = "default";
    static final Property<String> PROPERTY_STRING = Property
        .builder(Constants.PLUGIN_NAME, "string_property", String.class)
        .withDefaultValue(PROPERTY_STRING_DEFAULT)
        .withUserAccessLevels(PropertyAccessLevel.READ, PropertyAccessLevel.EDIT)
        .withAdminAccessLevels(PropertyAccessLevel.ADD, PropertyAccessLevel.REMOVE)
        .build();

    static final Property<Boolean> PROPERTY_BOOLEAN = Property
        .builder(Constants.PLUGIN_NAME, "boolean_property", Boolean.class)
        .withDefaultValue(true)
        .withUserAccessLevels(PropertyAccessLevel.READ, PropertyAccessLevel.EDIT)
        .withAdminAccessLevels(PropertyAccessLevel.ADD, PropertyAccessLevel.REMOVE)
        .build();

    static final List<Property<?>> PROPERTIES = List.of(
        PROPERTY_STRING,
        PROPERTY_BOOLEAN
    );

    /**
     * Provides a stream of property containers.
     * <p>
     * This can be used in parameterized tests to test multiple implementations of {@link IPropertyContainerConst}.
     *
     * @return A stream of property containers.
     */
    static Stream<IPropertyContainerConst> propertyContainerProvider()
    {
        return Stream.of(
            PropertyContainer.forProperties(PROPERTIES, false),
            new PropertyContainerSnapshot(PropertyContainer.toPropertyMap(PROPERTIES, false))
        );
    }

    private static PropertyContainer of(
        List<Property<?>> properties,
        List<Object> values,
        List<Boolean> required)
    {
        if (properties.size() != values.size() || properties.size() != required.size())
            throw new IllegalArgumentException("Properties, values, and required lists must have the same size.");

        final Map<String, IPropertyValue<?>> propertyMap = LinkedHashMap.newLinkedHashMap(properties.size());
        for (int i = 0; i < properties.size(); i++)
        {
            final Property<?> property = properties.get(i);
            final Object value = values.get(i);
            final boolean isRequired = required.get(i);

            propertyMap.put(
                PropertyContainer.mapKey(property),
                PropertyContainer.mapUntypedValue(property, value, isRequired)
            );
        }
        return new PropertyContainer(propertyMap);
    }

    public static <T> PropertyContainer of(
        Property<T> property0, T value0, boolean required0)
    {
        return of(
            List.of(property0),
            List.of(value0),
            List.of(required0)
        );
    }

    public static <T, U> PropertyContainer of(
        Property<T> property0, T value0, boolean required0,
        Property<U> property1, U value1, boolean required1)
    {
        return of(
            List.of(property0, property1),
            List.of(value0, value1),
            List.of(required0, required1)
        );
    }

    public static <T, U, V> PropertyContainer of(
        Property<T> property0, T value0, boolean required0,
        Property<U> property1, U value1, boolean required1,
        Property<V> property2, V value2, boolean required2)
    {
        return of(
            List.of(property0, property1, property2),
            List.of(value0, value1, value2),
            List.of(required0, required1, required2)
        );
    }


}
