package nl.pim16aap2.testing.assertions;

import nl.altindag.log.model.LogEvent;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.ThrowableAssert;
import org.jspecify.annotations.NullMarked;

/**
 * Represents an assertion class for {@link LogEvent} instances.
 * <p>
 * This class provides methods to assert various conditions on a log event, such as checking for the presence of a
 * throwable, verifying the type of the throwable, and more.
 */
@NullMarked
public class LogEventAssert extends AbstractAssert<LogEventAssert, LogEvent>
{
    LogEventAssert(LogEvent actual)
    {
        super(actual, LogEventAssert.class);
    }

    /**
     * Creates a new {@link LogEventAssert} for the given {@link LogEvent}.
     *
     * @param actual
     *     The {@link LogEvent} to create the assertion for.
     * @return A new {@link LogEventAssert} for the given {@link LogEvent}.
     */
    static LogEventAssert assertThatLogEvent(LogEvent actual)
    {
        return new LogEventAssert(actual);
    }

    /**
     * Verifies that the log event has a throwable.
     *
     * @return A new {@link ThrowableAssert} containing the log event.
     */
    public ThrowableAssert<?> hasThrowable()
    {
        final Throwable throwable = actual.getThrowable().orElse(null);
        if (throwable == null)
            failWithMessage("Expected log event to have a throwable but found none.");
        return new ThrowableAssert<>(throwable);
    }

    /**
     * Verifies that the log event has a throwable that is an instance of the given type.
     *
     * @param throwableType
     *     The type of the throwable to check for.
     * @param <T>
     *     The type of the throwable to check for.
     * @return A {@link ThrowableAssert} for the throwable of the log event.
     */
    public <T extends Throwable> ThrowableAssert<T> hasThrowableOfType(Class<T> throwableType)
    {
        return getThrowableAssertForType(throwableType).isInstanceOf(throwableType);
    }

    /**
     * Verifies that the log event has a throwable that is exactly of the given type.
     *
     * @param throwableType
     *     The type of the throwable to check for.
     * @param <T>
     *     The type of the throwable to check for.
     * @return A {@link ThrowableAssert} for the throwable of the log event.
     */
    public <T extends Throwable> ThrowableAssert<T> hasThrowableExactlyOfType(Class<T> throwableType)
    {
        return getThrowableAssertForType(throwableType).isExactlyInstanceOf(throwableType);
    }

    private <T extends Throwable> ThrowableAssert<T> getThrowableAssertForType(Class<T> throwableType)
    {
        final Throwable throwable = actual.getThrowable().orElse(null);

        if (throwable == null)
            failWithMessage("Expected log event to have a throwable of type %s but found none.",
                throwableType.getName());

        //noinspection unchecked
        return (ThrowableAssert<T>) new ThrowableAssert<>(throwable);
    }
}
