package nl.pim16aap2.bigDoors.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import nl.pim16aap2.bigDoors.BigDoors;

public class Messages
{
    private HashMap<String, String> messageMap = new HashMap<>();
    private final BigDoors plugin;
    private String locale;
    private File textFile;

    public Messages(BigDoors plugin)
    {
        this.plugin = plugin;
        locale = plugin.getLocale();
        textFile = new File(plugin.getDataFolder(), locale + ".txt");
        readFile();
    }

    private void writeDefaultFile()
    {
        File defaultFile = new File(plugin.getDataFolder(), "en_US.txt");
        if (!defaultFile.setWritable(true))
            plugin.getMyLogger().myLogger(Level.SEVERE, "Failed to make file \"" + defaultFile + "\" writable!");

        // Load the default en_US from the resources folder.
        plugin.saveResource("en_US.txt", true);
        defaultFile.setWritable(false);
    }

    // Read locale file.
    private void readFile()
    {
        writeDefaultFile();

        try (BufferedReader br = new BufferedReader(new FileReader(textFile)))
        {
            String sCurrentLine;

            while ((sCurrentLine = br.readLine()) != null)
            {
                // Ignore comments and empty lines.
                if (sCurrentLine.startsWith("#") || sCurrentLine.isEmpty())
                    continue;
                String[] parts = sCurrentLine.split("=", 2);
                String key = parts[0];
                String value;

                // If there is no equals sign in the line, there will only be 1 part.
                // In all other cases there will be 2 parts (empty lines are skipped).
                if (parts.length == 1)
                {
                    plugin.getMyLogger().myLogger(Level.WARNING,
                                                  "Invalid syntax for translation: \"" + sCurrentLine + "\"");
                    value = "Invalid translation";
                }
                else
                    value = parts[1].replaceAll("&((?i)[0-9a-fk-or])", "\u00A7$1");

                String[] newLineSplitter = value.split("\\\\n");
                String values = newLineSplitter[0];

                for (int idx = 1; idx < newLineSplitter.length; ++idx)
                    values += "\n" + newLineSplitter[idx];

                messageMap.put(key, values);
            }
            br.close();
        }
        catch (FileNotFoundException e)
        {
            plugin.getMyLogger().myLogger(Level.SEVERE, "Locale file " + locale + ".txt does not exist!");
        }
        catch (IOException e)
        {
            plugin.getMyLogger().myLogger(Level.SEVERE, "Could not read locale file! (" + locale + ".txt)");
            e.printStackTrace();
        }
    }

    // Get a string from a key. Returns "null" if null.
    public String getString(String key)
    {
        String value = messageMap.get(key);
        if (value != null)
            return value;

        plugin.getMyLogger().warn("Failed to get the translation for key " + key);
        return "Translation for key \"" + key + "\" not found! Contact server admin!";
    }

    public String getStringReverse(String value)
    {
        return messageMap.entrySet().stream()
            .filter(e -> e.getValue().equals(value))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    }
}
