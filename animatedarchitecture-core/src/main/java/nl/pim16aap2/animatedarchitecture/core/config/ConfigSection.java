package nl.pim16aap2.animatedarchitecture.core.config;

import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.function.Consumer;

/**
 * Represents a section in the configuration.
 * <p>
 * This interface defines the contract for configuration sections, which are used to define specific parts of the
 * configuration with their own initial transform actions.
 */
public abstract class ConfigSection<T extends IConfigSectionResult>
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
    public abstract CommentedConfigurationNode buildInitialLimitsNode()
        throws SerializationException;

    /**
     * Gets the result of this configuration section.
     *
     * @param sectionNode
     *     The configuration node for this section.
     * @param silent
     *     When true, the method will not log any informational/status messages when parsing the results.
     * @return the result of this configuration section
     *
     * @throws SerializationException
     *     If an error occurs during serialization.
     */
    protected abstract T getResult(ConfigurationNode sectionNode, boolean silent)
        throws SerializationException;

    /**
     * Gets the consumer that will handle the result of this configuration section.
     *
     * @return the consumer that will handle the result, or null if no consumer is defined.
     */
    protected abstract @Nullable Consumer<T> getResultConsumer();

    /**
     * Applies the results of this section.
     *
     * @param root
     *     The root configuration node to read the data from.
     * @param silent
     *     When true, the method will not log any informational/status messages when parsing the results.
     */
    public final void applyResults(ConfigurationNode root, boolean silent)
        throws SerializationException
    {
        final Consumer<T> consumer = getResultConsumer();
        if (consumer == null)
            return;

        final ConfigurationNode sectionNode = root.node(getSectionTitle());
        final T result = getResult(sectionNode, silent);
        consumer.accept(result);
    }

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
    public CommentedConfigurationNode getSection(CommentedConfigurationNode root)
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
    public void populateDynamicData(CommentedConfigurationNode root)
    {
    }

    /**
     * Returns the title of the configuration section.
     *
     * @return the title of the section
     */
    public abstract String getSectionTitle();
}
