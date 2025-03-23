package nl.pim16aap2.testing.assertions;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import nl.pim16aap2.util.exceptions.ContextualOperationException;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A utility class for building assertions about {@link java.util.concurrent.CompletableFuture}s.
 */
public final class CompletableFutureAssertionsUtil
{
    private CompletableFutureAssertionsUtil()
    {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    @CheckReturnValue
    static ExceptionContextAssertionBuilder assertHasExceptionContext(CompletableFuture<?> future)
    {
        return new ExceptionContextAssertionBuilder(future);
    }

    /**
     * A builder for assertions about the exception context of a {@link CompletableFuture}.
     */
    public static final class ExceptionContextAssertionBuilder
    {
        private final CompletableFuture<?> future;

        private @Nullable String message = null;
        private AssertionsUtil.StringMatchType matchType = AssertionsUtil.StringMatchType.EXACT;
        private long timeout = 1000;

        private ExceptionContextAssertionBuilder(CompletableFuture<?> future)
        {
            this.future = future;
        }

        /**
         * Set the expected message of the exception context.
         *
         * @param message
         *     The expected message.
         * @return This builder.
         */
        @CheckReturnValue
        public ExceptionContextAssertionBuilder withMessage(@Nullable String message)
        {
            this.message = message;
            return this;
        }

        /**
         * Set the expected message of the exception context.
         *
         * @param format
         *     The format of the expected message.
         * @param args
         *     The arguments to format the message with.
         * @return This builder.
         */
        @FormatMethod
        @CheckReturnValue
        public ExceptionContextAssertionBuilder withMessage(
            @FormatString String format,
            @Nullable Object @Nullable ... args)
        {
            this.message = String.format(format, args);
            return this;
        }

        /**
         * Set the expected message of the exception context.
         *
         * @param matchType
         *     The way the message should be matched.
         * @param format
         *     The format of the expected message.
         * @param args
         *     The arguments to format the message with.
         * @return This builder.
         */
        @FormatMethod
        @CheckReturnValue
        public ExceptionContextAssertionBuilder withMessage(
            AssertionsUtil.StringMatchType matchType,
            @FormatString String format,
            @Nullable Object @Nullable ... args)
        {
            this.matchType = matchType;
            this.message = String.format(format, args);
            return this;
        }

        /**
         * Set the way the message should be matched.
         * <p>
         * This defaults to {@link AssertionsUtil.StringMatchType#EXACT}.
         *
         * @param matchType
         *     The way the message should be matched.
         * @return This builder.
         */
        @CheckReturnValue
        public ExceptionContextAssertionBuilder withStringMatchType(AssertionsUtil.StringMatchType matchType)
        {
            this.matchType = Objects.requireNonNull(matchType);
            return this;
        }

        /**
         * Set the timeout for the {@link CompletableFuture} to assert on.
         * <p>
         * This defaults to 1000 milliseconds.
         *
         * @param timeoutMs
         *     The timeout in milliseconds.
         * @return This builder.
         */
        @CheckReturnValue
        public ExceptionContextAssertionBuilder withTimeOut(long timeoutMs)
        {
            this.timeout = timeoutMs;
            return this;
        }

        /**
         * Assert that the exception context of the {@link CompletableFuture} matches the set values.
         * <p>
         * If the exception context does not match the set values, the test will fail.
         */
        @CanIgnoreReturnValue
        public ContextualOperationException thenAssert()
        {
            return CompletableFutureAssertionsUtil.thenAssert(this);
        }
    }

    /**
     * Gets the next {@link ContextualOperationException} in the chain of exceptions.
     *
     * @param throwable
     *     The {@link Throwable} to get the next {@link ContextualOperationException} from.
     * @return The next {@link ContextualOperationException} in the chain of exceptions, or {@code null} if none was
     * found.
     */
    static @Nullable ContextualOperationException getNextContextualOperationException(@Nullable Throwable throwable)
    {
        if (throwable == null)
            return null;

        Throwable currentException = throwable;
        while (!(currentException instanceof ContextualOperationException) && currentException.getCause() != null)
        {
            currentException = currentException.getCause();
        }

        return currentException instanceof ContextualOperationException contextualOperationException ?
            contextualOperationException :
            null;
    }

    static ContextualOperationException thenAssert(ExceptionContextAssertionBuilder builder)
    {
        final Throwable baseException = assertThrows(
            Throwable.class,
            () -> builder.future.orTimeout(builder.timeout, TimeUnit.MILLISECONDS).join()
        );

        @Nullable var contextException = getNextContextualOperationException(baseException);
        while (contextException != null)
        {
            if (builder.matchType.matches(contextException.getMessage(), builder.message))
            {
                return contextException;
            }
            contextException = getNextContextualOperationException(contextException.getCause());
        }
        return failContextualOperationExceptionAssertion(baseException);
    }

    @SuppressWarnings("NullAway") // NullAway doesn't like returning null. It is never reached, though.
    private static ContextualOperationException failContextualOperationExceptionAssertion(Throwable baseException)
    {
        fail(
            "Could not find the expected ContextualOperationException! Found exception: \n" +
                AssertionsUtil.throwableToString(baseException)
        );
        return null; // Just for the compiler, this will never be reached.
    }
}
