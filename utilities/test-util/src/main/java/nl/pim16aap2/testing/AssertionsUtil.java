package nl.pim16aap2.testing;

import ch.qos.logback.classic.Level;
import nl.pim16aap2.testing.logging.LogInspector;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.function.Executable;
import org.opentest4j.AssertionFailedError;

/**
 * Represents a set of utility methods for testing assertions.
 *
 * @author Pim
 */
public final class AssertionsUtil
{
    private AssertionsUtil()
    {
        // Utility class
    }

    private static Throwable getThrowableLoggedByExecutable(
        @Nullable Class<?> source, Executable executable, Class<?> expectedType, Level level)
    {
        final LogInspector inspector = LogInspector.get();
        final int countBefore = inspector.getThrowingCount(source);

        try
        {
            executable.execute();
        }
        catch (Throwable t)
        {
            throw new RuntimeException(t);
        }

        if (countBefore == inspector.getThrowingCount(source))
            throw new AssertionFailedError(
                String.format("Expected %s to be logged, but nothing was logged!", expectedType));

        final @Nullable Throwable throwable = inspector.getLastThrowable(source, level, true).orElse(null);
        // PointlessNullCheck to suppress NullAway false positive.
        //noinspection PointlessNullCheck
        if (throwable != null && expectedType.isInstance(throwable))
            return throwable;

        throw new AssertionFailedError(String.format("Expected %s to be logged, but instead the logger got: %s",
                                                     expectedType.getName(), throwable));
    }

    /**
     * See {@link #assertThrowableLogged(Class, Executable, Level)} for {@link Level#ERROR}.
     */
    public static <T extends Throwable> T assertThrowableLogged(Class<T> expectedType, Executable executable)
    {
        return assertThrowableLogged(expectedType, executable, Level.ERROR);
    }

    /**
     * Ensures that a throwable was logged to the logger.
     *
     * @param expectedType
     *     The expected type of the throwable that was logged.
     * @param executable
     *     The action that should log a throwable.
     * @param level
     *     The level at which the throwable should have been logged.
     * @param <T>
     *     The type of the throwable.
     * @return The throwable that was logged.
     */
    public static <T extends Throwable> T assertThrowableLogged(
        Class<T> expectedType, Executable executable, Level level)
    {
        final @Nullable Throwable throwable = getThrowableLoggedByExecutable(null, executable, expectedType, level);
        if (expectedType.isInstance(throwable))
            //noinspection unchecked
            return (T) throwable;

        throw new AssertionFailedError(String.format("Expected %s to be logged, but instead the logger got: %s",
                                                     expectedType.getName(), throwable));
    }

    /**
     * See {@link #assertThrowablesLogged(Executable, Level, Class[])} for {@link Level#ERROR}.
     */
    public static void assertThrowablesLogged(Executable executable, Class<?>... expectedTypes)
    {
        assertThrowablesLogged(executable, Level.ERROR, expectedTypes);
    }

    /**
     * Ensures that a throwable with the correct causes was logged.
     *
     * @param executable
     *     The action that should log a throwable.
     * @param level
     *     The level at which the throwable should have been logged.
     * @param expectedTypes
     *     The expected composition of the throwable that was logged.
     *     <p>
     *     For example, when providing "RuntimeException.class, IOException.class" the following exception would be
     *     required to have been logged: new RuntimeException(new IOException());
     */
    public static void assertThrowablesLogged(Executable executable, Level level, Class<?>... expectedTypes)
    {
        if (expectedTypes.length < 1)
            throw new IllegalArgumentException("No expected types provided! Please provide at least 1!");

        @Nullable Throwable throwable = getThrowableLoggedByExecutable(null, executable, expectedTypes[0], level);

        for (int idx = 0; idx < expectedTypes.length; ++idx)
        {
            final Class<?> expectedType = expectedTypes[idx];

            // NullAway does not realize Class#isInstance is also a null check.
            if (throwable == null || !expectedType.isInstance(throwable))
                throw new AssertionFailedError(
                    String.format("Expected %s to be logged at pos %d, but instead the logger got %s!",
                                  expectedType, idx, throwable));
            throwable = throwable.getCause();
        }
    }
}
