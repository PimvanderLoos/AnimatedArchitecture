package nl.pim16aap2.testing.assertions;

import lombok.experimental.UtilityClass;
import nl.altindag.log.model.LogEvent;
import nl.pim16aap2.testing.annotations.WithLogCapture;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.logging.Level;
import java.util.stream.Stream;

/**
 * Represents a set of utility methods to assert logs.
 * <p>
 * To use this utility, you should use the {@link WithLogCapture} annotation on your test class. This will set up log
 * capturing for your test and make the logs available for assertions.
 */
@UtilityClass
@NullMarked
final class LogAssertionsUtil
{
    /**
     * Filters the log events by the specified log level.
     *
     * @param logEvents
     *     The log events to filter.
     * @param level
     *     The log level to filter by.
     * @return A stream of log events that match the specified log level.
     */
    static Stream<LogEvent> filterByLogLevel(List<LogEvent> logEvents, @Nullable Level level)
    {
        if (level == null)
            return logEvents.stream();

        return logEvents.stream()
            .filter(logEvent -> logEvent.getLevel().equals(level.getName()));
    }

    /**
     * Formats the log events as an enumerated string.
     *
     * @param logEvents
     *     The log events to process.
     * @return The most recent log messages as an enumerated string with the position from the last log event.
     */
    static String formatLogEvents(List<LogEvent> logEvents)
    {
        return formatLogEvents(logEvents, logEvents.size());
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
                .append(String.format(" [%s] ", logEvent.getLevel()))
                .append('`').append(logEvent.getMessage()).append('`')
                .append(" from Logger ")
                .append(logEvent.getLoggerName());

            logEvent.getThrowable().ifPresent(throwable ->
                builder.append(" With throwable: `")
                    .append(throwable.getClass().getName())
                    .append("`: `")
                    .append(throwable.getMessage()).append('`'));

            builder.append('\n');
        }

        return builder.toString();
    }
}
