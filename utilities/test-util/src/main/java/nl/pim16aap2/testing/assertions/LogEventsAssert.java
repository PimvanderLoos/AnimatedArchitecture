package nl.pim16aap2.testing.assertions;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractListAssert;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.IntFunction;
import java.util.function.Predicate;

import static nl.pim16aap2.testing.assertions.LogAssertionsUtil.formatLogEvents;
import static org.assertj.core.util.Lists.newArrayList;

/**
 * An assertion class for a list of {@link LogEvent}s.
 * <p>
 * This class provides methods to assert various conditions on a list of {@link LogEvent}s. It extends
 * {@link AbstractAssert} from AssertJ to provide fluent assertions.
 */
@NullMarked
public class LogEventsAssert extends AbstractListAssert<LogEventsAssert, List<LogEvent>, LogEvent, LogEventAssert>
{
    private final LogCaptor logCaptor;

    LogEventsAssert(List<LogEvent> logEvents, LogCaptor logCaptor)
    {
        super(logEvents, LogEventsAssert.class);
        this.logCaptor = Objects.requireNonNull(logCaptor);

        isNotNull();
    }

    /**
     * Creates a new {@link LogEventsAssert} for the given list of {@link LogEvent}s.
     *
     * @param logEvents
     *     The list of {@link LogEvent}s to create the assertion for.
     * @param logCaptor
     *     The {@link LogCaptor} that captured the log events.
     * @return A new {@link LogEventsAssert} for the given list of {@link LogEvent}s.
     */
    static LogEventsAssert assertThatLogEvents(List<LogEvent> logEvents, LogCaptor logCaptor)
    {
        return new LogEventsAssert(logEvents, logCaptor);
    }

    private LogEventsAssert newInstance(List<LogEvent> logEvents)
    {
        return new LogEventsAssert(logEvents, logCaptor);
    }

    @Override
    protected void failWithMessage(String errorMessage, Object... arguments)
    {
        final String newMessage = errorMessage + "\n" + formatLogEvents(logCaptor.getLogEvents());
        super.failWithMessage(newMessage, arguments);
    }

    @Override
    protected LogEventAssert toAssert(LogEvent value, String description)
    {
        return new LogEventAssert(value).as(description);
    }

    @Override
    protected LogEventsAssert newAbstractIterableAssert(Iterable<? extends LogEvent> iterable)
    {
        return new LogEventsAssert(newArrayList(iterable), logCaptor);
    }

    @Override
    public LogEventsAssert isNotEmpty()
    {
        if (info.overridingErrorMessage() == null)
        {
            info.overridingErrorMessage("Expected at least one log event but found none");
        }
        iterables.assertNotEmpty(info, actual);
        return myself;
    }

    /**
     * Filters the log events that have a message matching the given matcher.
     * <p>
     * Note: If no log events match the expectation, the returned {@link LogEventsAssert} will be empty but not fail.
     * <p>
     * Example usage:
     * <pre>{@code
     * .withMessage(String::contains, "Expected message")
     * }</pre>
     *
     * @param matcher
     *     The matcher to apply to the log event messages.
     * @param expected
     *     The expected message to match against.
     * @param args
     *     The optional arguments to format the expected message.
     * @return A new {@link LogEventsAssert} containing the log events that match the expectation. This may be empty.
     */
    @FormatMethod
    public LogEventsAssert filteredByMessage(
        BiPredicate<String, String> matcher,
        @FormatString String expected,
        Object... args)
    {
        final String formattedMessage = String.format(expected, args);
        final List<LogEvent> matchingEvents = logEventsFilteredBy(
            logEvent -> matcher.test(logEvent.getMessage(), formattedMessage)
        );
        return newInstance(matchingEvents);
    }

    /**
     * Verifies that at least one log event has a message that matches the given matcher.
     * <p>
     * This method is similar to {@link #filteredByMessage(BiPredicate, String, Object...)} but asserts that the result
     * is not empty.
     * <p>
     * Example usage:
     * <pre>{@code
     * .hasAnyWithMessage(String::startsWith, "Expected message")
     * }</pre>
     *
     * @param matcher
     *     The matcher to apply to the log event messages.
     * @param expected
     *     The expected message to match against.
     * @param args
     *     The optional arguments to format the expected message.
     * @return A new {@link LogEventsAssert} containing the log events that match the expectation. This is never empty.
     */
    @FormatMethod
    public LogEventsAssert hasAnyWithMessage(
        BiPredicate<String, String> matcher,
        @FormatString String expected,
        Object... args)
    {
        return filteredByMessage(matcher, expected, args)
            .isNotEmpty(String.format(
                "Expected at least one log event with a message matching '%s' but found none",
                String.format(expected, args)
            ));
    }

