package nl.pim16aap2.animatedarchitecture.core.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public class GeneralSection implements IConfigSection
{
    public static final String SECTION_TITLE = "general";

    public static final String PATH_ALLOW_REDSTONE = "allow_redstone";

    public static final boolean DEFAULT_ALLOW_REDSTONE = true;

    @Override
    public CommentedConfigurationNode buildInitialLimitsNode()
        throws SerializationException
    {
        return CommentedConfigurationNode.root()
            .comment("""
                General settings for the Animated Architecture plugin.
                """)
            .act(node ->
                addInitialAllowRedstone(node.node(PATH_ALLOW_REDSTONE)));
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
                """);
    }

    @Override
    public String getSectionTitle()
    {
        return SECTION_TITLE;
    }
}
