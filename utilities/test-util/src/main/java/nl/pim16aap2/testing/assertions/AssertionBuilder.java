package nl.pim16aap2.testing.assertions;

import com.google.errorprone.annotations.CheckReturnValue;
import nl.altindag.log.LogCaptor;

import java.util.concurrent.CompletableFuture;

/**
 * A utility class for building assertions.
 * <p>
 * Some assertions can be built in other ways than this class, but this class provides a single point of entry for all
 * assertion builders.
 */
public final class AssertionBuilder
{
    private AssertionBuilder()
    {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Creates a new {@link LogAssertionsUtil.LogAssertion.LogAssertionBuilder} for the given {@link LogCaptor}.
     * <p>
     * The builder can be used to assert that certain log messages were logged with the provided properties (e.g. log
     * level, message, throwable, etc.).
     *
     * @param logCaptor
     *     The {@link LogCaptor} to create the assertion for.
     * @return A new {@link LogAssertionsUtil.LogAssertion.LogAssertionBuilder} for the given {@link LogCaptor}.
     */
    public static LogAssertionsUtil.LogAssertion.LogAssertionBuilder assertLogged(LogCaptor logCaptor)
    {
        return LogAssertionsUtil.logAssertionBuilder(logCaptor);
    }

    /**
     * Asserts that the given {@link CompletableFuture} has an exception context set.
     *
     * @param future
     *     The {@link CompletableFuture} to assert on.
     * @return The assertion builder with the given {@link CompletableFuture} set.
     */
    @CheckReturnValue
    public static CompletableFutureAssertionsUtil.ExceptionContextAssertionBuilder assertHasExceptionContext(
        CompletableFuture<?> future)
    {
        return CompletableFutureAssertionsUtil.assertHasExceptionContext(future);
    }
}
