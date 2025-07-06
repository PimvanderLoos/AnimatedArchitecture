package nl.pim16aap2.animatedarchitecture.core.config;

/**
 * Represents a subsection of {@link StructuresSection} in the configuration for a flag structure type.
 * <p>
 * This class is used to define a subsection for a specific structure type that is a flag.
 * <p>
 * The flag structure type has special handling because it has an additional configuration option:
 * {@code "movement_formula"}.
 */
public interface IStructureSubSectionFlag extends IStructureSubSection
{
    /**
     * The key used to identify the flag structure type in the configuration.
     */
    String STRUCTURE_TYPE_KEY = "animatedarchitecture:flag";
}
