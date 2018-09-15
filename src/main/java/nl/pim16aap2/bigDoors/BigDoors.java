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

import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory_Vall;
import nl.pim16aap2.bigDoors.NMS.AS_v1_12_R1.ArmorStandFactory_V1_12_R1;
import nl.pim16aap2.bigDoors.NMS.v1_11_R1.FallingBlockFactory_V1_11_R1;
import nl.pim16aap2.bigDoors.NMS.v1_12_R1.FallingBlockFactory_V1_12_R1;
import nl.pim16aap2.bigDoors.NMS.v1_13_R1.FallingBlockFactory_V1_13_R1;
import nl.pim16aap2.bigDoors.NMS.v1_13_R2.FallingBlockFactory_V1_13_R2;
import nl.pim16aap2.bigDoors.handlers.CommandHandler;
import nl.pim16aap2.bigDoors.handlers.EventHandlers;
import nl.pim16aap2.bigDoors.handlers.GUIHandler;
import nl.pim16aap2.bigDoors.handlers.LoginMessageHandler;
import nl.pim16aap2.bigDoors.handlers.LoginResourcePackHandler;
import nl.pim16aap2.bigDoors.handlers.RedstoneHandler;
import nl.pim16aap2.bigDoors.moveBlocks.BridgeOpener;
import nl.pim16aap2.bigDoors.moveBlocks.DoorOpener;
import nl.pim16aap2.bigDoors.moveBlocks.Opener;
import nl.pim16aap2.bigDoors.moveBlocks.PortcullisOpener;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationEast;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationNorth;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationSouth;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationWest;
import nl.pim16aap2.bigDoors.storage.sqlite.SQLiteJDBCDriverConnection;
import nl.pim16aap2.bigDoors.util.ConfigLoader;
import nl.pim16aap2.bigDoors.util.Messages;
import nl.pim16aap2.bigDoors.util.Metrics;

// TODO: Allow upright drawbridge creation.
// TODO: Make DoorInfo work for console.
// TODO: Make ListDoors explain there are no doors found when none around found.
// TODO: Get rid of "Multiple doors with that name found" message when there are actually 0 hits.
// TODO: Add a "Door Info" tool, that can get door info from hitting a power block.
// TODO: Make sure that doors don't get fucked up when player leaves the area or when the server stops.
// TODO: Make additional movement in animation algorithms depend on speed rather than size.

public class BigDoors extends JavaPlugin implements Listener
{
	private ToolVerifier                      tf;
	private SQLiteJDBCDriverConnection        db;
	private FallingBlockFactory_Vall        fabf;
	private Vector<DoorCreator>             dcal;
	private FallingBlockFactory_Vall       fabf2;
	private Vector<PowerBlockRelocator>    rlocs;
	private ConfigLoader                  config;
	private String                        locale;
	private MyLogger                      logger;
	private File                         logFile;
	private Messages                    messages;
	private Commander                  commander;
	private Vector<PortcullisCreator> pcCreators;
	private DoorOpener                doorOpener;
	private BridgeOpener            bridgeOpener;
	private boolean                 validVersion;
	private CommandHandler        commandHandler;
	private PortcullisOpener    portcullisOpener;
	
	private boolean               is1_13 = false;
	private boolean            enabledAS = false;

