package nl.pim16aap2.bigdoors.spigot.config;

import nl.pim16aap2.bigdoors.util.PLogger;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents an option in a config file.
 *
 * @author Pim
 */
public final class ConfigEntry<V>
{
    private final PLogger logger;
    private final FileConfiguration config;
    private final String optionName;
    private final V defaultValue;
    @Nullable
    private final String[] comment;
    @Nullable
    private final ConfigEntry.TestValue<V> verifyValue;
    private V value;

    /**
     * ConfigEntry Constructor.
     *
     * @param logger       The logger to use for exception reporting.
     * @param config       The config file to read from.
     * @param optionName   The name of this option as used in the config file.
     * @param defaultValue The default value of this option.
     * @param comment      The comment that will preceed this option.
     * @param verifyValue  Function to use to verify the validity of a value and change it if necessary.
     */
    ConfigEntry(final @NotNull PLogger logger, final @NotNull FileConfiguration config,
                final @NotNull String optionName, final @NotNull V defaultValue, final @Nullable String[] comment,
                final @Nullable ConfigEntry.TestValue<V> verifyValue)
    {
        this.logger = logger;
        this.config = config;
        this.optionName = optionName;
        this.defaultValue = defaultValue;
        this.comment = comment;
        this.verifyValue = verifyValue;
        setValue();
    }

    /**
     * ConfigEntry Constructor.
     *
     * @param logger       The logger to use for exception reporting.
     * @param config       The config file to read from.
     * @param optionName   The name of this option as used in the config file.
     * @param defaultValue The default value of this option.
     * @param comment      The comment that will preceed this option.
     */
    ConfigEntry(final @NotNull PLogger logger, final @NotNull FileConfiguration config,
                final @NotNull String optionName, final @NotNull V defaultValue, final @Nullable String[] comment)
    {
        this(logger, config, optionName, defaultValue, comment, null);
    }

    /**
     * Read the value of this config option from the config. If it fails, the  default value is used instead. If it is
     * available, the {@link #verifyValue} method is used to modify the value if it is invalid.
     */
    @SuppressWarnings("unchecked")
    private void setValue()
    {
        try
        {
            value = (V) config.get(optionName, defaultValue);
            if (value == null)
                value = defaultValue;
        }
        catch (Exception e)
        {
            logger.logException(e,
                                "Failed to read config value of: \"" + optionName + "\"! Using default value instead!");
            value = defaultValue;
        }
        if (verifyValue != null)
            value = verifyValue.test(value);
    }

    /**
     * Get the value of this config option.
     *
     * @return The value of the config option.
     */
    public V getValue()
    {
        return value;
    }

    /**
     * Get the comment of this config option.
     *
     * @return The comment of the config option.
     */
    public String[] getComment()
    {
        return comment;
    }

    /**
     * Convert the comment, name and value(s) of this config option into a string that can be used for writing the
     * config.
     *
     * @return The config option formatted for printing in the config file
     */
    @Override
    public String toString()
    {
        String string = "";

        // Print the comments, if there are any.
        if (comment != null)
            for (String comLine : comment)
                // Prefix every line by a comment-sign (#).
                string += "# " + comLine + "\n";

        string += optionName + ": ";
        if (value.getClass().isAssignableFrom(String.class))
            string += "\'" + value.toString() + "\'";
        else if (value instanceof List<?>)
        {
            StringBuilder builder = new StringBuilder();
            builder.append("\n");
            int listSize = ((List<?>) value).size();
            for (int index = 0; index < listSize; ++index)
                // Don't print newline at the end
                builder.append("  - ").append(((List<?>) value).get(index)).append(index == listSize - 1 ? "" : "\n");
            string += builder.toString();
        }
        else
            string += value.toString();
        return string;
    }

    /**
     * Interface that can be used to verify config values.
     *
     * @param <T> The type of the value.
     * @author Pim
     */
    interface TestValue<T>
    {
        /**
         * Checks if a given value is valid. If it is, it returns that value. If it isn't, it is changed so that it is
         * valid.
         * <p>
         * For example to check if a value doesn't exceed a certain threshold.
         *
         * @param value The value to check.
         * @return The value if it was valid, otherwise the value made valid.
         */
        @NotNull
        T test(final @NotNull T value);
    }
}
