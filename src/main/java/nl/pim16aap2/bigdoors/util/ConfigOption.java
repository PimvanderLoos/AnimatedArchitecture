package nl.pim16aap2.bigdoors.util;

import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;

import nl.pim16aap2.bigdoors.BigDoors;

/* This class represent a configuration option.
 * The general format is: the comment followed by
 * "<optionName>: <value>" on a new line for most options.
 * For Lists, every option appears on a new line after a '-'.
 */

public class ConfigOption<V>
{
    private final BigDoors plugin;
    private final FileConfiguration config;
    private final String optionName;
    private V value;
    private final V defaultValue;
    private final String[] comment;

    public ConfigOption(BigDoors plugin, FileConfiguration config, String optionName, V defaultValue, String[] comment)
    {
        this.plugin = plugin;
        this.config = config;
        this.optionName = optionName;
        this.defaultValue = defaultValue;
        this.comment = comment;
        setValue();
    }

    @SuppressWarnings("unchecked")
    private void setValue()
    {
        try
        {
            value = (V) config.get(optionName, defaultValue);
        }
        catch (Exception e)
        {
            plugin.getMyLogger().logMessage("Failed to read config value of: \"" + optionName + "\"! Using default value instead!", true, false);
            plugin.getMyLogger().logMessage(Util.exceptionToString(e), false, false);
            value = defaultValue;
        }
    }

    public V getValue()
    {
        return value;
    }

    public String[] getComment()
    {
        return comment;
    }

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
                builder.append("  - " + ((List<?>) value).get(index) + (index == listSize - 1 ? "" : "\n")); // Don't print newline at the end
            string += builder.toString();
        }
        else
            string +=  value.toString();

        return string;
    }
}
