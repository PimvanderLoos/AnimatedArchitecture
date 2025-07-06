package nl.pim16aap2.animatedarchitecture.core.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

/**
 * Represents a section in the configuration for protection hooks.
 * <p>
 * This section is used to configure hooks for various protection plugins, allowing the plugin to interact with them for
 * structure protection and other related features.
 */
public abstract class ProtectionHooksSection implements IConfigSection
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

    public abstract void populateProtectionHooks(CommentedConfigurationNode root)
        throws SerializationException;

    /**
     * Gets the subsection for the protection hooks section in the configuration.
     * <p>
     * If the subsection does not exist, it will be created.
     *
     * @param root
     *     The root configuration node to get the protection hooks subsection from.
     * @return The protection hooks subsection node.
     *
     * @throws SerializationException
     *     If there is an error during serialization.
     */
    protected CommentedConfigurationNode getProtectionHooksSubSection(CommentedConfigurationNode root)
        throws SerializationException
    {
        final CommentedConfigurationNode node = root.node(getSectionTitle());
        if (node.virtual())
            return buildInitialLimitsNode();
        return node;
    }

    @Override
    public String getSectionTitle()
    {
        return SECTION_TITLE;
    }
}
