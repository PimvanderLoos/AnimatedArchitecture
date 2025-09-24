package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyTestUtil.*;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class IPropertyContainerConstTest
{
    @ParameterizedTest
    @MethodSource("propertyContainerProvider")
    void testIterator(IPropertyContainerConst propertyContainer)
    {
        final var iterator = propertyContainer.iterator();

        final Map<String, @Nullable Object> results = HashMap.newHashMap(2);
        iterator.forEachRemaining(entry -> results.put(entry.property().getFullKey(), entry.propertyValue()));

        Assertions.assertEquals(2, results.size());
        Assertions.assertEquals(PROPERTY_STRING_DEFAULT, results.get(PROPERTY_STRING.getFullKey()));
    }

    @ParameterizedTest
    @MethodSource("propertyContainerProvider")
    void testIteratorReadOnly(IPropertyContainerConst propertyContainer)
    {
        final var iterator = propertyContainer.iterator();
        iterator.next();
        Assertions.assertThrows(UnsupportedOperationException.class, iterator::remove);
    }

    @ParameterizedTest
    @MethodSource("propertyContainerProvider")
    void testGetNonnullPropertyValue(IPropertyContainerConst propertyContainer)
    {
        final var value = propertyContainer.getPropertyValue(PROPERTY_STRING);

        Assertions.assertNotNull(value);
        Assertions.assertTrue(value.isSet());
        Assertions.assertEquals(PROPERTY_STRING_DEFAULT, value.value());
    }

    @ParameterizedTest
    @MethodSource("propertyContainerProvider")
    void testGetUnsetProperty(IPropertyContainerConst propertyContainer)
    {
        final var value = propertyContainer.getPropertyValue(PROPERTY_UNSET);

        Assertions.assertNotNull(value);
        Assertions.assertFalse(value.isSet());
        Assertions.assertNull(value.value());
    }

    @ParameterizedTest
    @MethodSource("propertyContainerProvider")
    void testHasProperty(IPropertyContainerConst propertyContainer)
    {
        Assertions.assertTrue(propertyContainer.hasProperty(PROPERTY_STRING));

        Assertions.assertFalse(propertyContainer.hasProperty(PROPERTY_UNSET));
    }

    @ParameterizedTest
    @MethodSource("propertyContainerProvider")
    void testHasProperties(IPropertyContainerConst propertyContainer)
    {
        assertThat(propertyContainer.hasProperties(PROPERTIES.subList(1, 2))).isTrue();
        assertThat(propertyContainer.hasProperties(PROPERTY_UNSET)).isFalse();
        assertThat(propertyContainer.hasProperties(PROPERTIES.getFirst(), PROPERTY_UNSET)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("propertyContainerProvider")
    void testGetRequiredPropertyValue(IPropertyContainerConst propertyContainer)
    {
        final var quarterCircles = propertyContainer.getRequiredPropertyValue(PROPERTY_STRING);
        Assertions.assertEquals(PROPERTY_STRING_DEFAULT, quarterCircles);
    }

    @ParameterizedTest
    @MethodSource("propertyContainerProvider")
    void testGetRequiredPropertyValueThrowsException(IPropertyContainerConst propertyContainer)
    {
        Assertions.assertThrows(
            NullPointerException.class,
            () -> propertyContainer.getRequiredPropertyValue(PROPERTY_UNSET)
        );
    }

    static Stream<IPropertyContainerConst> propertyContainerProvider()
    {
        return PropertyTestUtil.propertyContainerProvider();
    }
}
