package nl.pim16aap2.util.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.nio.file.Path;

import static nl.pim16aap2.util.logging.floggerbackend.Log4j2LogEventUtil.toLog4jLevel;

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
        final Level level = Level.ALL;

        final LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        final Configuration configuration = loggerContext.getConfiguration();

        final var pattern = PatternLayout
            .newBuilder()
            .withPattern("[%date{ISO8601}] [%thread/%level] [%c{1.1.1.*}]: %message%n")
            .build();

        final FileAppender appender = FileAppender
            .newBuilder()
            .setName(LOGGER_NAME)
            .withAppend(true)
            .withFileName(path.toAbsolutePath().toString())
            .setLayout(pattern)
            .setConfiguration(configuration)
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
            .withLoggerName(LOGGER_NAME)
            .withLevel(level)
            .build();

        loggerConfig.addFilter(customFilter);
        loggerConfig.addAppender(appender, level, levelFilter);
        configuration.addLogger(LOGGER_NAME, loggerConfig);

        loggerContext.updateLoggers();
    }
}
