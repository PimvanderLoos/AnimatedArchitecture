package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import nl.pim16aap2.animatedarchitecture.core.util.Constants;

import java.util.List;
import java.util.stream.Stream;

class PropertyTestUtil
{
    static final Property<Integer> PROPERTY_UNSET = new Property<>(
        "external",
        "unset_property",
        Integer.class,
        5,
        PropertyAccessLevel.USER_EDITABLE
    );

    static final String PROPERTY_STRING_DEFAULT = "default";
    static final Property<String> PROPERTY_STRING = new Property<>(
        Constants.PLUGIN_NAME,
        "string_property",
        String.class,
        PROPERTY_STRING_DEFAULT,
        PropertyAccessLevel.USER_EDITABLE
    );

    static final Property<Object> PROPERTY_NULLABLE = new Property<>(
        Constants.PLUGIN_NAME,
        "nullable_property",
        Object.class,
        null,
        PropertyAccessLevel.USER_EDITABLE
    );

    static final List<Property<?>> PROPERTIES = List.of(
        PROPERTY_STRING,
        PROPERTY_NULLABLE
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
            PropertyContainer.forProperties(PROPERTIES),
            new PropertyContainerSnapshot(PropertyContainer.toPropertyMap(PROPERTIES))
        );
    }
}
