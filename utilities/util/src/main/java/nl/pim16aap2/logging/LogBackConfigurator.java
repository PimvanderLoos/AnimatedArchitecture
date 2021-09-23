package nl.pim16aap2.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.FileAppender;
import lombok.Setter;
import lombok.experimental.Accessors;
import nl.pim16aap2.util.LazyInit;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a configuration tool for the LogBack backend used by Flogger (via slf4j).
 *
 * @author Pim
 */
public class LogBackConfigurator
{
    private static final Map<java.util.logging.Level, Level> LEVEL_MAPPER = new HashMap<>();

    static
    {
        LEVEL_MAPPER.put(java.util.logging.Level.OFF, Level.OFF);
        LEVEL_MAPPER.put(java.util.logging.Level.SEVERE, Level.ERROR);
        LEVEL_MAPPER.put(java.util.logging.Level.WARNING, Level.WARN);
        LEVEL_MAPPER.put(java.util.logging.Level.INFO, Level.INFO);
        LEVEL_MAPPER.put(java.util.logging.Level.CONFIG, Level.DEBUG);
        LEVEL_MAPPER.put(java.util.logging.Level.FINE, Level.DEBUG);
        LEVEL_MAPPER.put(java.util.logging.Level.FINER, Level.DEBUG);
        LEVEL_MAPPER.put(java.util.logging.Level.FINEST, Level.TRACE);
        LEVEL_MAPPER.put(java.util.logging.Level.ALL, Level.ALL);
    }

    private static final LazyInit<Logger> LOGGER =
        new LazyInit<>(() -> LoggerFactory.getLogger(LogBackConfigurator.class));

    private final List<AppenderSpecification> appenderList = new ArrayList<>();

    private @Nullable Level level = null;

    @Setter
    @Accessors(chain = true)
    private @Nullable Path logFile = null;

    /**
     * Sets the log level of the root logger.
     *
     * @param level
     *     The log level.
     * @return The current configurator.
     */
    public LogBackConfigurator setLevel(Level level)
    {
        this.level = level;
        return this;
    }

    /**
     * Sets the log level of the root logger.
     *
     * @param level
     *     The log level.
     * @return The current configurator.
     */
    public LogBackConfigurator setLevel(java.util.logging.Level level)
    {
        return setLevel(LEVEL_MAPPER.getOrDefault(level, Level.INFO));
    }

    /**
     * Adds an {@link AppenderSpecification} to the configuration.
     *
     * @param appenderSpecification
     *     The appender to add.
     * @return The current configurator.
     */
    public LogBackConfigurator addAppender(AppenderSpecification appenderSpecification)
    {
        appenderList.add(appenderSpecification);
        return this;
    }

    /**
     * Adds an appender to the logger.
     *
     * @param name
     *     The name of the appender.
     * @param clz
     *     The fully specified name of the appender class as returned by {@link Class#getName()}. E.g.
     *     "my.package.logging.MyAppender".
     * @param pattern
     *     The pattern to use for the formatting of the messages logged by the appender. E.g. "%-5level [%thread]:
     *     %message%n". See <a href="http://logback.qos.ch/manual/layouts.html">LogBack Chapter 6: Layouts</a>.
     * @return The current configurator.
     */
    public LogBackConfigurator addAppender(String name, String clz, @Nullable String pattern)
    {
        return addAppender(new AppenderSpecification(name, clz, pattern));
    }

    /**
     * See {@link #addAppender(String, String, String)}.
     */
    public LogBackConfigurator addAppender(String name, String clz)
    {
        addAppender(name, clz, null);
        return this;
    }

    /**
     * Applies the config in its current state to the logger factory.
     *
     * @return The current configurator.
     */
    public LogBackConfigurator apply()
    {
        @Nullable String config = null;
        try
        {
            final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            final JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            context.reset();

            config = buildConfig();
            configurator.doConfigure(new ByteArrayInputStream(config.getBytes(StandardCharsets.UTF_8)));
            configurator.setContext(context);
            logConfig(config);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to configure LogBack from settings:\n" + config, e);
        }
        return this;
    }

    private void logConfig(String config)
    {
        if (LOGGER.get().isTraceEnabled())
            LOGGER.get().trace("LogBack configuration: \n" + config);
    }

    private String buildConfig()
    {
        final StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<configuration>\n");

        final StringBuilder appenderRefs = new StringBuilder();
        for (final AppenderSpecification appender : appenderList)
        {
            sb.append(appender);
            appenderRefs.append(getAppenderRef(appender.name()));
        }

        if (logFile != null && isFileLoggingEnabled())
        {
            sb.append(getLogFileAppenderConfig(logFile));
            appenderRefs.append(getAppenderRef("FileLogger"));
        }
        return sb.append("\t<root level=\"").append(level).append("\">\n")
                 .append(appenderRefs)
                 .append("\t</root>\n</configuration>").toString();
    }

    /**
     * Gets the configuration portion of the logFile appender.
     *
     * @param file
     *     The logFile to use.
     * @return The configuration of the logFile appender.
     */
    private String getLogFileAppenderConfig(Path file)
    {
        return
            """
                <appender name="FileLogger" class="${FILE_APPENDER_CLASS}">
                    <file>${LOG_FILE_NAME}</file>
                    <append>true</append>
                    <encoder>
                        <pattern>[%date{ISO8601}] [%thread/%level]: %logger: %message%n</pattern>
                    </encoder>
                </appender>
            """.replace("${LOG_FILE_NAME}", file.toAbsolutePath().toString())
               // Use placeholder for class name to ensure that it works nicely with relocation.
               .replace("${FILE_APPENDER_CLASS}", FileAppender.class.getName());
    }

    /**
     * Checks if file logging should be enabled.
     * <p>
     * File logging should be enabled when {@link #logFile} is not null and can be used for logging. See {@link
     * #ensureLogFileExists(Path)}.
     *
     * @return True if file logging should be enabled.
     */
    private boolean isFileLoggingEnabled()
    {
        if (logFile == null)
            return false;
        try
        {
            ensureLogFileExists(logFile);
        }
        catch (IOException | UnsupportedOperationException e)
        {
            throw new RuntimeException(e);
        }
        return true;
    }

    /**
     * Ensures that the provided file can be used for logging. This means that the file exists (or can be created) and
     * that the file is writable (or can be made so).
     *
     * @param file
     *     The file to check.
     * @throws IOException
     *     If an issue occurred creating the file or updating it writable status.
     */
    private static void ensureLogFileExists(Path file)
        throws IOException
    {
        if (!Files.isRegularFile(file))
            Files.createFile(file);

        if (!Files.isWritable(file) && !file.toFile().setWritable(true))
            throw new IOException("Failed to make file \"" + file + "\" writable! Is it a directory?");
    }

    private static String getAppenderRef(String name)
    {
        return String.format("\t\t<appender-ref ref=\"%s\" />\n", name);
    }

    public record AppenderSpecification(String name, String clz, @Nullable String pattern)
    {
        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append(String.format("\t<appender name=\"%s\" class=\"%s\"", name, clz));
            if (pattern == null)
                return sb.append(" />\n").toString();

            return sb
                .append(String.format(">\n\t\t<encoder>\n\t\t\t<pattern>%s</pattern>\n\t\t</encoder>\n\t</appender>\n",
                                      pattern))
                .toString();
        }
    }
}
