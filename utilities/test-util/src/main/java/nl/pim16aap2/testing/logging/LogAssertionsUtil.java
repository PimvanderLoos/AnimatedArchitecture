package nl.pim16aap2.testing.logging;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import nl.pim16aap2.util.logging.floggerbackend.Log4j2LogEventUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

/**
 * Represents a set of utility methods to assert logs.
 * <p>
 * To use this utility, you should use the {@link WithLogCapture} annotation on your test class. This will set up log
 * capturing for your test and make the logs available for assertions.
 */
public final class LogAssertionsUtil
{
    private LogAssertionsUtil()
    {
        // Utility class
    }

    /**
     * Gets the log event at the specified position.
     * <p>
     * If the provided position is non-negative, the position will be relative to the start of the log events. For
     * example, 0 will get the first log event, 1 will get the second, etc.
     * <p>
     * If the position is negative, the position will be relative to the end of the log events. For example, -1 will get
     * the last log event, -2 will get the second to last, etc.
     *
     * @param logEvents
     *     The log events to get the log event from.
     * @param position
     *     The position of the log event to get. This can be either the absolute position from the start of the log
     *     events or a negative position from the end of the log events.
     * @return The log event at the specified position.
     */
    static LogEvent getLogEvent(List<LogEvent> logEvents, int position)
    {
        final boolean isRelative = position < 0;
        final int index = isRelative ? logEvents.size() + position : position;

        if (index >= logEvents.size() || index < 0)
            Assertions.fail(String.format(
                """
                    Expected at least %d log events, but only got %d!
                    %s
                    """,
                Math.abs(position),
                logEvents.size(),
                formatLogEvents(logEvents, isRelative ? -10 : 10))
            );

        return logEvents.get(index);
    }

    /**
     * Formats the log events as an enumerated string.
     *
     * @param logEvents
     *     The log events to process.
     * @param count
     *     The number of log messages to get. When non-negative, the oldest log messages will be returned. When
     *     negative, the most recent log messages will be returned.
     * @return The most recent log messages as an enumerated string with the position from the last log event. Each log
     * event is separated by a newline.
     */
    static String formatLogEvents(List<LogEvent> logEvents, int count)
    {
        if (logEvents.isEmpty())
            return "No log events were captured!";

        final var builder = new StringBuilder();
        final int numEvents = Math.min(Math.abs(count), logEvents.size());
        final boolean isRelative = count < 0;

        if (isRelative)
            // PMD False positive: https://github.com/pmd/pmd/issues/4910
            builder.append("Most recent log messages with their offset:\n"); //NOPMD
        else
            builder.append("Oldest log messages with their index:\n");

        // Get the format based on the number of events to ensure that the format has enough digits for the enumeration.
        final var format = String.format("  [%%%dd]", (int) Math.log10(numEvents) + 1 + (isRelative ? 1 : 0));

        final int start = isRelative ? logEvents.size() - 1 : 0;

        for (int processed = 0; processed < numEvents; ++processed)
        {
            final int index = isRelative ? start - processed : start + processed;
            final int position = isRelative ? -processed - 1 : processed;

            final var logEvent = logEvents.get(index);
            builder
                .append(String.format(format, position))
                .append(" [").append(logEvent.getLevel()).append("] `")
                .append(logEvent.getMessage())
                .append("`\n");
        }
        return builder.toString();
    }

    /**
     * Creates a new instance of a {@link LogAssertion.LogAssertionBuilder} with the provided log captor.
     *
     * @param logCaptor
     *     The log captor to use.
     * @return A new instance of a {@link LogAssertion.LogAssertionBuilder}.
     */
    public static LogAssertion.LogAssertionBuilder logAssertionBuilder(LogCaptor logCaptor)
    {
        return LogAssertion.builder().logCaptor(logCaptor);
    }

    private static void assertMessageEquality(
        String title,
        LogAssertion logAssertion,
        String expected,
        String actual,
        List<LogEvent> logEvents)
    {
        if (!logAssertion.comparisonMethod.compare(expected, actual))
            Assertions.fail(String.format(
                """
                    Expected %s: '%s' at position %d
                    Received %s: '%s'
                    Comparison method: %s
                    %s
                    """,
                title, expected, logAssertion.position,
                title, actual,
                logAssertion.comparisonMethod,
                formatLogEvents(logEvents, 10))
            );
    }

