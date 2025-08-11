package nl.pim16aap2.animatedarchitecture.spigot.core.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.config.IConfigSectionResult;
import nl.pim16aap2.animatedarchitecture.core.config.LoggingSection;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Represents a section in the configuration for logging settings specific to the Spigot implementation.
 * <p>
 * This section is used to configure logging behavior, such as enabling or disabling specific loggers.
 */
@AllArgsConstructor
public class LoggingSectionSpigot extends LoggingSection<LoggingSectionSpigot.Result>
{
    @Getter
    private final @Nullable Consumer<Result> resultConsumer;

    @Override
    protected Result getResult(ConfigurationNode sectionNode, boolean silent)
        throws SerializationException
    {
        return new Result(
            getLevel(sectionNode),
            getDebug(sectionNode)
        );
    }

    private Level getLevel(ConfigurationNode sectionNode)
        throws SerializationException
    {
        final String logLevelName = sectionNode.node(PATH_LOG_LEVEL).get(String.class);
        return Objects.requireNonNullElse(Util.parseLogLevelStrict(logLevelName), DEFAULT_LOG_LEVEL);
    }

    private boolean getDebug(ConfigurationNode sectionNode)
    {
        return sectionNode.node(PATH_DEBUG).getBoolean(DEFAULT_DEBUG);
    }

    /**
     * Represents the result of the logging configuration section.
     *
     * @param logLevel
     *     the logging level to be used.
     * @param debug
     *     Some debug information that should not be used in production.
     */
    public record Result(
        Level logLevel,
        boolean debug
    ) implements IConfigSectionResult
    {
        /**
         * The default result used when no data is available.
         */
        public static final Result DEFAULT = new Result(
            LoggingSection.DEFAULT_LOG_LEVEL,
            LoggingSection.DEFAULT_DEBUG
        );
    }
}
