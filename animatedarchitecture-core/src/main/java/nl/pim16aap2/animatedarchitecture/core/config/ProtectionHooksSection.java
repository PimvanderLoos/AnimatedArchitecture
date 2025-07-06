package nl.pim16aap2.animatedarchitecture.core.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

/**
 * Represents a section in the configuration for protection hooks.
 * <p>
 * This section is used to configure hooks for various protection plugins, allowing the plugin to interact with them for
 * structure protection and other related features.
 */
public class ProtectionHooksSection implements IConfigSection
{
    public static final String SECTION_TITLE = "protection_hooks";

    @Override
    public CommentedConfigurationNode buildInitialLimitsNode()
        throws SerializationException
    {
        throw new UnsupportedOperationException("Protection hooks section is not implemented yet.");
    }

    @Override
    public String getSectionTitle()
    {
        return SECTION_TITLE;
    }
}
