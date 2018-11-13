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
    private Map<String, String> messageMap = new HashMap<String, String>();
    private BigDoors plugin;
    private String   locale;
    private File   textFile;
    
	public Messages(BigDoors plugin)
	{
		this.plugin = plugin;
		this.locale = plugin.getLocale();
		textFile    = new File(plugin.getDataFolder(), locale + ".txt");
		readFile();
	}
	
	// Read locale file.
	private void readFile()
	{
		// Load the default en_US from the resources.
		plugin.saveResource("en_US.txt", true);

		try (BufferedReader br = new BufferedReader(new FileReader(this.textFile)))
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
				
				this.messageMap.put(key, values);
			}
			br.close();
		} 
		catch (FileNotFoundException e)
		{
			plugin.getMyLogger().myLogger(Level.SEVERE, "Locale file " + this.locale + ".txt does not exist!");
		} 
		catch (IOException e)
		{
			plugin.getMyLogger().myLogger(Level.SEVERE, "Could not read locale file! (" + this.locale + ".txt)");
			e.printStackTrace();
		}
	}
	
	// Get a string from a key. Returns "null" if null.
	public String getString(String key)
	{
		String value = null;
		value = this.messageMap.get(key);
		if (value == null)
		{
			value = "BigDoors: Translation not found! Contact server admin!";
			plugin.getMyLogger().logMessageToConsole("Failed to get translation for key " + key);
		}
		return value;
	}
}
