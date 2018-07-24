package nl.pim16aap2.bigDoors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import nl.pim16aap2.bigDoors.customEntities.FallingBlockFactory_Vall;
import nl.pim16aap2.bigDoors.customEntities.v1_11_R1.FallingBlockFactory_V1_11_R1;
import nl.pim16aap2.bigDoors.customEntities.v1_12_R1.FallingBlockFactory_V1_12_R1;
import nl.pim16aap2.bigDoors.handlers.CommandHandler;
import nl.pim16aap2.bigDoors.handlers.EventHandlers;
import nl.pim16aap2.bigDoors.handlers.GUIHandler;
import nl.pim16aap2.bigDoors.handlers.RedstoneHandler;
import nl.pim16aap2.bigDoors.moveBlocks.BridgeOpener;
import nl.pim16aap2.bigDoors.moveBlocks.DoorOpener;
import nl.pim16aap2.bigDoors.moveBlocks.Opener;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationEast;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationNorth;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationSouth;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationWest;
import nl.pim16aap2.bigDoors.storage.sqlite.SQLiteJDBCDriverConnection;
import nl.pim16aap2.bigDoors.util.ConfigLoader;
import nl.pim16aap2.bigDoors.util.Messages;
import nl.pim16aap2.bigDoors.util.Metrics;

// TODO: Create commandlistener that can be used to wait for command trees. Create interface and extend queued up commands from there.
public class BigDoors extends JavaPlugin implements Listener
{
	private ToolVerifier               tf;
	private SQLiteJDBCDriverConnection db;
	private FallingBlockFactory_Vall fabf;
	private Vector<DoorCreator>      dcal;
	private ConfigLoader           config;
	private String                 locale;
	private MyLogger               logger;
	private File                  logFile;
	private Messages             messages;
	private Commander           commander;
	private DoorOpener         doorOpener;
	private BridgeOpener     bridgeOpener;
	private CommandHandler commandHandler;
	

	@Override
	public void onEnable()
	{
		logFile        = new File(getDataFolder(), "log.txt");
		logger         = new MyLogger(this, logFile);
		
		// Load the files for the correct version of Minecraft.
		if (!compatibleMCVer()) 
		{
			logger.logMessage("Trying to load the plugin on an incompatible version of Minecraft! This plugin will NOT be enabled!", true, true);
			return;
		}
		
		readConfigValues();
				
		this.messages  = new Messages(this);	
		
		// Are stats allowed?
		if (config.getBool("allowStats"))
		{
			logger.myLogger(Level.INFO, "Enabling stats! Thanks, it really helps!");
			@SuppressWarnings("unused")
			Metrics metrics = new Metrics(this);
		} 
		else 
			// Y u do dis? :(
			logger.myLogger(Level.INFO, "Stats disabled, not laoding stats :(... Please consider enabling it! I am a simple man, seeing higher user numbers helps me stay motivated!");

		
		dcal           = new Vector<DoorCreator>(2);
		this.db        = new SQLiteJDBCDriverConnection(this, config.getString("dbFile"));
		this.tf        = new ToolVerifier(messages.getString("DC.StickName"));
		doorOpener     = new DoorOpener(this);
		bridgeOpener   = new BridgeOpener(this);
		
		commandHandler = new CommandHandler(this);
		commander      = new Commander(this, db);
		Bukkit.getPluginManager().registerEvents(new EventHandlers   (this), this);
		Bukkit.getPluginManager().registerEvents(new GUIHandler      (this), this);
		Bukkit.getPluginManager().registerEvents(new RedstoneHandler (this), this);
		getCommand("unlockDoor").setExecutor(new CommandHandler(this));
		getCommand("pausedoors").setExecutor(new CommandHandler(this));
		getCommand("doordebug" ).setExecutor(new CommandHandler(this));
		getCommand("opendoors" ).setExecutor(new CommandHandler(this));
		getCommand("listdoors" ).setExecutor(new CommandHandler(this));
		getCommand("stopdoors" ).setExecutor(new CommandHandler(this));
		getCommand("doorinfo"  ).setExecutor(new CommandHandler(this));
		getCommand("opendoor"  ).setExecutor(new CommandHandler(this));
		getCommand("nameDoor"  ).setExecutor(new CommandHandler(this));
		getCommand("bigdoors"  ).setExecutor(new CommandHandler(this));
		getCommand("newdoor"   ).setExecutor(new CommandHandler(this));
		getCommand("deldoor"   ).setExecutor(new CommandHandler(this));
		getCommand("fixdoor"   ).setExecutor(new CommandHandler(this));
		getCommand("shutup"    ).setExecutor(new CommandHandler(this));
		getCommand("bdm"       ).setExecutor(new CommandHandler(this));
		
		liveDevelopmentLoad();
		
//		readDoors(); // Import doors from .txt file. Only needed for debugging! I'm the only one with the file!
	}