    /**
     * Verifies that exactly one log event has a message that matches the given matcher.
     * <p>
     * This method is similar to {@link #filteredByMessage(BiPredicate, String, Object...)} but asserts that the result
     * contains exactly one element.
     * <p>
     * Example usage:
     * <pre>{@code
     * .singleWithMessage(String::contains, "Expected message")
     * }</pre>
     *
     * @param matcher
     *     The matcher to apply to the log event messages.
     * @param expected
     *     The expected message to match against.
     * @param args
     *     The optional arguments to format the expected message.
     * @return A new {@link LogEventAssert} containing the log event that matches the expectation.
     */
    @FormatMethod
    public LogEventAssert singleWithMessage(
        BiPredicate<String, String> matcher,
        @FormatString String expected,
        Object... args)
    {
        return filteredByMessage(matcher, expected, args)
            .singleElement(count -> String.format(
                "Expected exactly one log event with a message matching '%s' but found %d",
                String.format(expected, args),
                count
            ));
    }


    /**
     * Filters the log events that have a message containing the expected message.
     * <p>
     * Note: If no log events match the expectation, the returned {@link LogEventsAssert} will be empty but not fail.
     *
     * @param expectedMessage
     *     The expected message to be contained in the log event messages.
     * @param args
     *     Optional arguments to format the expected message.
     * @return A new {@link LogEventsAssert} containing the log events that match the expectation. This may be empty.
     */
    @FormatMethod
    public LogEventsAssert filteredByMessagesContaining(@FormatString String expectedMessage, Object... args)
    {
        return filteredByMessage(String::contains, expectedMessage, args);
    }

    /**
     * Verifies that at least one log event has a message that contains the expected message.
     * <p>
     * This method is similar to {@link #filteredByMessagesContaining(String, Object...)} but asserts that the result is
     * not empty.
     *
     * @param expectedMessage
     *     The expected message to be contained in the log event messages.
     * @param args
     *     Optional arguments to format the expected message.
     * @return A new {@link LogEventsAssert} containing the log events that match the expectation. This is never empty.
     */
    @FormatMethod
    public LogEventsAssert hasAnyWithMessageContaining(@FormatString String expectedMessage, Object... args)
    {
        return filteredByMessagesContaining(expectedMessage, args)
            .isNotEmpty(String.format(
                "Expected at least one log event with a message containing '%s' but found none",
                String.format(expectedMessage, args)
            ));
    }

    /**
     * Verifies that exactly one log event has a message that contains the expected message.
     *
     * @param expectedMessage
     *     The expected message to be contained in the log event messages.
     * @param args
     *     Optional arguments to format the expected message.
     * @return A new {@link LogEventAssert} containing the log event that matches the expectation.
     */
    @FormatMethod
    public LogEventAssert singleWithMessageContaining(@FormatString String expectedMessage, Object... args)
    {
        return filteredByMessagesContaining(expectedMessage, args)
            .singleElement(count -> String.format(
                "Expected exactly one log event with a message containing '%s' but found %d",
                String.format(expectedMessage, args),
                count
            ));
    }


    /**
     * Filters the log events that have a message that exactly matches the expected message.
     * <p>
     * Note: If no log events match the expectation, the returned {@link LogEventsAssert} will be empty but not fail.
     *
     * @param expectedMessage
     *     The expected message to match exactly.
     * @param args
     *     Optional arguments to format the expected message.
     * @return A new {@link LogEventsAssert} containing the log events that match the expectation. This may be empty.
     */
    @FormatMethod
    public LogEventsAssert filteredByMessagesExactly(@FormatString String expectedMessage, Object... args)
    {
        return filteredByMessage(String::equals, expectedMessage, args);
    }

    /**
     * Verifies that at least one log event has a message that exactly matches the expected message.
     * <p>
     * This method is similar to {@link #filteredByMessagesExactly(String, Object...)} but asserts that the result is
     * not empty.
     *
     * @param expectedMessage
     *     The expected message to match exactly.
     * @param args
     *     Optional arguments to format the expected message.
     * @return A new {@link LogEventsAssert} containing the log events that match the expectation. This is never empty.
     */
    @FormatMethod
    public LogEventsAssert hasAnyWithMessageExactly(@FormatString String expectedMessage, Object... args)
    {
        return filteredByMessagesExactly(expectedMessage, args)
            .isNotEmpty(String.format(
                "Expected at least one log event with exact message '%s' but found none",
                String.format(expectedMessage, args)
            ));
    }