    private static void assertEquality(
        String title,
        LogAssertion logAssertion,
        Object expected,
        Object actual,
        List<LogEvent> logEvents)
    {
        if (!Objects.equals(expected, actual))
            Assertions.fail(String.format(
                """
                    Expected %s: '%s' at position %d
                    Received %s: '%s'
                    %s
                    """,
                title, expected, logAssertion.position,
                title, actual,
                formatLogEvents(logEvents, 10))
            );
    }

    private static void findLogEvent(LogAssertion logAssertion)
    {
        final var logEvents = logAssertion.logCaptor.getLogEvents();

        var stream = logAssertion
            .logCaptor
            .getLogEvents()
            .stream();

        if (logAssertion.level != null)
            stream = stream.filter(logEvent -> logEvent.getLevel().equals(logAssertion.level));

        if (logAssertion.formattedMessage != null)
            stream = stream.filter(logEvent -> logAssertion.comparisonMethod.compare(
                logAssertion.formattedMessage,
                logEvent.getFormattedMessage())
            );

        if (stream.findAny().isEmpty())
            Assertions.fail(String.format(
                """
                    Could not find log event: %s
                    %s
                    """,
                logAssertion,
                formatLogEvents(logEvents, logEvents.size()))
            );
    }

    /**
     * Asserts that a log event was logged.
     * <p>
     * Creates a new instance of a {@link LogAssertion.LogAssertionBuilder} using
     * {@link LogAssertionsUtil#logAssertionBuilder(LogCaptor)}.
     *
     * @param logAssertion
     *     The log assertion to check.
     */
    public static void assertLogged(LogAssertion logAssertion)
    {
        if (logAssertion.position == null)
        {
            findLogEvent(logAssertion);
            return;
        }

        final var logEvents = logAssertion.logCaptor.getLogEvents();
        final var logEvent = getLogEvent(logEvents, logAssertion.position);

        if (logAssertion.formattedMessage != null)
            assertMessageEquality(
                "Formatted Message",
                logAssertion,
                logAssertion.formattedMessage,
                logEvent.getFormattedMessage(),
                logEvents
            );

        if (logAssertion.level != null)
            assertEquality(
                "Log Level",
                logAssertion,
                logAssertion.level,
                logEvent.getLevel(),
                logEvents
            );
    }

    /**
     * Ensures that a message was logged. The assertion will fail if the message was not logged.
     *
     * @param logCaptor
     *     The log captor to get the log event from.
     * @param position
     *     The position of the log event to check. 0 means the first (oldest) log event, 1 means the second, etc.
     *     <p>
     *     If the position is negative, the position will be relative to the end of the log events. For example, -1 will
     *     get the last log event, -2 will get the second to last, etc.
     * @param message
     *     The expected message that was logged.
     * @param comparisonMethod
     *     The method to use when comparing the expected and actual messages.
     */
    public static void assertLogged(
        LogCaptor logCaptor,
        int position,
        String message,
        MessageComparisonMethod comparisonMethod)
    {
        // TODO: Delete this method and replace all calls with the builder method.
        assertLogged(
            LogAssertion.builder()
                .logCaptor(logCaptor)
                .position(position)
                .formattedMessage(message)
                .comparisonMethod(comparisonMethod)
                .build()
        );
    }

    /**
     * Ensures that a message was logged. The assertion will fail if the throwable was not logged.
     * <p>
     * Shortcut for {@link #assertLogged(LogCaptor, int, String, MessageComparisonMethod)} with a position of -1 and
     * using {@link MessageComparisonMethod#EQUALS} for the comparison method.
     *
     * @param logCaptor
     *     The log captor to check.
     * @param message
     *     The expected message that was logged along with the throwable. If null, the message will not be checked.
     */
    public static void assertLogged(LogCaptor logCaptor, String message)
    {
        assertLogged(logCaptor, -1, message, MessageComparisonMethod.EQUALS);
    }

