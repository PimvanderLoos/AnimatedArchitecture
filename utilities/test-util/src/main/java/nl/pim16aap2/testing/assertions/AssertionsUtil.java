package nl.pim16aap2.testing.assertions;

import org.assertj.core.api.InstanceOfAssertFactories;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.*;

/**
 * A utility class for general assertions.
 */
public final class AssertionsUtil
{
    private AssertionsUtil()
    {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    static String throwableToString(Throwable throwable)
    {
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    /**
     * Asserts that the given class has a single constructor that is private and throws an UnsupportedOperationException
     * when called.
     * <p>
     * This is useful to ensure that utility classes cannot be instantiated.
     *
     * @param clazz
     *     The class to check.
     * @throws AssertionError
     *     if the class does not have the expected constructor.
     */
    public static void assertSingletonConstructor(Class<?> clazz)
    {
        final var constructors = clazz.getDeclaredConstructors();

        assertThat(constructors)
            .as("Class " + clazz.getName() + " should have a single constructor.")
            .singleElement(InstanceOfAssertFactories.type(Constructor.class))
            .satisfies(constructor ->
            {
                assertThat(constructor.getModifiers())
                    .as("Constructor of " + clazz.getName() + " should be private.")
                    .isEqualTo(java.lang.reflect.Modifier.PRIVATE);
                assertThat(constructor.getParameterCount())
                    .as("Constructor of " + clazz.getName() + " should have no parameters.")
                    .isZero();
            });

        final var constructor = constructors[0];
        constructor.setAccessible(true);

        assertThatException()
            .isThrownBy(constructor::newInstance)
            .withRootCauseExactlyInstanceOf(UnsupportedOperationException.class);
    }
}