    /**
     * Verifies that exactly one log event has a message that exactly matches the expected message.
     *
     * @param expectedMessage
     *     The expected message to match exactly.
     * @param args
     *     Optional arguments to format the expected message.
     * @return A new {@link LogEventAssert} containing the log event that matches the expectation.
     */
    @FormatMethod
    public LogEventAssert singleWithMessageExactly(@FormatString String expectedMessage, Object... args)
    {
        return filteredByMessagesExactly(expectedMessage, args)
            .singleElement(count -> String.format(
                "Expected exactly one log event with exact message '%s' but found %d",
                String.format(expectedMessage, args),
                count
            ));
    }


    /**
     * Filters the log events that have a message that exactly matches the expected message.
     * <p>
     * Note: If no log events match the expectation, the returned {@link LogEventsAssert} will be empty but not fail.
     *
     * @param expectedMessage
     *     The expected message to be at the start of the log event messages.
     * @param args
     *     Optional arguments to format the expected message.
     * @return A new {@link LogEventsAssert} containing the log events that match the expectation. This may be empty.
     */
    @FormatMethod
    public LogEventsAssert filteredByMessagesStartingWith(@FormatString String expectedMessage, Object... args)
    {
        return filteredByMessage(String::startsWith, expectedMessage, args);
    }

    /**
     * Verifies that at least one log event has a message that starts with the expected message.
     * <p>
     * This method is similar to {@link #filteredByMessagesStartingWith(String, Object...)} but asserts that the result
     * is not empty.
     *
     * @param expectedMessage
     *     The expected message to be at the start of the log event messages.
     * @param args
     *     Optional arguments to format the expected message.
     * @return A new {@link LogEventsAssert} containing the log events that match the expectation. This is never empty.
     */
    @FormatMethod
    public LogEventsAssert hasAnyWithMessageStartingWith(@FormatString String expectedMessage, Object... args)
    {
        return filteredByMessagesStartingWith(expectedMessage, args)
            .isNotEmpty(String.format(
                "Expected at least one log event with a message starting with '%s' but found none",
                String.format(expectedMessage, args)
            ));
    }

    /**
     * Verifies that exactly one log event has a message that starts with the expected message.
     *
     * @param expectedMessage
     *     The expected message to be at the start of the log event messages.
     * @param args
     *     Optional arguments to format the expected message.
     * @return A new {@link LogEventAssert} containing the log event that matches the expectation.
     */
    @FormatMethod
    public LogEventAssert singleWithMessageStartingWith(@FormatString String expectedMessage, Object... args)
    {
        return filteredByMessagesStartingWith(expectedMessage, args)
            .singleElement(count -> String.format(
                "Expected exactly one log event with a message starting with '%s' but found %d",
                String.format(expectedMessage, args),
                count
            ));
    }


    /**
     * Filters the log events that have a message that ends with the expected message.
     * <p>
     * Note: If no log events match the expectation, the returned {@link LogEventsAssert} will be empty but not fail.
     *
     * @param expectedMessage
     *     The expected message to be at the end of the log event messages.
     * @param args
     *     Optional arguments to format the expected message.
     * @return A new {@link LogEventsAssert} containing the log events that match the expectation. This may be empty.
     */
    @FormatMethod
    public LogEventsAssert filteredByMessagesEndingWith(@FormatString String expectedMessage, Object... args)
    {
        return filteredByMessage(String::endsWith, expectedMessage, args);
    }

    /**
     * Verifies that at least one log event has a message that ends with the expected message.
     * <p>
     * This method is similar to {@link #filteredByMessagesEndingWith(String, Object...)} but asserts that the result is
     * not empty.
     *
     * @param expectedMessage
     *     The expected message to be at the end of the log event messages.
     * @param args
     *     Optional arguments to format the expected message.
     * @return A new {@link LogEventsAssert} containing the log events that match the expectation. This is never empty.
     */
    @FormatMethod
    public LogEventsAssert hasAnyWithMessageEndingWith(@FormatString String expectedMessage, Object... args)
    {
        return filteredByMessagesEndingWith(expectedMessage, args)
            .isNotEmpty(String.format(
                "Expected at least one log event with a message ending with '%s' but found none",
                String.format(expectedMessage, args)
            ));
    }

