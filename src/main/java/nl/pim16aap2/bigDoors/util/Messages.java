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
		this.plugin     = plugin;
		this.locale     = plugin.getLocale();
		textFile        = new File(plugin.getDataFolder(), locale + ".txt");
		readFile();
	}
	
	// Read locale file.
	private void readFile()
	{
		
		// If the file does not exist, load the default en_US from the resources.
		if (!this.textFile.exists())
			plugin.saveResource("en_US.txt", false);

		try (BufferedReader br = new BufferedReader(new FileReader(this.textFile)))
		{
			String sCurrentLine;

			while ((sCurrentLine = br.readLine()) != null)
			{
				String key, value;
				String[] parts = sCurrentLine.split("=", 2);
				key    = parts[0];
				value  = parts[1];
				this.messageMap.put(key, value);
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
			value = "BigDoors: NULL, contact server owner!";
		return value;
	}
}
