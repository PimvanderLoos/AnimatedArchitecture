package nl.pim16aap2.bigdoors.util;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FastFieldCopierTest
{
    private final IPLogger logger = new BasicPLogger();

    @Test
    @SneakyThrows
    void testFastFieldCopier()
    {
        final FastFieldCopier<Foo, Bar> copier = FastFieldCopier.of(logger, Foo.class, "str", Bar.class, "str");

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
        final RuntimeException ex =
            Assertions.assertThrows(RuntimeException.class,
                                    () -> FastFieldCopier.of(logger, Foo.class, "str", Bar.class, "intVal"));
        Assertions.assertEquals(IllegalArgumentException.class, ex.getCause().getClass());
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