    /**
     * Verifies that exactly one log event has a message that ends with the expected message.
     *
     * @param expectedMessage
     *     The expected message to be at the end of the log event messages.
     * @param args
     *     Optional arguments to format the expected message.
     * @return A new {@link LogEventAssert} containing the log event that matches the expectation.
     */
    @FormatMethod
    public LogEventAssert singleWithMessageEndingWith(@FormatString String expectedMessage, Object... args)
    {
        return filteredByMessagesEndingWith(expectedMessage, args)
            .singleElement(count -> String.format(
                "Expected exactly one log event with a message ending with '%s' but found %d",
                String.format(expectedMessage, args),
                count
            ));
    }


    /**
     * Filters the log events that have a message matching the given regular expression.
     * <p>
     * Note: If no log events match the expectation, the returned {@link LogEventsAssert} will be empty but not fail.
     *
     * @param regex
     *     The regular expression to match the log event messages against.
     * @return A new {@link LogEventsAssert} containing the log events that match the expectation. This may be empty.
     */
    public LogEventsAssert filteredByMessagesMatching(String regex)
    {
        return newInstance(logEventsFilteredBy(logEvent -> logEvent.getMessage().matches(regex)));
    }

    /**
     * Verifies that at least one log event has a message that matches the given regular expression.
     * <p>
     * This method is similar to {@link #filteredByMessagesMatching(String)} but asserts that the result is not empty.
     *
     * @param regex
     *     The regular expression to match the log event messages against.
     * @return A new {@link LogEventsAssert} containing the log events that match the expectation. This is never empty.
     */
    public LogEventsAssert hasAnyWithMessageMatching(String regex)
    {
        return filteredByMessagesMatching(regex)
            .isNotEmpty(String.format(
                "Expected at least one log event with a message matching '%s' but found none",
                regex
            ));
    }

    /**
     * Verifies that exactly one log event has a message that matches the given regular expression.
     *
     * @param regex
     *     The regular expression to match the log event messages against.
     * @return A new {@link LogEventAssert} containing the log event that matches the expectation.
     */
    public LogEventAssert singleWithMessageMatching(String regex)
    {
        return filteredByMessagesMatching(regex)
            .singleElement(count -> String.format(
                "Expected exactly one log event with a message matching '%s' but found %d",
                regex,
                count
            ));
    }


    /**
     * Filters the log events by the given predicate.
     * <p>
     * If no log events match the predicate, the returned {@link LogEventsAssert} will be empty (but not fail!).
     *
     * @param predicate
     *     The predicate to filter the log events by.
     * @return A new {@link LogEventsAssert} containing the filtered log events. This may be empty.
     */
    public LogEventsAssert filteredBy(Predicate<LogEvent> predicate)
    {
        return new LogEventsAssert(logEventsFilteredBy(predicate), logCaptor);
    }

    /**
     * Verifies that at least one log event matches the given predicate.
     * <p>
     * This method is similar to {@link #filteredBy(Predicate)} but asserts that the result is not empty.
     *
     * @param predicate
     *     The predicate to filter the log events by.
     * @return A new {@link LogEventsAssert} containing the filtered log events. This is never empty.
     */
    public LogEventsAssert hasAnyFilteredBy(Predicate<LogEvent> predicate)
    {
        return filteredBy(predicate)
            .isNotEmpty("Expected at least one log event matching the predicate but found none");
    }

    private int logEventCount()
    {
        return actual.size();
    }

    /**
     * Verifies that exactly one log event matches the given predicate.
     *
     * @param predicate
     *     The predicate to filter the log events by.
     * @return A new {@link LogEventAssert} containing the log event that matches the expectation.
     */
    public LogEventAssert singleFilteredBy(Predicate<LogEvent> predicate)
    {
        return filteredBy(predicate)
            .singleElement("Expected exactly one log event matching the predicate but found %d");
    }


    /**
     * Filters the log events by the given throwable type.
     * <p>
     * Note: If no log events match the expectation, the returned {@link LogEventsAssert} will be empty but not fail.
     *
     * @param throwableType
     *     The type of the throwable to filter by.
     * @return A new {@link LogEventsAssert} containing the filtered log events. This may be empty.
     */
    public LogEventsAssert filteredByThrowableOfType(Class<? extends Throwable> throwableType)
    {
        return filteredBy(
            event -> event.getThrowable()
                .map(throwableType::isInstance)
                .orElse(false)
        );
    }

