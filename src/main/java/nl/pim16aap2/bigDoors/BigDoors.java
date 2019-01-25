package nl.pim16aap2.bigDoors;

import java.io.File;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory_Vall;
import nl.pim16aap2.bigDoors.NMS.AS_v1_12_R1.ArmorStandFactory_V1_12_R1;
import nl.pim16aap2.bigDoors.NMS.AS_v1_13_R2.ArmorStandFactory_V1_13_R2;
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
import nl.pim16aap2.bigDoors.moveBlocks.BlockMover;
import nl.pim16aap2.bigDoors.moveBlocks.BridgeOpener;
import nl.pim16aap2.bigDoors.moveBlocks.DoorOpener;
import nl.pim16aap2.bigDoors.moveBlocks.Opener;
import nl.pim16aap2.bigDoors.moveBlocks.PortcullisOpener;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationEast;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationNorth;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationSouth;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationWest;
import nl.pim16aap2.bigDoors.storage.sqlite.SQLiteJDBCDriverConnection;
import nl.pim16aap2.bigDoors.toolUsers.ToolUser;
import nl.pim16aap2.bigDoors.toolUsers.ToolVerifier;
import nl.pim16aap2.bigDoors.util.ConfigLoader;
import nl.pim16aap2.bigDoors.util.DoorType;
import nl.pim16aap2.bigDoors.util.Messages;
import nl.pim16aap2.bigDoors.util.Metrics;
import nl.pim16aap2.bigDoors.util.TimedCache;
import nl.pim16aap2.bigDoors.waitForCommand.WaitForCommand;

// TODO: Store starting x,z values in savedBlocks, then make putblocks etc part of abstact class.
// TODO: Add success message for changing door opendirection.
// TODO: Add /RenameDoor command and config option.
// TODO: Add "Server" as door owner.
// TODO: Add /BDM [PlayerName (when online) || PlayerUUID || Server] to open a doorMenu for a specific player.
// TODO: Store playernames as well as UUID in database (verify on login).
// TODO: Add command: /OpenDoor [Player (when online) || PlayerUUID || Server] <DoorName (when owner is specified) || DoorUID>(repeat 1:inf) [Speed (double]
// TODO: Change config so you no longer request specific values via a String.
// TODO: Add permissions per door-type.
// TODO: Catch specific exceptions in update checker. Or at least ssl exception, it's very spammy.
// TODO: Add javadoc (@ param) stuff etc to "api" and replace any method comment by jdoc stuff.
// TODO: Use lambda for block movement to get rid of code duplication (all the iterators).
// TODO: Fix /BDRestart

public class BigDoors extends JavaPlugin implements Listener
{
    private ToolVerifier                   tf;
    private SQLiteJDBCDriverConnection     db;
    private FallingBlockFactory_Vall     fabf;
    private FallingBlockFactory_Vall    fabf2;
    private ConfigLoader               config;
    private String                     locale;
    private MyLogger                   logger;
    private SpigotUpdater             updater;
    private Metrics                   metrics;
    private File                      logFile;
    private Messages                 messages;
    private Vector<ToolUser>        toolUsers;
    private Commander               commander;
    private Vector<WaitForCommand> cmdWaiters;
    private DoorOpener             doorOpener;
    private Vector<BlockMover>    blockMovers;
    private BridgeOpener         bridgeOpener;
    private boolean              validVersion;
    private CommandHandler     commandHandler;
    private RedstoneHandler   redstoneHandler;
    private PortcullisOpener portcullisOpener;
    private String                loginString;
    //                 Chunk         Location DoorUID
    private TimedCache<Long, HashMap<Long,    Long>> pbCache; // Powerblock cache.

    private boolean            is1_13 = false;
    private boolean         enabledAS = false;

    private LoginResourcePackHandler rPackHandler;

