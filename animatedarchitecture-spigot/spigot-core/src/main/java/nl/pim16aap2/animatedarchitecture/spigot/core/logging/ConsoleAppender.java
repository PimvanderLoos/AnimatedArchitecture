package nl.pim16aap2.animatedarchitecture.spigot.core.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Represents an appender that redirects our logger's output to {@link Bukkit#getLogger()}.
 *
 * @author Pim
 */
@AllArgsConstructor
public class ConsoleAppender extends AppenderBase<ILoggingEvent>
{
    @Getter
    private final String name;

    /**
     * Whether to remap the log level of the console output to {@link java.util.logging.Level#INFO} to work around the
     * fact that Bukkit only accepts {@link java.util.logging.Level#INFO} and higher without further configuration.
     */
    @Getter
    @Setter
    private volatile boolean remapLogLevel;

    /**
     * Gets the {@link java.util.logging.Level} type of level from a {@link ch.qos.logback.classic.Level} type.
     * <p>
     * When the level could not be mapped, it defaults to {@link java.util.logging.Level#INFO}.
     *
     * @param lvl
     *     The {@link ch.qos.logback.classic.Level} to map to a {@link java.util.logging.Level}.
     * @return The mapped {@link java.util.logging.Level} if it could be mapped, otherwise
     * {@link java.util.logging.Level#INFO}.
     */
    private static java.util.logging.Level getJULLevel(ch.qos.logback.classic.Level lvl)
    {
        if (lvl.equals(ch.qos.logback.classic.Level.OFF))
            return java.util.logging.Level.OFF;
        if (lvl.equals(ch.qos.logback.classic.Level.ERROR))
            return java.util.logging.Level.SEVERE;
        if (lvl.equals(ch.qos.logback.classic.Level.WARN))
            return java.util.logging.Level.WARNING;
        if (lvl.equals(ch.qos.logback.classic.Level.INFO))
            return java.util.logging.Level.INFO;
        if (lvl.equals(ch.qos.logback.classic.Level.DEBUG))
            return java.util.logging.Level.CONFIG;
        if (lvl.equals(ch.qos.logback.classic.Level.TRACE))
            return java.util.logging.Level.FINE;
        if (lvl.equals(ch.qos.logback.classic.Level.ALL))
            return java.util.logging.Level.ALL;

        // Default:
        return java.util.logging.Level.INFO;
    }

    @Override
    protected void append(ILoggingEvent eventObject)
    {
        java.util.logging.Level level = getJULLevel(eventObject.getLevel());
        String prefix = "";

        if (!Bukkit.getLogger().isLoggable(level))
        {
            if (!remapLogLevel)
                return;
            prefix = "{" + level.getName() + "} ";
            level = java.util.logging.Level.INFO;
        }

        Bukkit.getLogger().log(getLogRecord(eventObject, level, prefix));
    }

    private static LogRecord getLogRecord(ILoggingEvent eventObject, Level level, String prefix)
    {
        final LogRecord logRecord = new LogRecord(level, prefix + eventObject.getFormattedMessage());

        logRecord.setInstant(Instant.ofEpochMilli(eventObject.getTimeStamp()));
        logRecord.setLoggerName(eventObject.getLoggerName());
        logRecord.setLongThreadID(Thread.currentThread().threadId());
        if (eventObject.getThrowableProxy() != null &&
            eventObject.getThrowableProxy() instanceof ThrowableProxy throwableProxy)
            logRecord.setThrown(throwableProxy.getThrowable());

        return logRecord;
    }
}