    /**
     * Verifies that at least one log event has a throwable of the given type associated with it.
     * <p>
     * This method is similar to {@link #filteredByThrowableOfType(Class)} but asserts that the result is not empty.
     *
     * @param throwableType
     *     The type of the throwable to check for.
     * @return A new {@link LogEventsAssert} containing the filtered log events. This is never empty.
     */
    public LogEventsAssert hasAnyWithThrowableOfType(Class<? extends Throwable> throwableType)
    {
        return filteredByThrowableOfType(throwableType)
            .isNotEmpty(String.format(
                "Expected at least one log event with a throwable of type %s but found none",
                throwableType.getName()
            ));
    }

    /**
     * Filters the log events that have exactly one throwable of the given type associated with them.
     *
     * @param throwableType
     *     The type of the throwable to filter by.
     * @return A new {@link LogEventAssert} containing the log event that matches the expectation.
     */
    public LogEventAssert singleWithThrowableOfType(Class<? extends Throwable> throwableType)
    {
        return filteredByThrowableOfType(throwableType)
            .singleElement(count -> String.format(
                "Expected exactly one log event with a throwable of type %s but found %d",
                throwableType.getName(),
                count
            ));
    }


    /**
     * Filters the log events that have a throwable that is exactly of the given type associated with them.
     * <p>
     * Note: If no log events match the expectation, the returned {@link LogEventsAssert} will be empty but not fail.
     *
     * @param throwableType
     *     The type of the throwable to filter by.
     * @return A new {@link LogEventsAssert} containing the filtered log events. This may be empty.
     */
    public LogEventsAssert filteredByThrowableExactlyOfType(Class<? extends Throwable> throwableType)
    {
        return filteredBy(
            event -> event.getThrowable()
                .map(throwable -> throwable.getClass().equals(throwableType))
                .orElse(false)
        );
    }

    /**
     * Verifies that at least one log event has a throwable that is of the given type.
     * <p>
     * This method is similar to {@link #filteredByThrowableOfType(Class)} but asserts that the result is not empty.
     *
     * @param throwableType
     *     The type of the throwable to check for.
     * @return A new {@link LogEventsAssert} containing the filtered log events. This is never empty.
     */
    public LogEventsAssert hasAnyWithThrowableExactlyOfType(Class<? extends Throwable> throwableType)
    {
        return filteredByThrowableExactlyOfType(throwableType)
            .isNotEmpty(String.format(
                "Expected at least one log event with a throwable exactly of type %s but found none",
                throwableType.getName()
            ));
    }

    /**
     * Verifies that exactly one log event has a throwable that is exactly of the given type.
     *
     * @param throwableType
     *     The type of the throwable to check for.
     * @return A new {@link LogEventAssert} containing the log event that matches the expectation.
     */
    public LogEventAssert singleWithThrowableExactlyOfType(Class<? extends Throwable> throwableType)
    {
        return filteredByThrowableExactlyOfType(throwableType)
            .singleElement(count -> String.format(
                "Expected exactly one log event with a throwable exactly of type %s but found %d",
                throwableType.getName(),
                count
            ));
    }


    /**
     * Filters the log events that have any throwable associated with them.
     * <p>
     * Note: If no log events match the expectation, the returned {@link LogEventsAssert} will be empty but not fail.
     *
     * @return A new {@link LogEventsAssert} containing the filtered log events. This may be empty.
     */
    public LogEventsAssert filteredByHasThrowable()
    {
        return filteredBy(event -> event.getThrowable().isPresent());
    }

    /**
     * Verifies that at least one log event has a throwable associated with it.
     * <p>
     * This method is similar to {@link #filteredByHasThrowable()} but asserts that the result is not empty.
     *
     * @return A new {@link LogEventsAssert} containing the filtered log events. This is never empty.
     */
    public LogEventsAssert anyWithThrowable()
    {
        return filteredByHasThrowable()
            .isNotEmpty("Expected at least one log event with a throwable but found none");
    }

    /**
     * Verifies that exactly one log event has a throwable associated with it.
     *
     * @return A new {@link LogEventAssert} containing the log event that matches the expectation.
     */
    public LogEventAssert singleWithThrowable()
    {
        return filteredByHasThrowable()
            .singleElement("Expected exactly one log event with a throwable but found %d");
    }


    /**
     * Filters the log events that do not have any throwable associated with them.
     * <p>
     * Note: If no log events match the expectation, the returned {@link LogEventsAssert} will be empty but not fail.
     *
     * @return A new {@link LogEventsAssert} containing the filtered log events. This may be empty.
     */
    public LogEventsAssert filteredByWithoutThrowable()
    {
        return filteredBy(event -> event.getThrowable().isEmpty());
    }

