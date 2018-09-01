package nl.pim16aap2.bigDoors.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import nl.pim16aap2.bigDoors.BigDoors;

public class ConfigLoader
{
	private String           header;
	private String           dbFile;
	private boolean      allowStats;
	private int         maxDoorSize;
	private String     languageFile;
	private int        maxDoorCount;
	private boolean  enableRedstone;
	private String   powerBlockType;
	private boolean checkForUpdates;
	
	private String[] enableRedstoneComment 	=
		{
			"Allow doors to be opened using redstone signals."
		};
	private String[] powerBlockTypeComment 	=
		{
			"Choose the type of the power block that is used to open doors using redstone.",
			"A list can be found here: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html",
			"This is the block that will open doors placed on top of it when it receives a redstone signal."	
		};
	private String[] maxDoorCountComment   	=
		{
			"Maximum number of doors a player can own. -1 = infinite."
		};
	private String[] languageFileComment  	=
		{
			"Specify a language file to be used. Note that en_US.txt will get regenerated!"
		};
	private String[] dbFileComment 			=
		{
			"Pick the name (and location if you want) of the database."	
		};
    private String[] checkForUpdatesComment 	= 
		{
			"Allow this plugin to check for updates on startup. It will not download new versions!"
		};
    private String[] allowStatsComment      	= 
    		{
    			"Allow this plugin to send (anonymised) stats using bStats. Please consider keeping it enabled.",
    			"It has a negligible impact on performance and more users on stats keeps me more motivated to support this plugin!"
    		};
    
    private String[] maxDoorSizeComment     	= 
		{
			"Max. number of blocks allowed in a door.",
			"If this number is exceeded, doors will open instantly and skip the animation."
		};
    
	
	private ArrayList<ConfigOption> configOptionsList;
    private BigDoors plugin;
	
	public ConfigLoader(BigDoors plugin) 
	{
		this.plugin = plugin;
		configOptionsList = new ArrayList<ConfigOption>();
		header = "Config file for BigDoors. Don't forget to make a backup before making changes!";
		makeConfig();
	}
	
	// Read the current config, the make a new one based on the old one or default values, whichever is applicable.
	public void makeConfig()
	{
		FileConfiguration config = plugin.getConfig();
		
		// Read all the options from the config, then put them in a configOption with their name, value and comment.
		// Then put all configOptions into an ArrayList.
		enableRedstone  = config.getBoolean(    "allowRedstone"  , true        );
		configOptionsList.add(new ConfigOption( "allowRedstone"  , enableRedstone , enableRedstoneComment ));
		powerBlockType  = config.getString(     "powerBlockType" , "GOLD_BLOCK");
		configOptionsList.add(new ConfigOption( "powerBlockType" , powerBlockType , powerBlockTypeComment ));
		maxDoorCount    = config.getInt(        "maxDoorCount"   , -1          );
		configOptionsList.add(new ConfigOption( "maxDoorCount"   , maxDoorCount   , maxDoorCountComment   ));
		languageFile    = config.getString(     "languageFile"   , "en_US"     );
		configOptionsList.add(new ConfigOption( "languageFile"   , languageFile   , languageFileComment   ));
		dbFile          = config.getString(     "dbFile"         , "doorDB.db" );
		configOptionsList.add(new ConfigOption( "dbFile"         , dbFile         , dbFileComment         ));
		checkForUpdates = config.getBoolean(    "checkForUpdates", true        );
		configOptionsList.add(new ConfigOption( "checkForUpdates", checkForUpdates, checkForUpdatesComment));
		allowStats      = config.getBoolean(    "allowStats"     , true        );
		configOptionsList.add(new ConfigOption( "allowStats"     , allowStats     , allowStatsComment     ));
		maxDoorSize     = config.getInt       ( "maxDoorSize"    , -1          );
		configOptionsList.add(new ConfigOption( "maxDoorSize"    , maxDoorSize    , maxDoorSizeComment    ));
		
		writeConfig();
	}
	
	// Write new config file.
	public void writeConfig()
	{
		// Write all the config options to the config.yml.
		try
		{
			File dataFolder = plugin.getDataFolder();
			if (!dataFolder.exists())
				dataFolder.mkdir();

			File saveTo = new File(plugin.getDataFolder(), "config.yml");
			if (!saveTo.exists())
				saveTo.createNewFile();
			else
			{
				saveTo.delete();
				saveTo.createNewFile();
			}
			FileWriter  fw = new FileWriter(saveTo, true);
			PrintWriter pw = new PrintWriter(fw);
			
			if (header != null)
				pw.println("# " + header + "\n");
			
			for (ConfigOption configOption : configOptionsList)
				pw.println(configOption.toString() + "\n");
			 
			pw.flush();
			pw.close();
		}
		catch (IOException e)
		{
			Bukkit.getLogger().log(Level.SEVERE, "Could not save config.yml! Please contact pim16aap2 and show him the following code:");
			e.printStackTrace();
		}
	}
	
	public Integer getInt(String path)
	{
		for (ConfigOption configOption : configOptionsList)
			if (configOption.getName().equals(path))
				return configOption.getInt();
		return null;
	}
	
	public Boolean getBool(String path)
	{
		for (ConfigOption configOption : configOptionsList)
			if (configOption.getName().equals(path))
				return configOption.getBool();
		return null;
	}
	
	public String getString(String path)
	{
		for (ConfigOption configOption : configOptionsList)
			if (configOption.getName().equals(path))
				return configOption.getString();
		return null;
	}
	
	public List<String> getStringList(String path)
	{
		for (ConfigOption configOption : configOptionsList)
			if (configOption.getName().equals(path))
				return configOption.getStringList();
		return null;
	}
}
