package nl.pim16aap2.testing.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents an inspector for a LogBack logger.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
public final class LogInspector extends AppenderBase<ILoggingEvent>
{
    private static final LogInspector INSTANCE = new LogInspector();

    /**
     * The history of all logged events.
     */
    @GuardedBy("historyLock")
    private final LogEventHistory history = new LogEventHistory();

    // We use a fair lock to ensure the lock requests are processed in a fifo manner, to avoid issues where
    // a reader might acquire a lock before a writer even though the writer requested it first.
    // In the testing setting, the reads will often come immediately after the writes, so in the default setting
    // (non-fair), exceptions might not always be available when trying to verify that they were logged.
    private final ReentrantLock historyLock = new ReentrantLock(true);

    private LogInspector()
    {
        initAppender();
    }

    public static LogInspector get()
    {
        return INSTANCE;
    }

    /**
     * Gets a filtered list of logging events that have been recorded.
     *
     * @param source
     *     The class the logging events were logged from. This may be null to process all logging events regardless of
     *     their originating class.
     * @param onlyThrowing
     *     True to only look for logging events where a throwable was thrown.
     * @param level
     *     The cutoff level. All logging events under this level will be filtered out.
     * @param includeHigherLevels
     *     Whether to include levels above the provided level in the search. When this is true, errors logged at, for
     *     example, {@link Level#ERROR} will also be included in the search when the provided level is
     *     {@link Level#INFO}.
     * @return The filtered list of logging events.
     */
    public List<ILoggingEvent> getLogHistory(
        @Nullable Class<?> source, boolean onlyThrowing,
        Level level, boolean includeHigherLevels)
    {
        return getWithHistoryLock(hist -> hist.getSelection(source, onlyThrowing, level, includeHigherLevels));
    }

    /**
     * Gets all logging events that contained a throwable and were logged with any log level.
     */
    public List<ILoggingEvent> getThrowingHistory(@Nullable Class<?> source)
    {
        return getLogHistory(source, true, Level.ALL, true);
    }

    /**
     * Gets all logging events that contained a throwable and were logged with any log level.
     */
    public List<ILoggingEvent> getThrowingHistory()
    {
        return getThrowingHistory(null);
    }

    /**
     * Gets a list of all {@link ILoggingEvent}s that have been recorded from a single class.
     *
     * @param source
     *     The class the logging events were logged from.
     * @return The list of all recorded logging events.
     */
    public List<ILoggingEvent> getLogHistory(@Nullable Class<?> source)
    {
        return getLogHistory(source, false, Level.ALL, true);
    }

    /**
     * Gets a list of all {@link ILoggingEvent}s that have been recorded.
     *
     * @return The list of all recorded logging events.
     */
    public List<ILoggingEvent> getLogHistory()
    {
        return getLogHistory(null);
    }

    /**
     * Gets the number of entries in the log history that match the provided description.
     *
     * @param source
     *     The class the logging events were logged from. This may be null to process all logging events regardless of
     *     their originating class.
     * @param onlyThrowing
     *     True to only look for logging events where a throwable was thrown.
     * @param level
     *     The cutoff level. All logging events under this level will be filtered out.
     * @param includeHigherLevels
     *     Whether to include levels above the provided level in the search. When this is true, errors logged at, for
     *     example, {@link Level#ERROR} will also be included in the search when the provided level is
     *     {@link Level#INFO}.
     * @return The number of entries in the log history that match the provided parameters.
     */
    public int getLogCount(@Nullable Class<?> source, boolean onlyThrowing, Level level, boolean includeHigherLevels)
    {
        return getWithHistoryLock(hist -> hist.getSize(source, onlyThrowing, level, includeHigherLevels));
    }

    /**
     * Gets the total number of entries in the log event history.
     *
     * @param source
     *     The class the logging events were logged from.
     */
    public int getLogCount(@Nullable Class<?> source)
    {
        return getLogCount(source, false, Level.ALL, true);
    }

    /**
     * Gets the number of logging events that contained a throwable and were logged with at least {@link Level#ERROR}
     * level.
     */
    public int getThrowingCount(@Nullable Class<?> source)
    {
        return getLogCount(source, true, Level.ERROR, true);
    }

    /**
     * Gets the number of logging events that contained a throwable and were logged with at least {@link Level#ERROR}
     * log level.
     */
    public int getThrowingCount()
    {
        return getThrowingCount(null);
    }

    /**
     * Gets the total number of entries in the log event history.
     */
    public int getLogCount()
    {
        return getLogCount(null);
    }

    /**
     * Gets the most recent entry in the logging event history that contained a throwable.
     *
     * @param source
     *     The class the logging events were logged from. This may be null to process all logging events regardless of
     *     their originating class.
     * @param level
     *     The cutoff level. All logging events under this level will be filtered out.
     * @param includeHigherLevels
     *     Whether to include levels above the provided level in the search. When this is true, errors logged at, for
     *     example, {@link Level#ERROR} will also be included in the search when the provided level is
     *     {@link Level#INFO}.
     * @return The most recent entry in the logging event history that contained a throwable.
     */
    public Optional<Throwable> getLastThrowable(@Nullable Class<?> source, Level level, boolean includeHigherLevels)
    {
        return getWithHistoryLock(hist -> hist.getLastThrowable(source, level, includeHigherLevels));
    }

    /**
     * Gets the most recent entry in the logging event history that contained a throwable and was logged at
     * {@link Level#ERROR} or above.
     *
     * @param source
     *     The class the logging events were logged from. This may be null to process all logging events regardless of
     *     their originating class.
     */
    public Optional<Throwable> getLastThrowable(@Nullable Class<?> source)
    {
        return getLastThrowable(source, Level.ERROR, true);
    }

    /**
     * Gets the most recent entry in the logging event history that contained a throwable and was logged at
     * {@link Level#ERROR} or above.
     */
    public Optional<Throwable> getLastThrowable()
    {
        return getLastThrowable(null);
    }

    @Override
    protected void append(ILoggingEvent eventObject)
    {
        withHistoryLock(hist -> hist.add(eventObject));
    }

    /**
     * Clears the history.
     */
    public void clearHistory()
    {
        withHistoryLock(LogEventHistory::clear);
    }

    private <T> T getWithHistoryLock(Function<LogEventHistory, T> fun)
    {
        historyLock.lock();
        try
        {
            return fun.apply(history);
        }
        finally
        {
            historyLock.unlock();
        }
    }

    private <T> void withHistoryLock(Consumer<LogEventHistory> consumer)
    {
        historyLock.lock();
        try
        {
            consumer.accept(history);
        }
        finally
        {
            historyLock.unlock();
        }
    }

    private void initAppender()
    {
        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        setContext(loggerContext);
        start();
        final Logger logbackLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        logbackLogger.detachAndStopAllAppenders();
        logbackLogger.addAppender(this);
        logbackLogger.setLevel(Level.ALL);
    }
}
