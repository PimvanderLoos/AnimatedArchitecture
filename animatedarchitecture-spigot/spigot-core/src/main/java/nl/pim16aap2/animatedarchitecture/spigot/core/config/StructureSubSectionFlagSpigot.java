package nl.pim16aap2.animatedarchitecture.spigot.core.config;

import nl.pim16aap2.animatedarchitecture.core.config.IStructureSubSectionFlag;
import nl.pim16aap2.animatedarchitecture.core.config.StructureTypeConfigurationOption;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;

import java.util.List;

/**
 * Spigot-specific implementation of {@link IStructureSubSectionFlag}.
 * <p>
 * This class is used to define a subsection for a specific flag structure type in the configuration.
 */
public class StructureSubSectionFlagSpigot extends StructureSubSectionSpigot implements IStructureSubSectionFlag
{
    private static final String DEFAULT_MOVEMENT_FORMULA =
        "min(0.07 * radius, 3) * sin(radius / 1.7 + height / 12 + counter / 12)";

    public static final StructureTypeConfigurationOption<String> OPTION_MOVEMENT_FORMULA =
        new StructureTypeConfigurationOption<>(
            "movement_formula",
            DEFAULT_MOVEMENT_FORMULA,
            String.class,
            """
                The movement formula of the blocks for flags. The formula is evaluated for each block
                for each step in the animation.
                
                You can find a list of supported operators in the formula here:
                https://github.com/PimvanderLoos/JCalculator
                
                The formula can use the following variables:
                  'radius':  The distance of the block to the pole it is connected to.
                  'counter': The number of steps that have passed in the animation.
                  'length':  The total length of the flag.
                  'height':  The height of the block for which the formula is used. The bottom row has a height of 0.
                
                The return value of the formula is the horizontal displacement of a single block in the flag.
                
                Default: %s
                """.formatted(DEFAULT_MOVEMENT_FORMULA)
        );

    /**
     * Creates a new {@link IStructureSubSectionFlag} with the given structure type.
     *
     * @param flagType
     *     The structure type this subsection is for. This should be a flag structure type.
     */
    public StructureSubSectionFlagSpigot(StructureType flagType)
    {
        super(flagType);

        if (!STRUCTURE_TYPE_KEY.equals(flagType.getFullKey()))
        {
            throw new IllegalArgumentException("The structure type must be a flag type: " + flagType.getFullKey());
        }
    }

    @Override
    protected void appendConfigurationOptions(List<StructureTypeConfigurationOption<?>> options)
    {
        super.appendConfigurationOptions(options);
        options.add(OPTION_MOVEMENT_FORMULA);
    }
}