	@Override
	public void onDisable()
	{
		// Force stop all doors. I don't know if this is needed. Do spigot stop running tasks gracefully already?
		// Does Spigot stop them forcefully (and destroy them in the process) meaning this does exactly fuck all?
		// So many questions, so many answers probably online. TODO: Don't be lazy and do some research.
		this.commander.setCanGo(false);
		// TODO: Remove all door creator sticks on disable.
		// TODO: Do not allow dropping / moving to transfor of creator sticks.
	}
	
	// Read the saved list of doors, if it exists. ONLY for debugging purposes. Should be removed from the final export!
	public void readDoors()
	{
		File dataFolder = getDataFolder();
		if (!dataFolder.exists())
		{
			Bukkit.getLogger().log(Level.INFO, "No save file found. No doors loaded!");
			return;
		}
		File readFrom = new File(getDataFolder(), "doors.txt");
		try (BufferedReader br = new BufferedReader(new FileReader(readFrom)))
		{
			String sCurrentLine;
			sCurrentLine = br.readLine();

			while (sCurrentLine != null)
			{
				int xMin, yMin, zMin, xMax, yMax, zMax;
				int engineX, engineY, engineZ;
				String name;
				boolean isOpen;
				World world;

				String[] strs = sCurrentLine.trim().split("\\s+");

				name    = strs[0];
				isOpen  = Boolean.getBoolean(strs[1]);
				world   = Bukkit.getServer().getWorld(strs[2]);
				xMin    = Integer.parseInt(strs[3]);
				yMin    = Integer.parseInt(strs[4]);
				zMin    = Integer.parseInt(strs[5]);
				xMax    = Integer.parseInt(strs[6]);
				yMax    = Integer.parseInt(strs[7]);
				zMax    = Integer.parseInt(strs[8]);
				engineX = Integer.parseInt(strs[9]);
				engineY = Integer.parseInt(strs[10]);
				engineZ = Integer.parseInt(strs[11]);
				
				Door door = new Door(world, xMin, yMin, zMin, xMax, yMax, zMax, engineX, engineY, engineZ, name, isOpen, -1, false, 0, "27e6c556-4f30-32bf-a005-c80a46ddd935", 0);
				commander.addDoor(door);

				sCurrentLine = br.readLine();
			}
			br.close();
		} 
		catch (FileNotFoundException e)
		{
			Bukkit.getLogger().log(Level.INFO, "No save file found. No doors loaded!");
		} 
		catch (IOException e)
		{
			Bukkit.getLogger().log(Level.WARNING, "Could not read file!!");
			e.printStackTrace();
		}
	}
	
	// Get the Vector of doorcreators (= users creating a door right now).
	public Vector<DoorCreator> getDoorCreators()
	{
		return this.dcal;
	}
	
	public FallingBlockFactory_Vall getFABF()
	{
		return this.fabf;
	}
	
	public Opener getDoorOpener(int type)
	{
		switch (type)
		{
		case 0:
			return this.doorOpener;
		case 1:
			return this.bridgeOpener;
		}
		return null;
	}
	
	// Get the command Handler.
	public CommandHandler getCommandHandler()
	{
		return this.commandHandler;
	}
	
	// Get the commander (class executing commands).
	public Commander getCommander()
	{
		return this.commander;
	}
	
	// Get the logger.
	public MyLogger getMyLogger()
	{
		return this.logger;
	}
	
	// Get the messages.
	public Messages getMessages()
	{
		return this.messages;
	}

	// Returns the config handler.
	public ConfigLoader getConfigLoader()
	{
		return config;
	}
	
	// Get the ToolVerifier.
	public ToolVerifier getTF()
	{
		return this.tf;
	}
	
	public String getLocale()
	{
		return locale == null ? "en_US" : locale;
	}
	
	public void readConfigValues()
	{
		// Load the settings from the config file.
		this.config 	= new ConfigLoader(this);
		this.locale = config.getString("languageFile");
	}
	
	// This function simply loads these classes to make my life a bit less hell-ish with live development.
	public void liveDevelopmentLoad()
	{
		new GetNewLocationNorth();
		new GetNewLocationEast ();
		new GetNewLocationSouth();
		new GetNewLocationWest ();
	}
	
	// Check + initialize for the correct version of Minecraft.
	public boolean compatibleMCVer()
	{
        String version;

        try 
        {
            version = Bukkit.getServer().getClass().getPackage().getName().replace(".",  ",").split(",")[3];
        } 
        catch (ArrayIndexOutOfBoundsException useAVersionMentionedInTheDescriptionPleaseException) 
        {
            return false;
        }

        if (version.equals("v1_10_R1"))
			this.fabf = null;
        else if (version.equals("v1_11_R1"))
			this.fabf = new FallingBlockFactory_V1_11_R1();
        else if (version.equals("v1_12_R1"))
			this.fabf = new FallingBlockFactory_V1_12_R1();
        // Return true if compatible.
        return fabf != null;
	}
}