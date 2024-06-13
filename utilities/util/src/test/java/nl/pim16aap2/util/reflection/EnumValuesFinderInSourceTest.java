package nl.pim16aap2.util.reflection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EnumValuesFinderInSourceTest
{
    @Test
    void testNamedRetrieval()
    {
        Assertions.assertEquals(
            TestEnum.FIELD_0,
            new EnumValuesFinder(TestEnum.class).withName("FIELD_0").getNullable()
        );
        Assertions.assertEquals(TestEnum.FIELD_1, new EnumValuesFinder(TestEnum.class).withName("FIELD_1").get());
    }

    @Test
    void testIndexedRetrieval()
    {
        Assertions.assertEquals(
            TestEnum.FIELD_2,
            new EnumValuesFinder(TestEnum.class).atIndex(2).getNullable()
        );
        Assertions.assertEquals(TestEnum.FIELD_3, new EnumValuesFinder(TestEnum.class).atIndex(3).get());
    }

    @Test
    void testArrayRetrieval()
    {
        Assertions.assertArrayEquals(TestEnum.values(), new EnumValuesFinder(TestEnum.class).get());
        Assertions.assertArrayEquals(TestEnum.values(), new EnumValuesFinder(TestEnum.class).getNullable());
    }

    @Test
    void testInvalidClass()
    {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EnumValuesFinder(this.getClass()));
    }

    @Test
    void testInvalidName()
    {
        Assertions.assertThrows(
            NullPointerException.class,
            () -> new EnumValuesFinder(TestEnum.class).withName("FIELD_999").get()
        );
        Assertions.assertNull(new EnumValuesFinder(TestEnum.class).withName("FIELD_999").getNullable());
    }

    @Test
    void testInvalidIndex()
    {
        Assertions.assertThrows(
            NullPointerException.class,
            () -> new EnumValuesFinder(TestEnum.class).atIndex(999).get()
        );
        Assertions.assertNull(new EnumValuesFinder(TestEnum.class).atIndex(999).getNullable());
    }

    private enum TestEnum
    {
        FIELD_0,
        FIELD_1,
        FIELD_2,
        FIELD_3
    }
}
