package nl.pim16aap2.animatedarchitecture.core.config;

import org.jspecify.annotations.Nullable;

/**
 * Represents a configuration option for a structure type.
 *
 * @param name
 *     The name of the option, used as the key in the configuration.
 * @param defaultValue
 *     The default value for this option, can be null.
 * @param comment
 *     The comment for this option, can be null.
 */
public record StructureTypeConfigurationOption(
    String name,
    Object defaultValue,
    @Nullable
    String comment
)
{
    /**
     * The configuration option for the speed multiplier of a structure type.
     */
    public static StructureTypeConfigurationOption SPEED_MULTIPLIER =
        new StructureTypeConfigurationOption("speed_multiplier", 1.0);

    /**
     * The configuration option for the price formula of a structure type.
     */
    public static StructureTypeConfigurationOption PRICE =
        new StructureTypeConfigurationOption("price", "0");

    /**
     * Creates a new {@link StructureTypeConfigurationOption} without a comment.
     *
     * @param name
     *     The name of the option, used as the key in the configuration.
     * @param defaultValue
     *     The default value for this option.
     */
    public StructureTypeConfigurationOption(String name, Object defaultValue)
    {
        this(name, defaultValue, null);
    }
}
