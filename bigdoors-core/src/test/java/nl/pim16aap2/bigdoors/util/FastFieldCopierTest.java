package nl.pim16aap2.bigdoors.util;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FastFieldCopierTest
{
    @Test
    @SneakyThrows
    void testFastFieldCopier()
    {
        final FastFieldCopier<Foo, Bar> copier = FastFieldCopier.of(Foo.class, "str", Bar.class, "str");

        final String a = "a-a-a-a-a";
        final String b = "b-b-b-b-b";
        final Foo foo = new Foo(a);
        final Bar bar = new Bar(b, -1);

        Assertions.assertEquals(b, bar.str);
        copier.copy(foo, bar);
        Assertions.assertEquals(a, bar.str);
    }

    @Test
    @SneakyThrows
    void testInvalidTypes()
    {
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> FastFieldCopier.of(Foo.class, "str", Bar.class, "intVal"));
    }

    @AllArgsConstructor
    private static final class Foo
    {
        @SuppressWarnings("unused")
        private String str;
    }

    @AllArgsConstructor
    private static final class Bar
    {
        private String str;
        @SuppressWarnings("unused")
        private int intVal;
    }
}
