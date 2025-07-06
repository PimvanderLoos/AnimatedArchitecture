package nl.pim16aap2.animatedarchitecture.core.config;

import org.jspecify.annotations.Nullable;

/**
 * Represents a configuration option for a structure type.
 *
 * @param name
 *     The name of the option, used as the key in the configuration.
 * @param defaultValue
 *     The default value for this option, can be null.
 * @param type
 *     The type of the value for this option, used for validation and conversion.
 * @param comment
 *     The comment for this option, can be null.
 */
public record StructureTypeConfigurationOption<T>(
    String name,
    T defaultValue,
    Class<T> type,
    @Nullable
    String comment
)
{
    /**
     * The configuration option for the speed multiplier of a structure type.
     */
    public static StructureTypeConfigurationOption<Double> ANIMATION_SPEED_MULTIPLIER =
        new StructureTypeConfigurationOption<>("animation_speed_multiplier", 1.0, Double.class);

    /**
     * The configuration option for the price formula of a structure type.
     */
    public static StructureTypeConfigurationOption<String> PRICE_FORMULA =
        new StructureTypeConfigurationOption<>("price_formula", "0", String.class);

    /**
     * Creates a new {@link StructureTypeConfigurationOption} without a comment.
     *
     * @param name
     *     The name of the option, used as the key in the configuration.
     * @param defaultValue
     *     The default value for this option.
     * @param type
     *     The type of the value for this option, used for validation and conversion.
     */
    public StructureTypeConfigurationOption(String name, T defaultValue, Class<T> type)
    {
        this(name, defaultValue, type, null);
    }
}
