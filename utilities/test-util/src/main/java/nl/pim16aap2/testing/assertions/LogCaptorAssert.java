package nl.pim16aap2.testing.assertions;

import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.apache.logging.log4j.Level;
import org.assertj.core.api.AbstractAssert;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

import static nl.pim16aap2.testing.assertions.LogAssertionsUtil.filterByLogLevel;
import static nl.pim16aap2.testing.assertions.LogAssertionsUtil.formatLogEvents;

/**
 * An assertion class for {@link LogCaptor} instances.
 * <p>
 * This class provides methods to assert various conditions on the logs captured by a {@link LogCaptor}. It extends
 * {@link AbstractAssert} from AssertJ to provide fluent assertions.
 */
@NullMarked
public class LogCaptorAssert extends AbstractAssert<LogCaptorAssert, LogCaptor>
{
    LogCaptorAssert(LogCaptor actual)
    {
        super(actual, LogCaptorAssert.class);
    }

    /**
     * Creates a new {@link LogCaptorAssert} for the given {@link LogCaptor}.
     *
     * @param actual
     *     The {@link LogCaptor} to create the assertion for.
     * @return A new {@link LogCaptorAssert} for the given {@link LogCaptor}.
     */
    public static LogCaptorAssert assertThatLogCaptor(LogCaptor actual)
    {
        return new LogCaptorAssert(actual);
    }

    @Override
    protected void failWithMessage(String errorMessage, Object... arguments)
    {
        final String newMessage = errorMessage + "\n" + formatLogEvents(actual.getLogEvents());
        super.failWithMessage(newMessage, arguments);
    }

    /**
     * Verifies that the {@link LogCaptor} has captured one or more log events.
     *
     * @return A {@link LogEventsAssert} containing the captured log events.
     */
    public LogEventsAssert atAllLevels()
    {
        return at(null);
    }

    /**
     * Verifies that one or more error logs were captured.
     *
     * @return A {@link LogEventsAssert} containing the error logs.
     */
    public LogEventsAssert atError()
    {
        return at(Level.ERROR);
    }

    /**
     * Verifies that one or more warning logs were captured.
     *
     * @return A {@link LogEventsAssert} containing the warning logs.
     */
    public LogEventsAssert atWarn()
    {
        return at(Level.WARN);
    }

    /**
     * Verifies that one or more info logs were captured.
     *
     * @return A {@link LogEventsAssert} containing the info logs.
     */
    public LogEventsAssert atInfo()
    {
        return at(Level.INFO);
    }

    /**
     * Verifies that one or more debug logs were captured.
     *
     * @return A {@link LogEventsAssert} containing the debug logs.
     */
    public LogEventsAssert atDebug()
    {
        return at(Level.DEBUG);
    }

    /**
     * Verifies that one or more trace logs were captured.
     *
     * @return A {@link LogEventsAssert} containing the trace logs.
     */
    public LogEventsAssert atTrace()
    {
        return at(Level.TRACE);
    }

    private LogEventsAssert at(@Nullable Level level)
    {
        final List<LogEvent> logEvents = getLogsOfLevel(level);
        return LogEventsAssert.assertThatLogEvents(logEvents, actual);
    }

    /**
     * Verifies that no logs were captured.
     *
     * @return {@code this} assertion object
     */
    public LogCaptorAssert hasNoLogs()
    {
        if (!actual.getLogs().isEmpty())
            failWithMessage("Expected no logs, but found: %s", actual.getLogs());
        return this;
    }

    /**
     * Verifies that no error logs were captured.
     *
     * @return {@code this} assertion object
     */
    public LogCaptorAssert hasNoErrorLogs()
    {
        return hasNoLogsOfLevel(Level.ERROR);
    }

    /**
     * Verifies that no warning logs were captured.
     *
     * @return {@code this} assertion object
     */
    public LogCaptorAssert hasNoWarnLogs()
    {
        return hasNoLogsOfLevel(Level.WARN);
    }

    /**
     * Verifies that no info logs were captured.
     *
     * @return {@code this} assertion object
     */
    public LogCaptorAssert hasNoInfoLogs()
    {
        return hasNoLogsOfLevel(Level.INFO);
    }

    /**
     * Verifies that no debug logs were captured.
     *
     * @return {@code this} assertion object
     */
    public LogCaptorAssert hasNoDebugLogs()
    {
        return hasNoLogsOfLevel(Level.DEBUG);
    }

    /**
     * Verifies that no trace logs were captured.
     *
     * @return {@code this} assertion object
     */
    public LogCaptorAssert hasNoTraceLogs()
    {
        return hasNoLogsOfLevel(Level.TRACE);
    }

    private LogCaptorAssert hasNoLogsOfLevel(Level level)
    {
        int messageCount = getLogsOfLevel(level).size();

        if (messageCount != 0)
            failWithMessage("Expected no logs of level '%s', but found %d", level, messageCount);

        return this;
    }

    /**
     * Retrieves the logs of the specified level from the {@link LogCaptor}.
     *
     * @param level
     *     The log level to filter by, or {@code null} to get all logs.
     * @return A list of {@link LogEvent} objects that match the specified log level.
     */
    private List<LogEvent> getLogsOfLevel(@Nullable Level level)
    {
        return filterByLogLevel(actual.getLogEvents(), level).toList();
    }
}
