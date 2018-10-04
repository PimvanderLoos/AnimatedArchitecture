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
	private String           dbFile;
	private String           header;
	private int            coolDown;
	private boolean      allowStats;
	private int         maxDoorSize;
	private double     pcMultiplier = 1.0;
	private double     dbMultiplier = 1.0;
	private double     bdMultiplier = 1.0;
	private String     resourcePack;
	private String     languageFile;
	private int        maxDoorCount;
	private boolean    autoDLUpdate;
	private boolean  enableRedstone;
	private String   powerBlockType;
	private boolean checkForUpdates;
	
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
		String defResPackUrl     = "https://www.dropbox.com/s/0q6h8jkfjqrn1tp/BigDoorsResourcePack.zip?dl=1";
		String defResPackUrl1_13 = "https://www.dropbox.com/s/al4idl017ggpnuq/BigDoorsResourcePack-1_13.zip?dl=1";
		
		String[] enableRedstoneComment 	=
			{
				"Allow doors to be opened using redstone signals."
			};
		String[] powerBlockTypeComment 	=
			{
				"Choose the type of the power block that is used to open doors using redstone.",
				"A list can be found here: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html",
				"This is the block that will open doors placed on top of it when it receives a redstone signal."	
			};
		String[] maxDoorCountComment   	=
			{
				"Maximum number of doors a player can own. -1 = infinite."
			};
		String[] languageFileComment  	=
			{
				"Specify a language file to be used. Note that en_US.txt will get regenerated!"
			};
		String[] dbFileComment 			=
			{
				"Pick the name (and location if you want) of the database."	
			};
		String[] checkForUpdatesComment 	= 
			{
					"Allow this plugin to check for updates on startup. It will not download new versions!"
			};
	    String[] autoDLUpdateComment 	= 
			{
				"Allow this plugin to automatically download new updates. They will be applied on restart."
			};
	    String[] allowStatsComment      	= 
	    		{
	    			"Allow this plugin to send (anonymised) stats using bStats. Please consider keeping it enabled.",
	    			"It has a negligible impact on performance and more users on stats keeps me more motivated to support this plugin!"
	    		};
	    String[] maxDoorSizeComment     	= 
			{
				"Max. number of blocks allowed in a door.",
				"If this number is exceeded, doors will open instantly and skip the animation."
			};
	    String[] resourcePackComment     =
		    	{
		    		"This plugin uses a support resource pack for things suchs as sound.",
		    		"You can let this plugin load the resource pack for you or load it using your server.properties if you prefer that.",
		    		"Of course, you can also disable the resource pack altogether as well. Just put \"NONE\" (without quotation marks) as url.",
		    		"The default resource pack for 1.11.x/1.12.x is: \'" + defResPackUrl     + "'",
		    		"The default resource pack for 1.13.x is: \'"        + defResPackUrl1_13 + "\'"
		    	};
	    String[] multiplierComment       =
		    	{
		    		"These multipliers affect the opening/closing speed of their respective door types.",
		    		"Note that the maximum speed is limited, so beyond a certain point rasising these values won't have any effect.",
		    		"To use the default values, set them to \"0.0\" or \"1.0\" (without quotation marks).",
		    		"bd = Big Door, pc = Portcullis, db = Drawbridge.",
		    		"Note that everything is optimized for default values, so it's recommended to leave this setting as-is."
		    	};
	    String[] coolDownComment         =
		    	{
		    		"Cooldown on using doors. Time is measured in seconds."
		    	};
		
		FileConfiguration config = plugin.getConfig();
		
		// Read all the options from the config, then put them in a configOption with their name, value and comment.
		// Then put all configOptions into an ArrayList.
		enableRedstone   = config.getBoolean(   "allowRedstone"   , true             );
		configOptionsList.add(new ConfigOption( "allowRedstone"   , enableRedstone  , enableRedstoneComment ));
		powerBlockType   = config.getString(    "powerBlockType"  , "GOLD_BLOCK"     );
		configOptionsList.add(new ConfigOption( "powerBlockType"  , powerBlockType  , powerBlockTypeComment ));
		maxDoorCount     = config.getInt(       "maxDoorCount"    , -1               );
		configOptionsList.add(new ConfigOption( "maxDoorCount"    , maxDoorCount    , maxDoorCountComment   ));
		languageFile     = config.getString(    "languageFile"    , "en_US"          );
		configOptionsList.add(new ConfigOption( "languageFile"    , languageFile    , languageFileComment   ));
		dbFile           = config.getString(    "dbFile"          , "doorDB.db"      );
		configOptionsList.add(new ConfigOption( "dbFile"          , dbFile          , dbFileComment         ));
		checkForUpdates  = config.getBoolean(   "checkForUpdates" , true             );
		configOptionsList.add(new ConfigOption( "checkForUpdates" , checkForUpdates , checkForUpdatesComment));
		autoDLUpdate     = config.getBoolean(   "auto-update"     , true             );
		configOptionsList.add(new ConfigOption( "auto-update"     , autoDLUpdate    , autoDLUpdateComment   ));
		allowStats       = config.getBoolean(   "allowStats"      , true             );
		configOptionsList.add(new ConfigOption( "allowStats"      , allowStats      , allowStatsComment     ));
		maxDoorSize      = config.getInt       ("maxDoorSize"     , -1               );
		configOptionsList.add(new ConfigOption( "maxDoorSize"     , maxDoorSize     , maxDoorSizeComment    ));
		resourcePack     = config.getString(    "resourcePack"    , plugin.is1_13() ? defResPackUrl1_13 : defResPackUrl);
		configOptionsList.add(new ConfigOption( "resourcePack"    , resourcePack    , resourcePackComment   ));
		bdMultiplier     = config.getDouble(    "bdMultiplier"    , 0.0D             );
		configOptionsList.add(new ConfigOption( "bdMultiplier"    , bdMultiplier    , multiplierComment     ));
		pcMultiplier     = config.getDouble(    "pcMultiplier"    , 0.0D             );
		configOptionsList.add(new ConfigOption( "pcMultiplier"    , pcMultiplier    , null                  ));
		dbMultiplier     = config.getDouble(    "dbMultiplier"    , 0.0D             );
		configOptionsList.add(new ConfigOption( "dbMultiplier"    , dbMultiplier    , null                  ));
		coolDown         = config.getInt(       "coolDown"        , 0                );
		configOptionsList.add(new ConfigOption( "coolDown"        , coolDown        , coolDownComment       ));
		
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
			
			for (int idx = 0; idx < configOptionsList.size(); ++idx)
				pw.println(configOptionsList.get(idx).toString() + 
						// Only print an additional newLine if the next config option has a comment.
						(idx < configOptionsList.size() - 1 && configOptionsList.get(idx + 1).getComment() == null ? "" : "\n"));
			 
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
	
	public Double getDouble(String path)
	{
		for (ConfigOption configOption : configOptionsList)
			if (configOption.getName().equals(path))
				return configOption.getDouble();
		return null;
	}
}