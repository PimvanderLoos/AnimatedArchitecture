package nl.pim16aap2.animatedarchitecture.core.config;

import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;

import java.util.List;

/**
 * Represents a subsection of {@link StructuresSection} in the configuration.
 * <p>
 * This interface is used to define a subsection for a specific structure type.
 */
public interface IStructureSubSection
{
    /**
     * Returns the structure type this subsection is for.
     *
     * @return the structure type this subsection is associated with.
     */
    StructureType getStructureType();

    /**
     * Gets the configuration options for this structure type.
     *
     * @return a list of configuration options for this structure type.
     */
    List<StructureTypeConfigurationOption> getConfigurationOptions();

    /**
     * Returns the title of the section.
     * <p>
     * This is typically the full key of the structure type.
     *
     * @return the title of the section.
     */
    default String getSectionTitle()
    {
        return getStructureType().getFullKey();
    }
}
