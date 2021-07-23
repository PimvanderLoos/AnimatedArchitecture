package nl.pim16aap2.bigDoors.reflection;

import junit.framework.TestCase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ParameterGroupTest extends TestCase
{
    @Test
    void testBasic()
    {
        Class<?>[] base = new Class[]{int.class, Object.class, boolean.class};
        ParameterGroup pg = new ParameterGroup().withRequiredParameters(base);
        Assertions.assertTrue(pg.matches(base));

        pg = new ParameterGroup().withRequiredParameters(base).withRequiredParameters(Object.class);
        Assertions.assertFalse(pg.matches(base));

        pg = new ParameterGroup().withRequiredParameters(Object.class, int.class, Object.class);
        Assertions.assertFalse(pg.matches(base));
    }

    @Test
    void testOptional()
    {
        Class<?>[] base = new Class[]{int.class, Object.class, boolean.class};

        ParameterGroup pg = new ParameterGroup()
            .withOptionalParameters(boolean.class)
            .withRequiredParameters(int.class)     // <--- 0
            .withRequiredParameters(Object.class)  // <--- 1
            .withOptionalParameters(java.util.List.class)
            .withRequiredParameters(boolean.class) // <--- 2
            .withOptionalParameters(java.util.List.class);
        Assertions.assertTrue(pg.matches(base));

        pg.withRequiredParameters(Object.class);
        Assertions.assertFalse(pg.matches(base));
    }
}
