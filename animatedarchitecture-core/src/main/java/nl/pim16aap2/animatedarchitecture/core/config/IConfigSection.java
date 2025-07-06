package nl.pim16aap2.animatedarchitecture.core.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

/**
 * Represents a section in the configuration.
 * <p>
 * This interface defines the contract for configuration sections, which are used to define specific parts of the
 * configuration with their own initial transform actions.
 */
public interface IConfigSection
{
    /**
     * Builds the initial limits node for this configuration section.
     * <p>
     * This method is responsible for creating the initial configuration node for this section, including any default
     * values and comments.
     *
     * @return the initial limits node for this section
     *
     * @throws SerializationException
     *     if an error occurs during serialization
     */
    CommentedConfigurationNode buildInitialLimitsNode()
        throws SerializationException;

    /**
     * Gets the subsection for this configuration section.
     *
     * @param root
     *     The root configuration node to get the subsection from.
     * @return the subsection node for this configuration section
     *
     * @throws IllegalStateException
     *     if the section does not exist.
     */
    default CommentedConfigurationNode getSection(CommentedConfigurationNode root)
    {
        final CommentedConfigurationNode node = root.node(getSectionTitle());
        if (node.virtual())
            throw new IllegalStateException("Configuration section '" + getSectionTitle() + "' does not exist.");
        return node;
    }

    /**
     * Populates the dynamic data for this configuration section.
     * <p>
     * This method will not overwrite existing data in the node; it will only add new data if any is missing.
     *
     * @param root
     *     The root configuration node to populate with dynamic data.
     */
    default void populateDynamicData(CommentedConfigurationNode root)
    {
    }

    /**
     * Returns the title of the configuration section.
     *
     * @return the title of the section
     */
    String getSectionTitle();
}
