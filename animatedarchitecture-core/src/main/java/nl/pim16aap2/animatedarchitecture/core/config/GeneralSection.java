package nl.pim16aap2.animatedarchitecture.core.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

/**
 * Represents a section in the configuration file that governs the general settings.
 *
 * @param <T>
 *     the type of result this section produces.
 */
public abstract class GeneralSection<T extends IConfigSectionResult> extends ConfigSection<T>
{
    public static final String SECTION_TITLE = "general";
    public static final String SECTION_COMMENT = """
        General settings for the Animated Architecture plugin.
        """;

    @Override
    public CommentedConfigurationNode buildInitialLimitsNode()
        throws SerializationException
    {
        return CommentedConfigurationNode.root()
            .comment(SECTION_COMMENT);
    }

    @Override
    public String getSectionTitle()
    {
        return SECTION_TITLE;
    }
}
