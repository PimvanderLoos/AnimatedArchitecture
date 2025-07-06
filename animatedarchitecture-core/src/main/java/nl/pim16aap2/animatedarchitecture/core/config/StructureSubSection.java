package nl.pim16aap2.animatedarchitecture.core.config;

import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a subsection of {@link StructuresSection} in the configuration.
 * <p>
 * This class is used to define a subsection for a specific structure type.
 */
public class StructureSubSection implements IStructureSubSection
{
    public static final List<StructureTypeConfigurationOption<?>> DEFAULT_CONFIGURATION_OPTIONS = List.of(
        StructureTypeConfigurationOption.SPEED_MULTIPLIER,
        StructureTypeConfigurationOption.PRICE
    );

    @Getter
    private final StructureType structureType;

    public StructureSubSection(StructureType structureType)
    {
        this.structureType = structureType;
    }

    /**
     * Appends additional configuration options to the default list.
     *
     * @param options
     *     The list of configuration options to append to.
     */
    protected void appendConfigurationOptions(
        @SuppressWarnings("unused") List<StructureTypeConfigurationOption<?>> options)
    {
    }

    @Override
    public final List<StructureTypeConfigurationOption<?>> getConfigurationOptions()
    {
        final var options = new ArrayList<>(DEFAULT_CONFIGURATION_OPTIONS);
        appendConfigurationOptions(options);
        return Collections.unmodifiableList(options);
    }
}
