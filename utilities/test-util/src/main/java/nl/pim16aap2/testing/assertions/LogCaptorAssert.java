package nl.pim16aap2.testing.assertions;

import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.logging.Level;

import static nl.pim16aap2.testing.assertions.LogAssertionsUtil.filterByLogLevel;
import static nl.pim16aap2.testing.assertions.LogAssertionsUtil.formatLogEvents;

/**
 * An assertion class for {@link LogCaptor} instances.
 * <p>
 * This class provides methods to assert various conditions on the logs captured by a {@link LogCaptor}. It extends
 * {@link AbstractAssert} from AssertJ to provide fluent assertions.
 */
@NullMarked
public class LogCaptorAssert extends AbstractObjectAssert<LogCaptorAssert, LogCaptor>
{
    LogCaptorAssert(LogCaptor actual)
    {
        super(actual, LogCaptorAssert.class);
        isNotNull();
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
     * Gets all logs captured by the {@link LogCaptor}. The result may be empty.
     *
     * @return A {@link LogEventsAssert} containing the captured log events.
     */
    public LogEventsAssert atAllLevels()
    {
        return at(null);
    }

    /**
     * Gets all warning logs captured by the {@link LogCaptor}. The result may be empty.
     *
     * @return A {@link LogEventsAssert} containing the log events at the specified level.
     */
    public LogEventsAssert atSevere()
    {
        return at(Level.SEVERE);
    }

    /**
     * Gets all warning logs captured by the {@link LogCaptor}. The result may be empty.
     *
     * @return A {@link LogEventsAssert} containing the log events at the specified level.
     */
    public LogEventsAssert atWarning()
    {
        return at(Level.WARNING);
    }

    /**
     * Gets all info logs captured by the {@link LogCaptor}. The result may be empty.
     *
     * @return A {@link LogEventsAssert} containing the log events at the specified level.
     */
    public LogEventsAssert atInfo()
    {
        return at(Level.INFO);
    }

    /**
     * Gets all config logs captured by the {@link LogCaptor}. The result may be empty.
     *
     * @return A {@link LogEventsAssert} containing the log events at the specified level.
     */
    public LogEventsAssert atConfig()
    {
        return at(Level.CONFIG);
    }

    /**
     * Gets all fine logs captured by the {@link LogCaptor}. The result may be empty.
     *
     * @return A {@link LogEventsAssert} containing the log events at the specified level.
     */
    public LogEventsAssert atFine()
    {
        return at(Level.FINE);
    }

    /**
     * Gets all finer logs captured by the {@link LogCaptor}. The result may be empty.
     *
     * @return A {@link LogEventsAssert} containing the log events at the specified level.
     */
    public LogEventsAssert atFiner()
    {
        return at(Level.FINER);
    }

    /**
     * Gets all finest logs captured by the {@link LogCaptor}. The result may be empty.
     *
     * @return A {@link LogEventsAssert} containing the log events at the specified level.
     */
    public LogEventsAssert atFinest()
    {
        return at(Level.FINEST);
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
     * Verifies that no severe logs were captured.
     *
     * @return {@code this} assertion object
     */
    public LogCaptorAssert hasNoSevereLogs()
    {
        return hasNoLogsOfLevel(Level.SEVERE);
    }

    /**
     * Verifies that no warning logs were captured.
     *
     * @return {@code this} assertion object
     */
    public LogCaptorAssert hasNoWarningLogs()
    {
        return hasNoLogsOfLevel(Level.WARNING);
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
     * Verifies that no config logs were captured.
     *
     * @return {@code this} assertion object
     */
    public LogCaptorAssert hasNoConfigLogs()
    {
        return hasNoLogsOfLevel(Level.CONFIG);
    }

    /**
     * Verifies that no fine logs were captured.
     *
     * @return {@code this} assertion object
     */
    public LogCaptorAssert hasNoFineLogs()
    {
        return hasNoLogsOfLevel(Level.FINE);
    }

    /**
     * Verifies that no finer logs were captured.
     *
     * @return {@code this} assertion object
     */
    public LogCaptorAssert hasNoFinerLogs()
    {
        return hasNoLogsOfLevel(Level.FINER);
    }

    /**
     * Verifies that no finest logs were captured.
     *
     * @return {@code this} assertion object
     */
    public LogCaptorAssert hasNoFinestLogs()
    {
        return hasNoLogsOfLevel(Level.FINEST);
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
