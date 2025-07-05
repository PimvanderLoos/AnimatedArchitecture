package nl.pim16aap2.animatedarchitecture.core.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.transformation.TransformAction;

/**
 * Represents a section in the configuration.
 * <p>
 * This interface defines the contract for configuration sections, which are used to define specific parts of the
 * configuration with their own initial transform actions.
 */
public interface IConfigSection
{
    CommentedConfigurationNode buildInitialLimitsNode()
        throws SerializationException;

    String getSectionTitle();

    /**
     * Gets the initial transform action for this section.
     * <p>
     * This action should be used to create the initial state of the section.
     *
     * @return The initial transform action for this section.
     */
    default TransformAction getInitialTransform()
    {
        System.out.println("Getting initial transform for section: " + getSectionTitle());
        return (path, value) ->
        {
            System.out.println("Applying initial transform for section: " + getSectionTitle());
            value.node(getSectionTitle()).set(buildInitialLimitsNode());
            return null;
        };
    }
}
