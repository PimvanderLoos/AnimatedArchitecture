package nl.pim16aap2.bigdoors.spigot.core.logging;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import org.bukkit.Bukkit;

import java.time.Instant;
import java.util.logging.LogRecord;

/**
 * Represents an appender that redirects our logger's output to {@link Bukkit#getLogger()}.
 *
 * @author Pim
 */
public class ConsoleAppender extends AppenderBase<LoggingEvent>
{
    /**
     * Gets the {@link java.util.logging.Level} type of level from a {@link ch.qos.logback.classic.Level} type.
     * <p>
     * When the level could not be mapped, it defaults to {@link java.util.logging.Level#CONFIG}.
     *
     * @param lvl
     *     The {@link ch.qos.logback.classic.Level} to map to a {@link java.util.logging.Level}.
     * @return The mapped {@link java.util.logging.Level} if it could be mapped, otherwise {@link
     * java.util.logging.Level#CONFIG}.
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
        return java.util.logging.Level.CONFIG;
    }


    @Override
    protected void append(LoggingEvent eventObject)
    {
        final java.util.logging.Level level = getJULLevel(eventObject.getLevel());
        // It doesn't matter what our logging settings are if Bukkit will just refuse it anyway.
        if (!Bukkit.getLogger().isLoggable(level))
            return;

        final LogRecord logRecord = new LogRecord(level, eventObject.getFormattedMessage());

        logRecord.setInstant(Instant.ofEpochMilli(eventObject.getTimeStamp()));
        logRecord.setLoggerName(eventObject.getLoggerName());
        logRecord.setLongThreadID(Thread.currentThread().getId());
        if (eventObject.getThrowableProxy() != null &&
            eventObject.getThrowableProxy() instanceof ThrowableProxy throwableProxy)
            logRecord.setThrown(throwableProxy.getThrowable());

        Bukkit.getLogger().log(logRecord);
    }


}
