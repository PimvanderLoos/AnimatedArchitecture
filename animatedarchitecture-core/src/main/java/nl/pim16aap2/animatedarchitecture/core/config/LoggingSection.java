package nl.pim16aap2.animatedarchitecture.core.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.logging.Level;

/**
 * Represents a configuration section for logging settings in Animated Architecture.
 * <p>
 * This section allows the user to configure console logging, log level, and debug mode.
 *
 * @param <T>
 *     the type of result this section produces.
 */
public abstract class LoggingSection<T extends IConfigSectionResult> extends ConfigSection<T>
{
    public static final String SECTION_TITLE = "logging";

    public static final String PATH_LOG_LEVEL = "log_level";
    public static final String PATH_DEBUG = "debug";

    public static final Level DEFAULT_LOG_LEVEL = Level.INFO;
    public static final boolean DEFAULT_DEBUG = false;

    @Override
    public CommentedConfigurationNode buildInitialLimitsNode()
        throws SerializationException
    {
        return CommentedConfigurationNode.root()
            .comment("""
                Settings related to logging.
                """)
            .act(node ->
            {
                addInitialLogLevel(node.node(PATH_LOG_LEVEL));
                addInitialDebug(node.node(PATH_DEBUG));
            });
    }

    private void addInitialLogLevel(CommentedConfigurationNode node)
        throws SerializationException
    {
        node.set(DEFAULT_LOG_LEVEL.getName())
            .comment("""
                The log level to use. Note that levels lower than INFO aren't shown in the console by default,
                regardless of this setting. They are still written to this plugin's log file, though.
                
                Supported levels are:
                    OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL.
                
                This will default to INFO in case an invalid option is provided.
                """);
    }

    private void addInitialDebug(CommentedConfigurationNode node)
        throws SerializationException
    {
        node.set(DEFAULT_DEBUG)
            .comment("Don't use this. Just leave it on false.");
    }

    @Override
    public String getSectionTitle()
    {
        return SECTION_TITLE;
    }
}
