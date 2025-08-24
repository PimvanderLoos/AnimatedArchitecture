package nl.pim16aap2.util.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.nio.file.Path;

/**
 * Configures Log4J2 to log to a file at the specified path.
 * <p>
 * Only logs messages from the package {@code nl.pim16aap2}.
 * <p>
 * This class is a singleton. Use {@link #getInstance()} to get the instance.
 */
public final class Log4J2Configurator
{
    private static final Log4J2Configurator INSTANCE = new Log4J2Configurator();

    private static final String LOGGER_NAME = "nl.pim16aap2";
    private static final String MAIN_PACKAGE = "nl.pim16aap2.animatedarchitecture";
    private static final String UTIL_PACKAGE = "nl.pim16aap2.util";

    private final VariableLevelFilter levelFilter;

    private Log4J2Configurator()
    {
        levelFilter = new VariableLevelFilter(Level.ALL);
        levelFilter.start();
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

    /**
     * Gets the instance of the Log4J2Configurator.
     *
     * @return The instance of the Log4J2Configurator.
     */
    public static Log4J2Configurator getInstance()
    {
        return INSTANCE;
    }

    /**
     * Sets the level to filter on.
     * <p>
     * Any log event with a level lower than this level will be filtered out.
     *
     * @param level
     *     The level to filter on.
     */
    public void setLevel(Level level)
    {
        levelFilter.level(level);
    }

    /**
     * Sets the level to filter on.
     * <p>
     * Any log event with a level lower than this level will be filtered out.
     *
     * @param level
     *     The level to filter on.
     */
    public void setJULLevel(java.util.logging.Level level)
    {
        setLevel(toLog4jLevel(level));
    }

    /**
     * Sets the path to log to.
     * <p>
     * This method will create a new appender and logger for the specified path.
     *
     * @param path
     *     The path to log to.
     */
    public void setLogPath(Path path)
    {
        final LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        final Configuration configuration = loggerContext.getConfiguration();

        final var pattern = PatternLayout
            .newBuilder()
            .withPattern("[%date{ISO8601}] [%thread/%-5level] [%c{1.1.1.*}]: %message%n")
            .build();

        final var rollOverStrategy = DefaultRolloverStrategy
            .newBuilder()
            .withMax("3")
            .withFileIndex("min")
            .withConfig(configuration)
            .withCompressionLevelStr("5")
            .build();

        final String logFileBaseName = path.toAbsolutePath().resolve("aa").toString();

        final RollingFileAppender appender = RollingFileAppender
            .newBuilder()
            .withStrategy(rollOverStrategy)
            .setConfiguration(configuration)
            .setName(LOGGER_NAME)
            .withAppend(true)
            .withFilePattern(logFileBaseName + ".%i.log.gz")
            .withFileName(logFileBaseName + ".log")
            .withPolicy(SizeBasedTriggeringPolicy.createPolicy("10MB"))
            .setLayout(pattern)
            .setFilter(levelFilter)
            .build();
        appender.start();

        // Create a custom filter to log only messages from AnimatedArchitecture and its utilities.
        final Filter customFilter = new AbstractFilter()
        {
            @Override
            public Result filter(LogEvent event)
            {
                final String loggerName = event.getLoggerName();
                if (loggerName != null &&
                    (loggerName.startsWith(MAIN_PACKAGE) || loggerName.startsWith(UTIL_PACKAGE)))
                    return Result.ACCEPT;
                return Result.DENY;
            }
        };

        final LoggerConfig loggerConfig = LoggerConfig
            .newBuilder()
            .withAdditivity(true)
            .withConfig(configuration)
            .withtFilter(levelFilter)
            .withLevel(Level.ALL)
            .withLoggerName(LOGGER_NAME)
            .build();

        configuration.getCustomLevels();

        loggerConfig.addFilter(customFilter);
        loggerConfig.addAppender(appender, Level.ALL, levelFilter);
        configuration.addLogger(LOGGER_NAME, loggerConfig);

        loggerContext.updateLoggers();
    }
}
