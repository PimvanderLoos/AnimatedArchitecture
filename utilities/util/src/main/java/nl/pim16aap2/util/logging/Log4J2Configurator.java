package nl.pim16aap2.util.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.apache.logging.log4j.core.filter.MarkerFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Path;

/**
 * Per-plugin Log4j2 configurator:
 * <p>
 * - plugin-specific RollingFile appender
 * <p>
 * - plugin-specific Console appender with [projectName] prefix
 * <p>
 * - additivity=false so nothing bubbles to root appenders
 * <p>
 * - dynamic level changes via VariableLevelFilter
 */
@NullMarked
public final class Log4J2Configurator
{
    private static final Level INITIAL_LEVEL = Level.INFO;

    private final String projectName;
    private final String loggerNamePrefix;
    private final VariableLevelFilter levelFilter;

    private final String fileAppenderName;
    private final String consoleAppenderName;

    private final Configuration configuration;
    private final LoggerContext context;

    public Log4J2Configurator(String projectName, String loggerNamePrefix)
    {
        this.projectName = projectName;
        this.loggerNamePrefix = loggerNamePrefix;

        this.levelFilter = new VariableLevelFilter(INITIAL_LEVEL);
        this.fileAppenderName = projectName + "-File";
        this.consoleAppenderName = projectName + "-Console";

        this.context = (LoggerContext) LogManager.getContext(false);
        this.configuration = context.getConfiguration();

        levelFilter.start();
    }

    public static void setMarkerName(String name)
    {
        CustomLog4j2Backend.setMarkerName(name);
    }

    public void setLevel(Level level)
    {
        levelFilter.level(level);
    }

    public void configure(Path logDir)
    {
        final var marker = CustomLog4j2Backend.getMarker();
        if (marker == null)
            throw new IllegalStateException("Marker not set, call Log4J2Configurator.setMarkerName() first");

        final var fileLayout = PatternLayout.newBuilder()
            .withConfiguration(configuration)
            .withPattern("[%date{ISO8601}] [%thread/%level] [%c{1.1.1.*}]: %message%n%throwable")
            .build();

        final var consoleLayout = PatternLayout.newBuilder()
            .withConfiguration(configuration)
            .withPattern("[%date{HH:mm:ss}] [%thread/%level] [" + projectName + "]: %message%n%throwable")
            .build();

        // Appender for our log file:
        final String base = logDir.toAbsolutePath().resolve("aa").toString();
        final var rollStrategy = DefaultRolloverStrategy.newBuilder()
            .withMax("3")
            .withFileIndex("min")
            .withCompressionLevelStr("5")
            .withConfig(configuration)
            .build();

        final var fileAppender = RollingFileAppender.newBuilder()
            .setConfiguration(configuration)
            .setName(fileAppenderName)
            .withFileName(base + ".log")
            .withFilePattern(base + ".%i.log.gz")
            .withPolicy(SizeBasedTriggeringPolicy.createPolicy("10MB"))
            .withStrategy(rollStrategy)
            .withAppend(true)
            .setLayout(fileLayout)
            .setFilter(levelFilter)
            .build();
        fileAppender.start();
        configuration.addAppender(fileAppender);

        // Also append to console:
        final var consoleAppender = ConsoleAppender.newBuilder()
            .setConfiguration(configuration)
            .setName(consoleAppenderName)
            .setTarget(ConsoleAppender.Target.SYSTEM_OUT)
            .setLayout(consoleLayout)
            .setFilter(levelFilter)
            .build();
        consoleAppender.start();
        configuration.addAppender(consoleAppender);

        final var loggerConfig = LoggerConfig.newBuilder()
            .withAdditivity(false)
            .withConfig(configuration)
            .withtFilter(CompositeFilter.createFilters(
                levelFilter,
                MarkerFilter.createFilter(marker.getName(), Filter.Result.NEUTRAL, Filter.Result.DENY)
            ))
            .withLevel(Level.ALL)
            .withLoggerName(loggerNamePrefix)
            .build();

        loggerConfig.addAppender(fileAppender, Level.ALL, levelFilter);
        loggerConfig.addAppender(consoleAppender, Level.ALL, levelFilter);

        // Clean up any previous logger with the same exact name to avoid duplicates.
        final LoggerConfig existing = configuration.getLoggerConfig(loggerNamePrefix);
        if (existing != null && loggerNamePrefix.equals(existing.getName()))
            configuration.removeLogger(loggerNamePrefix);

        configuration.addLogger(loggerNamePrefix, loggerConfig);
        context.updateLoggers();
    }

    public void removeLoggers()
    {
        configuration.removeLogger(loggerNamePrefix);

        stopAndRemoveAppender(fileAppenderName);
        stopAndRemoveAppender(consoleAppenderName);

        context.updateLoggers();
    }

    /**
     * Converts a {@link java.util.logging.Level} to a {@link Level}.
     *
     * @param level
     *     The level to convert.
     * @return The converted level.
     *
     * @deprecated Use log4j2 levels directly.
     */
    @Deprecated(forRemoval = true)
    public static org.apache.logging.log4j.Level toLog4jLevel(java.util.logging.Level level)
    {
        final int logLevel = level.intValue();

        if (logLevel == java.util.logging.Level.OFF.intValue())
        {
            return org.apache.logging.log4j.Level.OFF;
        }
        if (logLevel == java.util.logging.Level.ALL.intValue())
        {
            return org.apache.logging.log4j.Level.ALL;
        }

        /*
         * FINEST  -> TRACE
         * FINER   -> TRACE
         * FINE    -> DEBUG
         * CONFIG  -> DEBUG
         * INFO    -> INFO
         * WARNING -> WARN
         * SEVERE  -> ERROR
         */
        if (logLevel < java.util.logging.Level.FINE.intValue())
        {
            // <= FINER -> TRACE
            return org.apache.logging.log4j.Level.TRACE;
        }
        if (logLevel < java.util.logging.Level.CONFIG.intValue())
        {
            // FINE -> DEBUG
            return org.apache.logging.log4j.Level.DEBUG;
        }
        else if (logLevel < java.util.logging.Level.WARNING.intValue())
        {
            // INFO -> INFO
            return org.apache.logging.log4j.Level.INFO;
        }
        else if (logLevel < java.util.logging.Level.SEVERE.intValue())
        {
            // WARNING -> WARN
            return org.apache.logging.log4j.Level.WARN;
        }
        // >= SEVERE -> ERROR
        return org.apache.logging.log4j.Level.ERROR;
    }

    private void stopAndRemoveAppender(String name)
    {
        final Appender appender = configuration.getAppenders().remove(name);
        if (appender != null)
            appender.stop();
    }
}