    @Override
    public void onEnable()
    {
        logFile = new File(getDataFolder(), "log.txt");
        logger  = new MyLogger(this, logFile);

        validVersion = compatibleMCVer();
        // Load the files for the correct version of Minecraft.
        if (!validVersion)
        {
            logger.logMessage("Trying to load the plugin on an incompatible version of Minecraft! This plugin will NOT be enabled!", true, true);
            return;
        }

        init(true);

        db               = new SQLiteJDBCDriverConnection(this, config.dbFile());
        commander        = new Commander(this, db);
        doorOpener       = new DoorOpener(this);
        bridgeOpener     = new BridgeOpener(this);
        bridgeOpener     = new BridgeOpener(this);
        commandHandler   = new CommandHandler(this);
        portcullisOpener = new PortcullisOpener(this);

        getCommand("inspectpowerblockloc").setExecutor(new CommandHandler(this));
        getCommand("changepowerblockloc" ).setExecutor(new CommandHandler(this));
        getCommand("bigdoorsenableas"    ).setExecutor(new CommandHandler(this));
        getCommand("setautoclosetime"    ).setExecutor(new CommandHandler(this));
        getCommand("setdoorrotation"     ).setExecutor(new CommandHandler(this));
        getCommand("newportcullis"       ).setExecutor(new CommandHandler(this));
        getCommand("toggledoor"          ).setExecutor(new CommandHandler(this));
        getCommand("pausedoors"          ).setExecutor(new CommandHandler(this));
        getCommand("bdrestart"           ).setExecutor(new CommandHandler(this));
        getCommand("closedoor"           ).setExecutor(new CommandHandler(this));
        getCommand("doordebug"           ).setExecutor(new CommandHandler(this));
        getCommand("bdversion"           ).setExecutor(new CommandHandler(this));
        getCommand("listdoors"           ).setExecutor(new CommandHandler(this));
        getCommand("stopdoors"           ).setExecutor(new CommandHandler(this));
        getCommand("bdcancel"            ).setExecutor(new CommandHandler(this));
        getCommand("filldoor"            ).setExecutor(new CommandHandler(this));
        getCommand("doorinfo"            ).setExecutor(new CommandHandler(this));
        getCommand("opendoor"            ).setExecutor(new CommandHandler(this));
        getCommand("nameDoor"            ).setExecutor(new CommandHandler(this));
        getCommand("bigdoors"            ).setExecutor(new CommandHandler(this));
        getCommand("newdoor"             ).setExecutor(new CommandHandler(this));
        getCommand("deldoor"             ).setExecutor(new CommandHandler(this));
        getCommand("bdm"                 ).setExecutor(new CommandHandler(this));

        liveDevelopmentLoad();
    }

    private void init(boolean firstRun)
    {
        if (!validVersion)
            return;

        readConfigValues();
        messages    = new Messages(this);
        toolUsers   = new Vector<ToolUser>(2);
        blockMovers = new Vector<BlockMover>(2);
        cmdWaiters  = new Vector<WaitForCommand>(2);
        tf          = new ToolVerifier(messages.getString("CREATOR.GENERAL.StickName"));
        loginString = "";

        if (!firstRun)
        {
            HandlerList.unregisterAll(redstoneHandler);
            redstoneHandler = null;

            HandlerList.unregisterAll(rPackHandler);
            rPackHandler = null;

            pbCache.reinit(config.cacheTimeout());
        }
        else
        {
            pbCache = new TimedCache<Long, HashMap<Long, Long>>(this, config.cacheTimeout());
        }

        if (config.enableRedstone())
        {
            redstoneHandler = new RedstoneHandler(this);
            Bukkit.getPluginManager().registerEvents(redstoneHandler, this);
        }

        if (!config.resourcePack().equals("NONE"))
        {
            rPackHandler = new LoginResourcePackHandler(this, config.resourcePack());
            Bukkit.getPluginManager().registerEvents(rPackHandler,  this);
        }
        Bukkit.getPluginManager().registerEvents(new EventHandlers      (this), this);
        Bukkit.getPluginManager().registerEvents(new GUIHandler         (this), this);
        Bukkit.getPluginManager().registerEvents(new LoginMessageHandler(this), this);

        if (config.allowStats())
        {
            logger.myLogger(Level.INFO, "Enabling stats! Thanks, it really helps!");
            if (metrics == null)
                metrics = new Metrics(this);
        }
        else
        {
            // Y u do dis? :(
            metrics = null;
            logger.myLogger(Level.INFO, "Stats disabled, not laoding stats :(... Please consider enabling it! I am a simple man, seeing higher user numbers helps me stay motivated!");
        }

        // Load update checker if allowed, otherwise unload it if needed or simply don't load it in the first place.
        if (config.checkForUpdates())
        {
            if (updater == null)
                updater = new SpigotUpdater(this, 58669);
        }
        else
            updater = null;

        if (!firstRun)
            commander.setCanGo(true);
    }

    public void restart()
    {
        if (!validVersion)
            return;
        reloadConfig();
        // Stop all tool users, end all blockmovers, and clear all command waiter.
        onDisable();
        toolUsers = null;
        cmdWaiters = null;
        blockMovers = null;
        init(false);
    }

    @Override
    public void onDisable()
    {
        if (!validVersion)
            return;

        // Stop all toolUsers and take all BigDoor tools from players.
        commander.setCanGo(false);
        for (ToolUser tu : toolUsers)
            tu.setIsDone(true);
        for (BlockMover bm : blockMovers)
            bm.putBlocks(true);

        toolUsers.clear();
        cmdWaiters.clear();
        blockMovers.clear();
    }

    public TimedCache<Long, HashMap<Long, Long>> getPBCache()
    {
        return pbCache;
    }

    public FallingBlockFactory_Vall getFABF()
    {
        return fabf;
    }

    public FallingBlockFactory_Vall getFABF2()
    {
        return fabf2;
    }

