package nl.pim16aap2.animatedarchitecture.core.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

/**
 * Represents a section in the configuration for protection hooks.
 * <p>
 * This section is used to configure hooks for various protection plugins, allowing the plugin to interact with them for
 * structure protection and other related features.
 *
 * @param <T>
 *     the type of result this section produces.
 */
public abstract class ProtectionHooksSection<T extends IConfigSectionResult> extends ConfigSection<T>
{
    public static final String SECTION_TITLE = "protection_hooks";

    @Override
    public CommentedConfigurationNode buildInitialLimitsNode()
        throws SerializationException
    {
        return CommentedConfigurationNode.root()
            .comment("""
                Enable or disable compatibility hooks for certain plugins.
                
                If the plugins aren't installed, these options do nothing.
                
                When enabled, structures cannot be toggled or created in areas not owned by the owner of that structure.
                """)
            .set("[]");
    }

    @Override
    public abstract void populateDynamicData(CommentedConfigurationNode root);

    @Override
    public String getSectionTitle()
    {
        return SECTION_TITLE;
    }
}
