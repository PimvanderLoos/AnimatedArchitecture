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
        ParameterGroup pg = new ParameterGroup.Builder().withRequiredParameters(base).construct();
        Assertions.assertTrue(pg.matches(base));

        pg = new ParameterGroup.Builder().withRequiredParameters(base).withRequiredParameters(Object.class).construct();
        Assertions.assertFalse(pg.matches(base));

        pg = new ParameterGroup.Builder().withRequiredParameters(Object.class, int.class, Object.class).construct();
        Assertions.assertFalse(pg.matches(base));
    }

    @Test
    void testOptional()
    {
        Class<?>[] base = new Class[]{int.class, Object.class, boolean.class};

        ParameterGroup pg = new ParameterGroup.Builder()
            .withOptionalParameters(boolean.class)
            .withRequiredParameters(int.class)     // <--- 0
            .withRequiredParameters(Object.class)  // <--- 1
            .withOptionalParameters(java.util.List.class)
            .withRequiredParameters(boolean.class) // <--- 2
            .withOptionalParameters(java.util.List.class).construct();
        Assertions.assertTrue(pg.matches(base));

        pg = new ParameterGroup.Builder(pg).withRequiredParameters(Object.class).construct();
        Assertions.assertFalse(pg.matches(base));
    }
}
