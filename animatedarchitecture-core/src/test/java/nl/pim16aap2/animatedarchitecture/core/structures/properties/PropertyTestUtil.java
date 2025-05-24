package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import nl.pim16aap2.animatedarchitecture.core.util.Constants;

import java.util.List;
import java.util.stream.Stream;

class PropertyTestUtil
{
    static final Property<Integer> PROPERTY_UNSET = Property
        .builder("external", "unset_property", Integer.class)
        .withDefaultValue(5)
        .isEditable()
        .build();

    static final String PROPERTY_STRING_DEFAULT = "default";
    static final Property<String> PROPERTY_STRING = Property
        .builder(Constants.PLUGIN_NAME, "string_property", String.class)
        .withDefaultValue(PROPERTY_STRING_DEFAULT)
        .isEditable()
        .build();

    static final List<Property<?>> PROPERTIES = List.of(
        PROPERTY_STRING
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
}