    /**
     * Verifies that at least one log event has a throwable associated with it.
     * <p>
     * This method is similar to {@link #filteredByHasThrowable()} but asserts that the result is not empty.
     *
     * @return A new {@link LogEventsAssert} containing the filtered log events. This is never empty.
     */
    public LogEventsAssert anyWithoutThrowable()
    {
        return filteredByWithoutThrowable()
            .isNotEmpty("Expected at least one log event without a throwable but found none");
    }

    /**
     * Verifies that exactly one log event does not have a throwable associated with it.
     *
     * @return A new {@link LogEventAssert} containing the log event that matches the expectation.
     */
    public LogEventAssert singleWithoutThrowable()
    {
        return filteredByWithoutThrowable()
            .singleElement("Expected exactly one log event without a throwable but found %d");
    }


    /**
     * Filters the log events by the logger name.
     *
     * @param loggerName
     *     The name of the logger to filter by.
     * @return A new {@link LogEventsAssert} containing the filtered log events.
     */
    public LogEventsAssert filteredByLoggerName(String loggerName)
    {
        return filteredBy(event -> event.getLoggerName().equals(loggerName));
    }

    /**
     * Asserts that the log events contain exactly one element and returns an assertion for that element.
     * <p>
     * This method is a convenience method to assert that the log events contain exactly one element and uses the
     * provided message format to override the error message if the assertion fails.
     *
     * @param messageFormat
     *     The format string for the error message if the assertion fails. This should contain a single "%d" placeholder
     *     for the number of log events.
     *     <p>
     *     For example:
     *     <pre>{@code .singleElement("Expected exactly one log event but found %d")}</pre>
     * @return A new {@link LogEventAssert} containing the single log event that matches the expectation.
     */
    private LogEventAssert singleElement(String messageFormat)
    {
        return this
            .overridingErrorMessage(messageFormat, logEventCount())
            .singleElement();
    }

    /**
     * Asserts that the log events contain exactly one element and returns an assertion for that element.
     * <p>
     * This method is a convenience method to assert that the log events contain exactly one element and uses the
     * provided message format to override the error message if the assertion fails.
     *
     * @param messageFormatter
     *     A function that takes the number of log events and returns a formatted message.
     *     <p>
     *     For example:
     *     <pre>{@code .singleElement(i -> String.format("Expected exactly one log event but found %d", i))}</pre>
     * @return A new {@link LogEventAssert} containing the single log event that matches the expectation.
     */
    private LogEventAssert singleElement(IntFunction<String> messageFormatter)
    {
        return this
            .overridingErrorMessage(messageFormatter.apply(logEventCount()))
            .singleElement();
    }

    @Override
    public LogEventAssert singleElement()
    {
        if (actual.size() == 1)
        {
            return toAssert(actual.getFirst(), "Single log event");
        }

        this.appendLogEventsToInfo()
            .failWithMessage("Expected exactly one log event but found %d", actual.size());

        // This line will never be reached, but it is required to satisfy the compiler.
        return new LogEventAssert(null);
    }

    /**
     * Appends the formatted log events to the info message.
     *
     * @return {@code this} instance of {@link LogEventsAssert} for method chaining.
     */
    private LogEventsAssert appendLogEventsToInfo()
    {
        final String base = info.overridingErrorMessage();
        final String formattedLogEvents = formatLogEvents(logCaptor.getLogEvents());

        if (base == null || base.isEmpty())
        {
            info.overridingErrorMessage(formattedLogEvents);
        }
        else
        {
            info.overridingErrorMessage(base + "\n" + formattedLogEvents);
        }
        return this;
    }

    /**
     * Asserts that the log events contain at least one element and returns an assertion for that element.
     * <p>
     * This method is a convenience method to assert that the log events contain at least one element and uses the
     * provided message to override the error message if the assertion fails.
     *
     * @param message
     *     The message to override the error message if the assertion fails.
     *     <p>
     *     For example: <pre>{@code .anyElement("Expected at least one log event but found none")}</pre>
     * @return {@code this} instance of {@link LogEventsAssert} for method chaining.
     */
    private LogEventsAssert isNotEmpty(String message)
    {
        return this
            .overridingErrorMessage(message)
            .appendLogEventsToInfo()
            .isNotEmpty();
    }

    private List<LogEvent> logEventsFilteredBy(Predicate<LogEvent> predicate)
    {
        return actual.stream()
            .filter(predicate)
            .toList();
    }
}