    /**
     * Ensures that a message was logged. The assertion will fail if the message was not logged.
     * <p>
     * Shortcut for {@link #assertLogged(LogCaptor, int, String, MessageComparisonMethod)} using
     * {@link MessageComparisonMethod#EQUALS} for the comparison method.
     *
     * @param logCaptor
     *     The log captor to check.
     * @param position
     *     The position of the log event to check. 0 means the first (oldest) log event, 1 means the second, etc.
     *     <p>
     *     If the position is negative, the position will be relative to the end of the log events. For example, -1 will
     *     get the last log event, -2 will get the second to last, etc.
     * @param message
     *     The expected message that was logged along with the throwable. If null, the message will not be checked.
     */
    public static void assertLogged(LogCaptor logCaptor, int position, String message)
    {
        assertLogged(logCaptor, position, message, MessageComparisonMethod.EQUALS);
    }

    /**
     * Ensures that a message was logged. The assertion will fail if the message was not logged.
     * <p>
     * Shortcut for {@link #assertLogged(LogCaptor, int, String, MessageComparisonMethod)} with an offset of 1.
     *
     * @param logCaptor
     *     The log captor to check.
     * @param message
     *     The expected message that was logged along with the throwable. If null, the message will not be checked.
     * @param comparisonMethod
     *     The method to use when comparing the expected and actual messages.
     */
    public static void assertLogged(LogCaptor logCaptor, String message, MessageComparisonMethod comparisonMethod)
    {
        assertLogged(logCaptor, -1, message, comparisonMethod);
    }

