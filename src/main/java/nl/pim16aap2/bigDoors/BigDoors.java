package nl.pim16aap2.bigDoors;

import java.io.File;
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
import nl.pim16aap2.bigDoors.waitForCommand.WaitForCommand;

// TODO: Store starting x,z values in savedBlocks, then make putblocks etc part of abstact class.
// TODO: Add success message for changing door opendirection.
// TODO: Add /RenameDoor command and config option.
// TODO: Add "Server" as door owner.
// TODO: Add /BDM [PlayerName (when online) || PlayerUUID || Server] to open a doorMenu for a specific player.
// TODO: Store playernames as well as UUID in database (verify on login).
// TODO: Add command: /OpenDoor [Player (when online) || PlayerUUID || Server] <DoorName (when owner is specified) || DoorUID>(repeat 1:inf) [Speed (double]
// TODO: Change config so you no longer request specific values via a String.

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

        this.init(true);

        this.db               = new SQLiteJDBCDriverConnection(this, config.dbFile());
        this.commander        = new Commander(this, this.db);
        this.doorOpener       = new DoorOpener(this);
        this.bridgeOpener     = new BridgeOpener(this);
        this.bridgeOpener     = new BridgeOpener(this);
        this.commandHandler   = new CommandHandler(this);
        this.portcullisOpener = new PortcullisOpener(this);

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
        if (!this.validVersion)
            return;

        this.readConfigValues();
        this.messages    = new Messages(this);
        this.toolUsers   = new Vector<ToolUser>(2);
        this.blockMovers = new Vector<BlockMover>(2);
        this.cmdWaiters  = new Vector<WaitForCommand>(2);
        this.tf          = new ToolVerifier(messages.getString("CREATOR.GENERAL.StickName"));
        this.loginString = "";

        if (config.enableRedstone())
        {
            if (this.redstoneHandler == null)
            {
                this.redstoneHandler = new RedstoneHandler(this);
                Bukkit.getPluginManager().registerEvents(this.redstoneHandler, this);
            }
        }
        else if (!firstRun)
        {
            HandlerList.unregisterAll(this.redstoneHandler);
            this.redstoneHandler = null;
        }

        // If it's not the first run, unregister the resource pack handler. It might not be needed anymore.
        if (!firstRun)
        {
            HandlerList.unregisterAll(this.rPackHandler);
            this.rPackHandler = null;
        }
        // If the resourcepack is set to "NONE", don't load it.
        if (!config.resourcePack().equals("NONE"))
        {
            // If a resource pack was set for the current version of Minecraft, send that pack to the client on login.
            this.rPackHandler = new LoginResourcePackHandler(this, config.resourcePack());
            Bukkit.getPluginManager().registerEvents(this.rPackHandler,  this);
        }
        Bukkit.getPluginManager().registerEvents(new EventHandlers      (this), this);
        Bukkit.getPluginManager().registerEvents(new GUIHandler         (this), this);
        Bukkit.getPluginManager().registerEvents(new LoginMessageHandler(this), this);

        // Load stats collector if allowed, otherwise unload it if needed or simply don't load it in the first place.
        if (config.allowStats())
        {
            logger.myLogger(Level.INFO, "Enabling stats! Thanks, it really helps!");
            if (this.metrics == null)
                this.metrics = new Metrics(this);
        } 
        else
        {
            // Y u do dis? :(
            this.metrics = null;
            logger.myLogger(Level.INFO, "Stats disabled, not laoding stats :(... Please consider enabling it! I am a simple man, seeing higher user numbers helps me stay motivated!");
        }

        // Load update checker if allowed, otherwise unload it if needed or simply don't load it in the first place.
        if (config.checkForUpdates())
        {
            if (this.updater == null)
                this.updater = new SpigotUpdater(this, 58669);
        }
        else
            this.updater = null;

        if (!firstRun)
            this.commander.setCanGo(true);
    }

    public void restart()
    {
        if (!this.validVersion)
            return;
        this.reloadConfig();
        // Stop all tool users, end all blockmovers, and clear all command waiter.
        this.onDisable();
        this.toolUsers = null;
        this.cmdWaiters = null;
        this.blockMovers = null;
        this.init(false);
    }

    @Override
    public void onDisable()
    {
        if (!this.validVersion)
            return;

        // Stop all toolUsers and take all BigDoor tools from players.
        this.commander.setCanGo(false);
        for (ToolUser tu : this.toolUsers)
            tu.setIsDone(true);
        for (BlockMover bm : this.blockMovers)
            bm.putBlocks(true);

        this.toolUsers.clear();
        this.cmdWaiters.clear();
        this.blockMovers.clear();
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

    public Opener getDoorOpener(DoorType type)
    {
        switch (type)
        {
        case DOOR:
            return this.doorOpener;
        case DRAWBRIDGE:
            return this.bridgeOpener;
        case PORTCULLIS:
            return this.portcullisOpener;
        }
        return null;
    }

    public void addBlockMover(BlockMover blockMover)
    {
        this.blockMovers.add(blockMover);
    }

    public void removeBlockMover(BlockMover blockMover)
    {
        this.blockMovers.remove(blockMover);
    }

    public Vector<BlockMover> getBlockMovers()
    {
        return this.blockMovers;
    }

    // Get the Vector of ToolUsers.
    public Vector<ToolUser> getToolUsers()
    {
        return this.toolUsers;
    }

    public void addToolUser(ToolUser toolUser)
    {
        this.toolUsers.add(toolUser);
    }

    public void removeToolUser(ToolUser toolUser)
    {
        this.toolUsers.remove(toolUser);
    }

    // Get the Vector of WaitForCommand.
    public Vector<WaitForCommand> getCommandWaiters()
    {
        return this.cmdWaiters;
    }

    public void addCommandWaiter(WaitForCommand cmdWaiter)
    {
        this.cmdWaiters.add(cmdWaiter);
    }

    public void removeCommandWaiter(WaitForCommand cmdWaiter)
    {
        this.cmdWaiters.remove(cmdWaiter);
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
        this.config = new ConfigLoader(this);
        this.locale = config.languageFile();
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
        if (version.equals("v1_13_R2"))
        {
            this.fabf2     = new ArmorStandFactory_V1_13_R2();
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

    public String getLoginString()
    {
        return this.loginString;
    }

    public void setLoginString(String str)
    {
        this.loginString = str;
    }



    /* 
     * API Starts here.
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
        return door.isOpen();
    }

    // Check the open-status of a door from a doorUID.
    public boolean isOpen (long doorUID)
    {
        Door door = this.getCommander().getDoor(doorUID);
        return this.isOpen(door);
    }
}