	@Override
	public void onEnable()
	{
		logFile        = new File(getDataFolder(), "log.txt");
		logger         = new MyLogger(this, logFile);
		
		validVersion = compatibleMCVer();
		// Load the files for the correct version of Minecraft.
		if (!validVersion) 
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
		
		dcal             = new Vector<DoorCreator>(2);
		rlocs            = new Vector<PowerBlockRelocator>(2);
		pcCreators       = new Vector<PortcullisCreator>(2);
		this.db          = new SQLiteJDBCDriverConnection(this, config.getString("dbFile"));
		this.tf          = new ToolVerifier(messages.getString("DC.StickName"));
		doorOpener       = new DoorOpener(this);
		bridgeOpener     = new BridgeOpener(this);
		bridgeOpener     = new BridgeOpener(this);
		portcullisOpener = new PortcullisOpener(this);
		
		commandHandler   = new CommandHandler(this);
		commander        = new Commander(this, db);
		Bukkit.getPluginManager().registerEvents(new EventHandlers   (this), this);
		Bukkit.getPluginManager().registerEvents(new GUIHandler      (this), this);
		Bukkit.getPluginManager().registerEvents(new RedstoneHandler (this), this);
		getCommand("changepowerblockloc").setExecutor(new CommandHandler(this));
		getCommand("bigdoorsenableas"   ).setExecutor(new CommandHandler(this));
		getCommand("newportcullis"      ).setExecutor(new CommandHandler(this));
		getCommand("unlockDoor"         ).setExecutor(new CommandHandler(this));
		getCommand("pausedoors"         ).setExecutor(new CommandHandler(this));
		getCommand("doordebug"          ).setExecutor(new CommandHandler(this));
		getCommand("bdversion"          ).setExecutor(new CommandHandler(this));
		getCommand("opendoors"          ).setExecutor(new CommandHandler(this));
		getCommand("listdoors"          ).setExecutor(new CommandHandler(this));
		getCommand("stopdoors"          ).setExecutor(new CommandHandler(this));
		getCommand("bdcancel"           ).setExecutor(new CommandHandler(this));
		getCommand("doorinfo"           ).setExecutor(new CommandHandler(this));
		getCommand("opendoor"           ).setExecutor(new CommandHandler(this));
		getCommand("nameDoor"           ).setExecutor(new CommandHandler(this));
		getCommand("bigdoors"           ).setExecutor(new CommandHandler(this));
		getCommand("newdoor"            ).setExecutor(new CommandHandler(this));
		getCommand("deldoor"            ).setExecutor(new CommandHandler(this));
		getCommand("fixdoor"            ).setExecutor(new CommandHandler(this));
		getCommand("bdm"                ).setExecutor(new CommandHandler(this));

		liveDevelopmentLoad();
		
		// If a resource pack was set for the current version of Minecraft, send that pack to the client on login.
		if (!config.getString("resourcePack").equals("NONE"))
			Bukkit.getPluginManager().registerEvents(new LoginResourcePackHandler(this, config.getString("resourcePack")), this);
		
		if (config.getBool("checkForUpdates"))
		{
			// Check for updates in a new thread, so the server won't hang when it cannot contact the update servers.
			Thread thread = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					BigDoors plugin = getPlugin();
				    SpigotUpdater updater = new SpigotUpdater(plugin, 58669);
				    try 
				    {
				        if (updater.checkForUpdates())
				        {
				            getLogger().info("An update was found! New version: " + updater.getLatestVersion() + " download: " + updater.getResourceURL());
				            Bukkit.getPluginManager().registerEvents(new LoginMessageHandler(plugin, "The BigDoors plugin is out of date!"), plugin);
				        }
				    }
				    catch (Exception exc) 
				    {
				    		getMyLogger().logMessage("Could not check for updates! Send this to pim16aap2: \n" + exc.getStackTrace().toString(), true, false);
				    }
				}
		
			});
			thread.start();
		}

