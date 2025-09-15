package nl.pim16aap2.animatedarchitecture.core.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

/**
 * Represents a section in the configuration file that governs the redstone settings.
 *
 * @param <T>
 *     the type of result this section produces.
 */
public abstract class RedstoneSection<T extends IConfigSectionResult> extends ConfigSection<T>
{
    public static final String SECTION_TITLE = "redstone";

    public static final String PATH_ALLOW_REDSTONE = "allow_redstone";

    public static final boolean DEFAULT_ALLOW_REDSTONE = true;

    @Override
    public CommentedConfigurationNode buildInitialLimitsNode()
        throws SerializationException
    {
        return CommentedConfigurationNode.root()
            .comment("""
                Settings related to redstone interactions.
                """)
            .act(node -> addInitialAllowRedstone(node.node(PATH_ALLOW_REDSTONE)));
    }

    private void addInitialAllowRedstone(CommentedConfigurationNode node)
        throws SerializationException
    {
        node.set(DEFAULT_ALLOW_REDSTONE)
            .comment("""
                Whether structures should respond to redstone signals.
                
                When enabled, structures will be animated when the redstone signal of their
                powerblock changes. The specific action depends on the structure type and settings.
                
                When disabled, structures will ignore all redstone signals.
                
                Default: %b
                """.formatted(DEFAULT_ALLOW_REDSTONE));
    }

    @Override
    public String getSectionTitle()
    {
        return SECTION_TITLE;
    }
}