    /**
     * Converts a throwable to a string.
     *
     * @param throwable
     *     The throwable to convert.
     * @return The string representation of the throwable.
     */
    private static String throwableToString(@Nullable Throwable throwable)
    {
        if (throwable == null)
            return "null";

        final var stringWriter = new StringWriter();
        final var printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    /**
     * Ensures that a certain number of throwables were logged. The assertion will fail if the number of throwables
     * logged does not match the expected count.
     *
     * @param logCaptor
     *     The log captor to check.
     * @param count
     *     The expected number of throwables to be logged.
     */
    public static void assertThrowingCount(LogCaptor logCaptor, int count)
    {
        final int throwingCount = getThrowingCount(logCaptor);
        if (throwingCount == count)
            return;

        Assertions.fail(String.format(
            """
                Expected %d throwables to be logged, but instead got %d!
                %s
                """,
            count,
            throwingCount,
            formatLogEvents(logCaptor.getLogEvents(), count))
        );
    }

    /**
     * Ensures that a throwable with the correct causes was logged. The assertion will fail if the throwable was not
     * logged.
     * <p>
     * Shortcut for {@link #assertThrowableNesting(Throwable, ThrowableSpec[])}.
     *
     * @param logCaptor
     *     The log captor to check.
     * @param position
     *     The position of the log event to check. 0 means the first (oldest) log event, 1 means the second, etc.
     *     <p>
     *     If the position is negative, the position will be relative to the end of the log events. For example, -1 will
     *     get the last log event, -2 will get the second to last, etc.
     * @param expectedMessage
     *     The expected message that was logged along with the throwable. If null, the message will not be checked.
     * @param expectedTypes
     *     The expected composition of the throwable that was logged. See {@link ThrowableSpec#matches(Throwable)}.
     */
    public static void assertThrowableLogged(
        LogCaptor logCaptor,
        int position,
        @Nullable String expectedMessage,
        Class<?>... expectedTypes)
    {
        assertThrowableLogged(logCaptor, position, expectedMessage, ThrowableSpec.of(expectedTypes));
    }

    /**
     * Ensures that a throwable with the correct causes was logged. The assertion will fail if the throwable was not
     * logged.
     * <p>
     * Shortcut for {@link #assertThrowableNesting(Throwable, ThrowableSpec[])} with
     * {@link ThrowableSpec#ThrowableSpec(Class, String)}.
     *
     * @param logCaptor
     *     The log captor to check.
     * @param position
     *     The position of the log event to check. 0 means the first (oldest) log event, 1 means the second, etc.
     *     <p>
     *     If the position is negative, the position will be relative to the end of the log events. For example, -1 will
     *     get the last log event, -2 will get the second to last, etc.
     * @param expectedMessage
     *     The expected message that was logged along with the throwable. If null, the message will not be checked.
     * @param throwableType0
     *     The expected type of the base throwable.
     * @param throwableMessage0
     *     The expected message of the base throwable.
     */
    public static void assertThrowableLogged(
        LogCaptor logCaptor,
        int position,
        @Nullable String expectedMessage,
        Class<?> throwableType0,
        @Nullable String throwableMessage0)
    {
        assertThrowableLogged(
            logCaptor,
            position,
            expectedMessage,
            new ThrowableSpec(throwableType0, throwableMessage0)
        );
    }

    /**
     * Ensures that a throwable with the correct causes was logged. The assertion will fail if the throwable was not
     * logged.
     * <p>
     * Shortcut for {@link #assertThrowableNesting(Throwable, ThrowableSpec[])} with
     * {@link ThrowableSpec#ThrowableSpec(Class, String)}.
     *
     * @param logCaptor
     *     The log captor to check.
     * @param position
     *     The position of the log event to check. 0 means the first (oldest) log event, 1 means the second, etc.
     *     <p>
     *     If the position is negative, the position will be relative to the end of the log events. For example, -1 will
     *     get the last log event, -2 will get the second to last, etc.
     * @param expectedMessage
     *     The expected message that was logged along with the throwable. If null, the message will not be checked.
     * @param throwableType0
     *     The expected type of the base throwable.
     * @param throwableMessage0
     *     The expected message of the base throwable.
     * @param throwableType1
     *     The expected type of the first nested throwable.
     * @param throwableMessage1
     *     The expected message of the first nested throwable.
     */
    public static void assertThrowableLogged(
        LogCaptor logCaptor,
        int position,
        @Nullable String expectedMessage,
        Class<?> throwableType0,
        @Nullable String throwableMessage0,
        Class<?> throwableType1,
        @Nullable String throwableMessage1)
    {
        assertThrowableLogged(
            logCaptor,
            position,
            expectedMessage,
            new ThrowableSpec(throwableType0, throwableMessage0),
            new ThrowableSpec(throwableType1, throwableMessage1)
        );
    }

    /**
     * Ensures that a throwable with the correct causes was logged. The assertion will fail if the throwable was not
     * logged.
     * <p>
     * Shortcut for {@link #assertThrowableNesting(Throwable, ThrowableSpec[])} with
     * {@link ThrowableSpec#ThrowableSpec(Class, String)}.
     *
     * @param logCaptor
     *     The log captor to check.
     * @param position
     *     The position of the log event to check. 0 means the first (oldest) log event, 1 means the second, etc.
     *     <p>
     *     If the position is negative, the position will be relative to the end of the log events. For example, -1 will
     *     get the last log event, -2 will get the second to last, etc.
     * @param expectedMessage
     *     The expected message that was logged along with the throwable. If null, the message will not be checked.
     * @param throwableType0
     *     The expected type of the base throwable.
     * @param throwableMessage0
     *     The expected message of the base throwable.
     * @param throwableType1
     *     The expected type of the first nested throwable.
     * @param throwableMessage1
     *     The expected message of the first nested throwable.
     * @param throwableType2
     *     The expected type of the second nested throwable.
     * @param throwableMessage2
     *     The expected message of the second nested throwable.
     */
    public static void assertThrowableLogged(
        LogCaptor logCaptor,
        int position,
        @Nullable String expectedMessage,
        Class<?> throwableType0,
        @Nullable String throwableMessage0,
        Class<?> throwableType1,
        @Nullable String throwableMessage1,
        Class<?> throwableType2,
        @Nullable String throwableMessage2)
    {
        assertThrowableLogged(
            logCaptor,
            position,
            expectedMessage,
            new ThrowableSpec(throwableType0, throwableMessage0),
            new ThrowableSpec(throwableType1, throwableMessage1),
            new ThrowableSpec(throwableType2, throwableMessage2)
        );
    }

    /**
     * Ensures that a throwable with the correct causes was logged. The assertion will fail if the throwable was not
     * logged.
     * <p>
     * Shortcut for {@link #assertThrowableNesting(Throwable, ThrowableSpec[])} with
     * {@link ThrowableSpec#ThrowableSpec(Class, String)}.
     *
     * @param logCaptor
     *     The log captor to check.
     * @param position
     *     The position of the log event to check. 0 means the first (oldest) log event, 1 means the second, etc.
     *     <p>
     *     If the position is negative, the position will be relative to the end of the log events. For example, -1 will
     *     get the last log event, -2 will get the second to last, etc.
     * @param expectedMessage
     *     The expected message that was logged along with the throwable. If null, the message will not be checked.
     * @param throwableType0
     *     The expected type of the base throwable.
     * @param throwableMessage0
     *     The expected message of the base throwable.
     * @param throwableType1
     *     The expected type of the first nested throwable.
     * @param throwableMessage1
     *     The expected message of the first nested throwable.
     * @param throwableType2
     *     The expected type of the second nested throwable.
     * @param throwableMessage2
     *     The expected message of the second nested throwable.
     * @param throwableType3
     *     The expected type of the third nested throwable.
     * @param throwableMessage3
     *     The expected message of the third nested throwable.
     */
    public static void assertThrowableLogged(
        LogCaptor logCaptor,
        int position,
        @Nullable String expectedMessage,
        Class<?> throwableType0,
        @Nullable String throwableMessage0,
        Class<?> throwableType1,
        @Nullable String throwableMessage1,
        Class<?> throwableType2,
        @Nullable String throwableMessage2,
        Class<?> throwableType3,
        @Nullable String throwableMessage3)
    {
        assertThrowableLogged(
            logCaptor,
            position,
            expectedMessage,
            new ThrowableSpec(throwableType0, throwableMessage0),
            new ThrowableSpec(throwableType1, throwableMessage1),
            new ThrowableSpec(throwableType2, throwableMessage2),
            new ThrowableSpec(throwableType3, throwableMessage3)
        );
    }

    /**
     * Ensures that a throwable with the correct causes was logged. The assertion will fail if the throwable was not
     * logged.
     *
     * @param logCaptor
     *     The log captor to check.
     * @param position
     *     The position of the log event to check. 0 means the first (oldest) log event, 1 means the second, etc.
     *     <p>
     *     If the position is negative, the position will be relative to the end of the log events. For example, -1 will
     *     get the last log event, -2 will get the second to last, etc.
     * @param expectedMessage
     *     The expected message that was logged along with the throwable. If null, the message will not be checked.
     * @param throwableSpecs
     *     The expected composition of the throwable that was logged. See
     *     {@link #assertThrowableNesting(Throwable, ThrowableSpec[])}.
     */
    public static void assertThrowableLogged(
        LogCaptor logCaptor,
        int position,
        @Nullable String expectedMessage,
        ThrowableSpec... throwableSpecs)
    {
        final var logEvent = getLogEvent(logCaptor.getLogEvents(), position);

        if (expectedMessage != null && !logEvent.getMessage().equals(expectedMessage))
            throw new AssertionFailedError(String.format(
                "Expected message '%s' to be logged, but instead got '%s'!",
                expectedMessage,
                logEvent.getMessage())
            );

        assertThrowableNesting(logEvent.getThrowable().orElse(null), throwableSpecs);
    }

    /**
     * Ensures that a throwable has the correct causes. The assertion will fail if the throwables do not match the
     * expected nesting.
     * <p>
     * Shortcut for {@link #assertThrowableNesting(Throwable, ThrowableSpec[])} with {@link ThrowableSpec#of(Class[])}.
     * <p>
     * None of the messages will be checked.
     *
     * @param base
     *     The base throwable to check.
     * @param expectedTypes
     *     The expected composition of the throwable that was logged. See {@link ThrowableSpec#matches(Throwable)}.
     */
    public static void assertThrowableNesting(@Nullable Throwable base, Class<?>... expectedTypes)
    {
        assertThrowableNesting(base, ThrowableSpec.of(expectedTypes));
    }

    /**
     * Ensures that a throwable has the correct causes. The assertion will fail if the throwables do not match the
     * expected nesting.
     * <p>
     * This method checks the nesting of the throwables, meaning that the first throwable in the array is expected to be
     * the root cause of the second, which is expected to be the root cause of the third, and so on.
     * <p>
     * If the base throwable is null, the method will throw an {@link AssertionFailedError}.
     * <p>
     * For each throwable spec, the method will ensure that the throwable matches the provided spec. See
     * {@link ThrowableSpec#matches(Throwable)}.
     * <p>
     * For example, when providing "[{RuntimeException.class, null}, IOException.class, "Error"]" the following
     * exception would be required to have been logged: new RuntimeException(new IOException("Error"));
     *
     * @param base
     *     The base throwable to check.
     * @param throwableSpecs
     *     The expected composition of the throwable that was logged.
     * @throws IllegalArgumentException
     *     If no throwables are specified.
     * @throws AssertionFailedError
     *     If the throwables do not match the expected nesting.
     */
    public static void assertThrowableNesting(@Nullable Throwable base, ThrowableSpec... throwableSpecs)
    {
        if (throwableSpecs.length < 1)
            throw new IllegalArgumentException("No throwables specified! Please provide at least 1!");

        @Nullable Throwable throwable = base;

        for (int idx = 0; idx < throwableSpecs.length; ++idx)
        {
            final ThrowableSpec expectedType = throwableSpecs[idx];

            if (!expectedType.matches(throwable))
            {
                throw new AssertionFailedError(String.format(
                    """
                        Expected exception of type %s with message '%s' at index %d
                        Received exception of type %s with message '%s'
                        Expected types: %s
                        Full stack trace of the base throwable:
                        %s
                        """,
                    expectedType.expectedType,
                    expectedType.expectedMessage,
                    idx,
                    throwable == null ? "null" : throwable.getClass(),
                    throwable == null ? "null" : throwable.getMessage(),
                    Arrays.toString(throwableSpecs),
                    throwableToString(base))
                );
            }
            throwable = throwable.getCause();
        }
    }

    /**
     * Gets the number of log events that were logged with a throwable.
     *
     * @param logCaptor
     *     The log captor to check.
     * @return The number of log events that were logged with a throwable.
     */
    public static int getThrowingCount(LogCaptor logCaptor)
    {
        return (int) logCaptor.getLogEvents().stream().filter(event -> event.getThrowable().isPresent()).count();
    }

    /**
     * Represents a specification for an expected exception.
     * <p>
     * This is used to specify the expected type of an exception and its message.
     * <p>
     * If the message is null, it will not be checked when comparing this specification to a throwable.
     *
     * @param expectedType
     *     The expected type of the exception.
     * @param expectedMessage
     *     The expected message of the exception.
     * @param stringCompareMethod
     *     The method to use when comparing the expected and actual messages.
     */
    public record ThrowableSpec(
        Class<?> expectedType,
        @Nullable String expectedMessage,
        MessageComparisonMethod stringCompareMethod)
    {
        /**
         * Creates a new instance of the exception specification.
         * <p>
         * Defaults to {@link MessageComparisonMethod#EQUALS} for the message comparison.
         *
         * @param expectedType
         *     The expected type of the exception.
         * @param expectedMessage
         *     The expected message of the exception.
         */
        public ThrowableSpec(Class<?> expectedType, @Nullable String expectedMessage)
        {
            this(expectedType, expectedMessage, MessageComparisonMethod.EQUALS);
        }

        /**
         * Creates a new instance of the exception specification.
         * <p>
         * Sets the expected message to null, meaning it will not be checked when comparing this specification to a
         * throwable.
         *
         * @param expectedType
         *     The expected type of the exception.
         */
        public ThrowableSpec(Class<?> expectedType)
        {
            this(expectedType, null);
        }

        /**
         * Checks whether the throwable matches the specification.
         *
         * @param throwable
         *     The throwable to check.
         * @return True if the throwable matches the specification, false otherwise.
         */
        @Contract(value = "null -> false", pure = true)
        public boolean matches(@Nullable Throwable throwable)
        {
            return expectedType.isInstance(throwable) &&
                messageMatches(throwable.getMessage());
        }

        /**
         * Checks whether the message matches the expected message.
         * <p>
         * If the expected message is null, this method will always return true.
         *
         * @param message
         *     The message to check.
         * @return True if the message matches, false otherwise.
         */
        public boolean messageMatches(@Nullable String message)
        {
            return expectedMessage == null || stringCompareMethod.compare(expectedMessage, message);
        }

        /**
         * Creates an array of ThrowableSpecs from the provided classes.
         * <p>
         * See {@link #ThrowableSpec(Class)}.
         *
         * @param classes
         *     The classes to create the ThrowableSpecs from.
         * @return The array of ThrowableSpecs.
         */
        public static ThrowableSpec[] of(Class<?>... classes)
        {
            return Arrays.stream(classes)
                .map(ThrowableSpec::new)
                .toArray(ThrowableSpec[]::new);
        }
    }

    /**
     * Represents a specification of an assertion for a log event.
     * <p>
     * New instances of this class can be created using the {@link LogAssertionsUtil#logAssertionBuilder} method.
     */
    @Builder
    @ToString
    @EqualsAndHashCode
    public static final class LogAssertion
    {
        /**
         * The log captor to get the log event from.
         * <p>
         * This field is required.
         */
        private LogCaptor logCaptor;

        /**
         * The position of the log event to check. 0 means the first (oldest) log event, 1 means the second, etc.
         * <p>
         * If the position is negative, the position will be relative to the end of the log events. For example, -1 will
         * get the last log event, -2 will get the second to last, etc.
         * <p>
         * <p>
         * When {@code null}, all log events will be checked until the first one that matches the conditions.
         * <p>
         * Default: {@code null}
         */
        @Builder.Default
        private @Nullable Integer position = null;

        /**
         * The fully formatted message that was logged, with all arguments substituted.
         * <p>
         * When {@code null}, the message will not be checked.
         * <p>
         * Default: {@code null}
         */
        @Builder.Default
        private @Nullable String formattedMessage = null;

        /**
         * The level of the log event.
         * <p>
         * When {@code null}, the level will not be checked.
         * <p>
         * Default: {@code null}
         */
        @Builder.Default
        private @Nullable String level = null;

        /**
         * The method to use when comparing the expected and actual messages.
         * <p>
         * Default: {@link MessageComparisonMethod#EQUALS}
         */
        @Builder.Default
        private MessageComparisonMethod comparisonMethod = MessageComparisonMethod.EQUALS;

        private static LogAssertionBuilder builder()
        {
            return new LogAssertionBuilder();
        }

        /**
         * Builder class for {@link LogAssertion}.
         */
        public static class LogAssertionBuilder
        {
            /**
             * Sets both the raw message and the arguments.
             * <p>
             * Note that this method will not set the formatted message.
             *
             * @param message
             *     The message to set. E.g. "Hello, %s!"
             * @param arguments
             *     The arguments to set.
             * @return This builder.
             */
            public LogAssertionBuilder message(String message, @Nullable Object... arguments)
            {
                this.formattedMessage(String.format(message, arguments));
                return this;
            }

            /**
             * Sets the expected log level of the log event.
             *
             * @param level
             *     The log level to set.
             * @return This builder.
             */
            public LogAssertionBuilder level(Level level)
            {
                this.level$value = Log4j2LogEventUtil.toLog4jLevel(level).name();
                this.level$set = true;
                return this;
            }

            /**
             * Runs the assertions on the log event as specified by this builder.
             */
            public void assertLogged()
            {
                LogAssertionsUtil.assertLogged(this.build());
            }
        }
    }

    /**
     * Represents a specification for an expected exception.
     */
    public enum MessageComparisonMethod
    {
        /**
         * Checks if the expected string is equal to the actual string.
         */
        EQUALS
            {
                @Override
                public boolean compare(String expected, @Nullable String actual)
                {
                    return Objects.equals(expected, actual);
                }
            },
        /**
         * Checks if the actual string contains the expected string.
         */
        CONTAINS
            {
                @Override
                public boolean compare(String expected, @Nullable String actual)
                {
                    if (actual == null)
                        return false;
                    return actual.contains(expected);
                }
            },
        /**
         * Checks if the actual string starts with the expected string.
         */
        STARTS_WITH
            {
                @Override
                public boolean compare(String expected, @Nullable String actual)
                {
                    if (actual == null)
                        return false;
                    return actual.startsWith(expected);
                }
            },
        /**
         * Checks if the actual string ends with the expected string.
         */
        ENDS_WITH
            {
                @Override
                public boolean compare(String expected, @Nullable String actual)
                {
                    if (actual == null)
                        return false;
                    return actual.endsWith(expected);
                }
            };

        /**
         * Compares the expected and actual strings.
         *
         * @param expected
         *     The expected string.
         * @param actual
         *     The actual string.
         * @return True if the strings match, false otherwise.
         */
        public abstract boolean compare(String expected, @Nullable String actual);
    }
}
