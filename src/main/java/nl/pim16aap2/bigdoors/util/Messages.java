package nl.pim16aap2.bigdoors.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import nl.pim16aap2.bigdoors.BigDoors;

public class Messages
{
    private HashMap<String, String> messageMap = new HashMap<>();
    private final BigDoors plugin;
    private File textFile;

    public Messages(BigDoors plugin)
    {
        this.plugin = plugin;
        textFile = new File(plugin.getDataFolder(), plugin.getConfigLoader().languageFile() + ".txt");
        if (!textFile.exists())
        {
            plugin.getMyLogger().logMessage("Failed to load language file: \"" + textFile + "\": File not found! Using default file instead!", true, false);
            textFile = new File(plugin.getDataFolder(), "en_US.txt");
        }
        readFile();
    }

    private void writeDefaultFile()
    {
        File defaultFile = new File(plugin.getDataFolder(), "en_US.txt");
        if (defaultFile.exists() && !defaultFile.setWritable(true))
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
                // Ignore comments.
                if (sCurrentLine.startsWith("#"))
                    continue;
                String key, value;
                String[] parts = sCurrentLine.split("=", 2);
                key    = parts[0];
                value  = parts[1].replaceAll("&((?i)[0-9a-fk-or])", "\u00A7$1");
                String[] newLineSplitter = value.split("\\\\n"); // Wut? Can I haz more backslash?

                String values = newLineSplitter[0];

                for (int idx = 1; idx < newLineSplitter.length; ++idx)
                    values += "\n" + newLineSplitter[idx];

                messageMap.put(key, values);
            }
            br.close();
        }
        catch (FileNotFoundException e)
        {
            plugin.getMyLogger().myLogger(Level.SEVERE, "Locale file \"" + textFile + "\" does not exist!");
        }
        catch (IOException e)
        {
            plugin.getMyLogger().myLogger(Level.SEVERE, "Could not read locale file! \"" + textFile + "\"");
            e.printStackTrace();
        }
    }

    public String getString(String key)
    {
        String value = null;
        value = messageMap.get(key);
        if (value == null)
        {
            value = "BigDoors: Translation not found! Contact server admin!";
            plugin.getMyLogger().logMessageToConsole("Failed to get the translation for key " + key);
        }
        return value;
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