    public BigDoors getPlugin()
    {
        return this;
    }

    public Opener getDoorOpener(DoorType type)
    {
        switch (type)
        {
        case DOOR:
            return doorOpener;
        case DRAWBRIDGE:
            return bridgeOpener;
        case PORTCULLIS:
            return portcullisOpener;
        }
        return null;
    }

    public void addBlockMover(BlockMover blockMover)
    {
        blockMovers.add(blockMover);
    }

    public void removeBlockMover(BlockMover blockMover)
    {
        blockMovers.remove(blockMover);
    }

    public Vector<BlockMover> getBlockMovers()
    {
        return blockMovers;
    }

    // Get the Vector of ToolUsers.
    public Vector<ToolUser> getToolUsers()
    {
        return toolUsers;
    }

    public void addToolUser(ToolUser toolUser)
    {
        toolUsers.add(toolUser);
    }

    public void removeToolUser(ToolUser toolUser)
    {
        toolUsers.remove(toolUser);
    }

    // Get the Vector of WaitForCommand.
    public Vector<WaitForCommand> getCommandWaiters()
    {
        return cmdWaiters;
    }

    public void addCommandWaiter(WaitForCommand cmdWaiter)
    {
        cmdWaiters.add(cmdWaiter);
    }

    public void removeCommandWaiter(WaitForCommand cmdWaiter)
    {
        cmdWaiters.remove(cmdWaiter);
    }

    // Get the command Handler.
    public CommandHandler getCommandHandler()
    {
        return commandHandler;
    }

    // Get the commander (class executing commands).
    public Commander getCommander()
    {
        return commander;
    }

    // Get the logger.
    public MyLogger getMyLogger()
    {
        return logger;
    }

    // Get the messages.
    public Messages getMessages()
    {
        return messages;
    }

    // Returns the config handler.
    public ConfigLoader getConfigLoader()
    {
        return config;
    }

    // Get the ToolVerifier.
    public ToolVerifier getTF()
    {
        return tf;
    }

    public String getLocale()
    {
        return locale == null ? "en_US" : locale;
    }

    private void readConfigValues()
    {
        // Load the settings from the config file.
        config = new ConfigLoader(this);
        locale = config.languageFile();
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
        return is1_13;
    }

    public boolean bigDoorsEnableAS()
    {
        if (enabledAS)
        {
            enabledAS = false;
            fabf2 = null;
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

        fabf2 = null;
        if (version.equals("v1_12_R1"))
        {
            fabf2     = new ArmorStandFactory_V1_12_R1();
            enabledAS = true;
        }
        if (version.equals("v1_13_R2"))
        {
            fabf2     = new ArmorStandFactory_V1_13_R2();
            enabledAS = true;
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

        fabf  = null;
        fabf2 = null;
        if (version.equals("v1_11_R1"))
            fabf     = new FallingBlockFactory_V1_11_R1();
        else if (version.equals("v1_12_R1"))
        {
            fabf     = new FallingBlockFactory_V1_12_R1();
            fabf2    = new ArmorStandFactory_V1_12_R1();
        }
        else if (version.equals("v1_13_R1"))
        {
            is1_13   = true;
            fabf     = new FallingBlockFactory_V1_13_R1();
        }
        else if (version.equals("v1_13_R2"))
        {
            is1_13   = true;
            fabf     = new FallingBlockFactory_V1_13_R2();
        }
        // Return true if compatible.
        return fabf != null;
    }

    public String getLoginString()
    {
        return loginString;
    }

    public void setLoginString(String str)
    {
        loginString = str;
    }



    /*
     * API Starts here.
     */

    // (Instantly?) Toggle a door with a given time.
    private boolean toggleDoor(Door door, double time, boolean instantOpen)
    {
        return getDoorOpener(door.getType()).openDoor(door, time, instantOpen, false);
    }

    // Toggle a door from a doorUID and instantly or not.
    public boolean toggleDoor(long doorUID, boolean instantOpen)
    {
        Door door = getCommander().getDoor(doorUID);
        return toggleDoor(door, 0.0, instantOpen);
    }

    // Toggle a door from a doorUID and a given time.
    public boolean toggleDoor(long doorUID, double time)
    {
        Door door = getCommander().getDoor(doorUID);
        return toggleDoor(door, time, false);
    }

    // Toggle a door from a doorUID using default values.
    public boolean toggleDoor(long doorUID)
    {
        Door door = getCommander().getDoor(doorUID);
        return toggleDoor(door, 0.0, false);
    }

    // Check the open-status of a door.
    private boolean isOpen (Door door)
    {
        return door.isOpen();
    }

    // Check the open-status of a door from a doorUID.
    public boolean isOpen (long doorUID)
    {
        Door door = getCommander().getDoor(doorUID);
        return this.isOpen(door);
    }
}