//		readDoors(); // Import doors from .txt file. Only needed for debugging! I'm the only one with the file!
	}

	@Override
	public void onDisable()
	{
		// Force stop all doors. I don't know if this is needed. Do spigot stop running tasks gracefully already?
		// Does Spigot stop them forcefully (and destroy them in the process) meaning this does exactly fuck all?
		// So many questions, so many answers probably online. TODO: Don't be lazy and do some research.
		if (validVersion)
		{
			this.commander.setCanGo(false);
			for (DoorCreator dc : this.getDoorCreators())
				dc.takeToolFromPlayer();
			for (PowerBlockRelocator pbr : this.getRelocators())
				pbr.takeToolFromPlayer();
		}
	}
	
	// Read the saved list of doors, if it exists. ONLY for debugging purposes. Should be removed from the final export!
	@SuppressWarnings("unused")
	private void readDoors()
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
				
				Door door = new Door(world, xMin, yMin, zMin, xMax, yMax, zMax, engineX, engineY, engineZ, name, isOpen, -1, false, 0, "27e6c556-4f30-32bf-a005-c80a46ddd935", 0, -1, -1, -1);
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
	
	// Get the Vector of relocators (= users relocating the power block of a door).
	public Vector<PowerBlockRelocator> getRelocators()
	{
		return this.rlocs;
	}
	// Get the Vector of relocators (= users relocating the power block of a door).
	public Vector<PortcullisCreator> getPCCreators()
	{
		return this.pcCreators;
	}
	
	public FallingBlockFactory_Vall getFABF()
	{
		return this.fabf;
	}
	
	public FallingBlockFactory_Vall getFABF2()
	{
		return this.fabf2;
	}
	
	public BigDoors getPlugin()
	{
		return this;
	}
	
	public Opener getDoorOpener(int type)
	{
		switch (type)
		{
		case 0:
			return this.doorOpener;
		case 1:
			return this.bridgeOpener;
		case 2:
			return this.portcullisOpener;
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
	
	private void readConfigValues()
	{
		// Load the settings from the config file.
		this.config 	= new ConfigLoader(this);
		this.locale = config.getString("languageFile");
	}
	
	// This function simply loads these classes to make my life a bit less hell-ish with live development.
	private void liveDevelopmentLoad()
	{
		new GetNewLocationNorth ();
		new GetNewLocationEast  ();
		new GetNewLocationSouth ();
		new GetNewLocationWest  ();
		commandHandler.stopDoors();
	}
	
	public boolean is1_13()
	{
		return this.is1_13;
	}
	
	public boolean bigDoorsEnableAS()
	{
		if (this.enabledAS)
		{
			this.enabledAS = false;
			this.fabf2 = null;
			return true;
		}
		
		String version;

        try 
        {
            version = Bukkit.getServer().getClass().getPackage().getName().replace(".",  ",").split(",")[3];
        } 
        catch (ArrayIndexOutOfBoundsException useAVersionMentionedInTheDescriptionPleaseException) 
        {
            return false;
        }

        this.fabf2 = null;
        if (version.equals("v1_12_R1"))
        {
        		this.fabf2     = new ArmorStandFactory_V1_12_R1();
    			this.enabledAS = true;
        }
        // Return true if compatible.
        return fabf2 != null;
	}
	
	public boolean isASEnabled()
	{
		return enabledAS;
	}
	
	// Check + initialize for the correct version of Minecraft.
	private boolean compatibleMCVer()
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

        this.fabf  = null;
        this.fabf2 = null;
        if (version.equals("v1_11_R1"))
			this.fabf     = new FallingBlockFactory_V1_11_R1();
        else if (version.equals("v1_12_R1"))
        {
        		this.fabf     = new FallingBlockFactory_V1_12_R1();
        		this.fabf2    = new ArmorStandFactory_V1_12_R1();
        }
        else if (version.equals("v1_13_R1"))
        {
	        	this.is1_13   = true;
	        	this.fabf     = new FallingBlockFactory_V1_13_R1();
        }
        else if (version.equals("v1_13_R2"))
        {
        		this.is1_13   = true;
			this.fabf     = new FallingBlockFactory_V1_13_R2();
        }
        // Return true if compatible.
        return fabf != null;
	}
	
	
	
	
	
	/* 
	 * API (ish) Starts here.
	 */
	
	// (Instantly?) Toggle a door with a given time.
	private boolean toggleDoor(Door door, double time, boolean instantOpen)
	{
		return this.getDoorOpener(door.getType()).openDoor(door, time, instantOpen, false);
	}
	
	// Toggle a door from a doorUID and instantly or not.
	public boolean toggleDoor(long doorUID, boolean instantOpen)
	{
		Door door = this.getCommander().getDoor(doorUID);
		return toggleDoor(door, 0.0, instantOpen);
	}
	
	// Toggle a door from a doorUID and a given time.
	public boolean toggleDoor(long doorUID, double time)
	{
		Door door = this.getCommander().getDoor(doorUID);
		return toggleDoor(door, time, false);
	}
	
	// Toggle a door from a doorUID using default values.
	public boolean toggleDoor(long doorUID)
	{
		Door door = this.getCommander().getDoor(doorUID);
		return toggleDoor(door, 0.0, false);
	}
	
	// Check the open-status of a door.
	private boolean isOpen (Door door)
	{
		return door.getStatus();
	}
	
	// Check the open-status of a door from a doorUID.
	public boolean isOpen (long doorUID)
	{
		Door door = this.getCommander().getDoor(doorUID);
		return this.isOpen(door);
	}
}