package nl.pim16aap2.bigDoors;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import nl.pim16aap2.bigDoors.GUI.GUI;
import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory_Vall;
import nl.pim16aap2.bigDoors.NMS.v1_11_R1.FallingBlockFactory_V1_11_R1;
import nl.pim16aap2.bigDoors.NMS.v1_12_R1.FallingBlockFactory_V1_12_R1;
import nl.pim16aap2.bigDoors.NMS.v1_13_R1.FallingBlockFactory_V1_13_R1;
import nl.pim16aap2.bigDoors.NMS.v1_13_R2.FallingBlockFactory_V1_13_R2;
import nl.pim16aap2.bigDoors.compatiblity.PlotSquaredNewProtectionCompat;
import nl.pim16aap2.bigDoors.compatiblity.PlotSquaredOldProtectionCompat;
import nl.pim16aap2.bigDoors.compatiblity.ProtectionCompat;
import nl.pim16aap2.bigDoors.compatiblity.WorldGuard7ProtectionCompat;
import nl.pim16aap2.bigDoors.handlers.CommandHandler;
import nl.pim16aap2.bigDoors.handlers.EventHandlers;
import nl.pim16aap2.bigDoors.handlers.GUIHandler;
import nl.pim16aap2.bigDoors.handlers.LoginMessageHandler;
import nl.pim16aap2.bigDoors.handlers.LoginResourcePackHandler;
import nl.pim16aap2.bigDoors.handlers.RedstoneHandler;
import nl.pim16aap2.bigDoors.moveBlocks.BlockMover;
import nl.pim16aap2.bigDoors.moveBlocks.BridgeOpener;
import nl.pim16aap2.bigDoors.moveBlocks.DoorOpener;
import nl.pim16aap2.bigDoors.moveBlocks.ElevatorOpener;
import nl.pim16aap2.bigDoors.moveBlocks.FlagOpener;
import nl.pim16aap2.bigDoors.moveBlocks.Opener;
import nl.pim16aap2.bigDoors.moveBlocks.PortcullisOpener;
import nl.pim16aap2.bigDoors.moveBlocks.SlidingDoorOpener;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationEast;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationNorth;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationSouth;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationWest;
import nl.pim16aap2.bigDoors.storage.sqlite.SQLiteJDBCDriverConnection;
import nl.pim16aap2.bigDoors.toolUsers.ToolUser;
import nl.pim16aap2.bigDoors.toolUsers.ToolVerifier;
import nl.pim16aap2.bigDoors.util.ConfigLoader;
import nl.pim16aap2.bigDoors.util.DoorOpenResult;
import nl.pim16aap2.bigDoors.util.DoorType;
import nl.pim16aap2.bigDoors.util.Messages;
import nl.pim16aap2.bigDoors.util.Metrics;
import nl.pim16aap2.bigDoors.util.TimedCache;
import nl.pim16aap2.bigDoors.waitForCommand.WaitForCommand;

// TODO: Add success message for changing door opendirection.
// TODO: Add "Server" as door owner.
// TODO: Add /BDM [PlayerName (when online) || PlayerUUID || Server] to open a doorMenu for a specific player.
// TODO: Catch specific exceptions in update checker. Or at least ssl exception, it's very spammy.
// TODO: Make release and debug build modes.
// TODO: Rewrite Openers to get rid of code duplication.
// TODO: Add javadoc (@ param) stuff etc to "api" and replace any method comment by jdoc stuff.
// TODO: Use lambda for block movement to get rid of code duplication (all the iterators).
// TODO: Use generics for ConfigOption.
// TODO: Make sure the abortable's BukkitTask isn't null.
// TODO: Make invalid input stuff more informative (e.g. int, float etc).
// TODO: Improve recovering from invalid input. When people use a float instead of an int, cast to int.
// TODO: Split up SQL functions into 2: One taking a connection and one without the connection, to get rid of code duplication.
// TODO: Use generic player heads before the skins are loaded. Then refresh once they are.
// TODO: Add help menu for every command separately. Use that when a mistake was made.

/* To test:
 * - Make sure other users can get proper access to doors given to them.
 * - Make sure removing owners works properly.
 * - Make sure you cannot add the same owner more than once (both different and same permissions).
 * - Make sure you cannot add 0 or >2 permissions.
 * - Make sure owner with permission 0 cannot remove themselves as owner.
 * - Make sure other players cannot remove another user as owner.
 */

public class BigDoors extends JavaPlugin implements Listener
{
    private ToolVerifier                     tf;
    private SQLiteJDBCDriverConnection       db;
    private FallingBlockFactory_Vall       fabf;
    private ConfigLoader                 config;
    private String                       locale;
    private MyLogger                     logger;
    private SpigotUpdater               updater;
    private Metrics                     metrics;
    private File                        logFile;
    private Messages                   messages;
    private Commander                 commander;
    private Vector<WaitForCommand>   cmdWaiters;
    private DoorOpener               doorOpener;
    private Vector<BlockMover>      blockMovers;
    private BridgeOpener           bridgeOpener;
    private SlidingDoorOpener slidingDoorOpener;
    private boolean                validVersion;
    private CommandHandler       commandHandler;
    private RedstoneHandler     redstoneHandler;
    private PortcullisOpener   portcullisOpener;
    private ElevatorOpener       elevatorOpener;
    private String                  loginString;
    @SuppressWarnings("unused")
    private FlagOpener               flagOpener;
    private HashMap<UUID, ToolUser>   toolUsers;
    private ArrayList<ProtectionCompat> protectionCompats;
    //                 Chunk         Location, DoorUID
    private TimedCache<Long, HashMap<Long,     Long>> pbCache; // Powerblock cache.

    private HashMap<UUID, GUI> playerGUIs;

    private boolean              is1_13 = false;

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

        db                = new SQLiteJDBCDriverConnection(this, config.dbFile());
        commander         = new Commander(this, db);
        doorOpener        = new DoorOpener(this);
        flagOpener        = new FlagOpener(this);
        bridgeOpener      = new BridgeOpener(this);
        bridgeOpener      = new BridgeOpener(this);
        commandHandler    = new CommandHandler(this);
        elevatorOpener    = new ElevatorOpener(this);
        portcullisOpener  = new PortcullisOpener(this);
        slidingDoorOpener = new SlidingDoorOpener(this);

        registerCommand("inspectpowerblockloc");
        registerCommand("changepowerblockloc" );
        registerCommand("setautoclosetime"    );
        registerCommand("setdoorrotation"     );
        registerCommand("setblockstomove"     );
        registerCommand("newportcullis"       );
        registerCommand("toggledoor"          );
        registerCommand("pausedoors"          );
        registerCommand("closedoor"           );
        registerCommand("doordebug"           );
        registerCommand("listdoors"           );
        registerCommand("stopdoors"           );
        registerCommand("bdcancel"            );
        registerCommand("filldoor"            );
        registerCommand("doorinfo"            );
        registerCommand("opendoor"            );
        registerCommand("nameDoor"            );
        registerCommand("bigdoors"            );
        registerCommand("newdoor"             );
        registerCommand("deldoor"             );
        registerCommand("bdm"                 );

        liveDevelopmentLoad();
    }

    private void registerCommand(String command)
    {
        getCommand(command).setExecutor(new CommandHandler(this));
    }

    private void addProtectionCompat(ProtectionCompat hook)
    {
        if (hook.success())
        {
            protectionCompats.add(hook);
            logger.logMessageToConsole("Successfully hooked into \"" + hook.getPlugin().getName() + "\"!");
        }
        else
            logger.logMessageToConsole("Failed to hook into \"" + hook.getPlugin().getName() + "\"!");
    }

    private void init(boolean firstRun)
    {
        if (!validVersion)
            return;

        readConfigValues();
        messages    = new Messages(this);
        toolUsers   = new HashMap<>();
        playerGUIs  = new HashMap<>();
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
            protectionCompats.clear();
        }
        else
        {
            pbCache = new TimedCache<Long, HashMap<Long, Long>>(this, config.cacheTimeout());
            protectionCompats = new ArrayList<ProtectionCompat>();
        }

        if (config.plotSquaredHook() && getServer().getPluginManager().getPlugin("PlotSquared") != null)
        {
            try
            {
                ProtectionCompat plotSquaredCompat;
                String PSVersion = getServer().getPluginManager().getPlugin("PlotSquared").getDescription().getVersion();
                if (PSVersion.startsWith("4."))
                {
                    logger.logMessageToConsole("New PlotSquared version detected! Note that this hook is not yet implemented!");
                    plotSquaredCompat = new PlotSquaredNewProtectionCompat(this);
                }
                else
                {
                    logger.logMessageToConsole("Old PlotSquared version detected!");
                    plotSquaredCompat = new PlotSquaredOldProtectionCompat(this);
                }
                addProtectionCompat(plotSquaredCompat);
            }
            catch (NoClassDefFoundError e)
            {
                logger.logMessageToConsole("Failed to initialize PlotSquared compatibility hook! Perhaps this version isn't supported? Check error.log for more info!");
//                logger.logMessage(Util.errorToString(e), false, false);
                logger.logMessageToConsole("Now resuming normal startup with PlotSquared Compatibility Hook disabled!");
            }
            catch (Exception e)
            {
                logger.logMessageToConsole("Failed to initialize PlotSquared compatibility hook!");
                e.printStackTrace();
                logger.logMessageToConsole("Now resuming normal startup with PlotSquared Compatibility Hook disabled!");
            }
        }

        try
        {
            if (config.worldGuardHook() && getServer().getPluginManager().getPlugin("WorldGuard") != null)
                addProtectionCompat(new WorldGuard7ProtectionCompat(this));
        }
        catch (NoClassDefFoundError e)
        {
            logger.logMessageToConsole("Failed to initialize WorldGuard compatibility hook! Only v7 seems to be supported atm!"
                + " Maybe that's the issue?");
//            logger.logMessage(Util.errorToString(e), false, false);
            logger.logMessageToConsole("Now resuming normal startup with Worldguard Compatibility Hook disabled!");
        }
        catch (Exception e)
        {
            logger.logMessageToConsole("Failed to initialize WorldGuard compatibility hook!");
            e.printStackTrace();
            logger.logMessageToConsole("Now resuming normal startup with Worldguard Compatibility Hook disabled!");
        }

        if (config.enableRedstone())
        {
            redstoneHandler = new RedstoneHandler(this);
            Bukkit.getPluginManager().registerEvents(redstoneHandler, this);
        }
        // If the resourcepack is set to "NONE", don't load it.
        if (!config.resourcePack().equals("NONE"))
        {
            // If a resource pack was set for the current version of Minecraft, send that pack to the client on login.
            rPackHandler = new LoginResourcePackHandler(this, config.resourcePack());
            Bukkit.getPluginManager().registerEvents(rPackHandler,  this);
        }
        Bukkit.getPluginManager().registerEvents(new EventHandlers      (this), this);
        Bukkit.getPluginManager().registerEvents(new GUIHandler         (this), this);
        Bukkit.getPluginManager().registerEvents(new LoginMessageHandler(this), this);

        // Load stats collector if allowed, otherwise unload it if needed or simply don't load it in the first place.
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

    public boolean canBreakBlock(Player player, Location loc)
    {
        for (ProtectionCompat compat : protectionCompats)
            if (!compat.canBreakBlock(player, loc))
                return false;
        return true;
    }

    public boolean canBreakBlocksBetweenLocs(Player player, Location loc1, Location loc2)
    {
        for (ProtectionCompat compat : protectionCompats)
            if (!compat.canBreakBlocksBetweenLocs(player, loc1, loc2))
                return false;
        return true;
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
        playerGUIs.forEach((key,value) -> value.close());
        playerGUIs = null;
        init(false);
    }

    @Override
    public void onDisable()
    {
        if (!validVersion)
            return;

        // Stop all toolUsers and take all BigDoor tools from players.
        commander.setCanGo(false);

        toolUsers.forEach((key,value) -> value.setIsDone(true));

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
        case SLIDINGDOOR:
            return slidingDoorOpener;
        case ELEVATOR:
            return elevatorOpener;
        case FLAG:
        default:
            return null;
        }
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

    public ToolUser getToolUser(Player player)
    {
        ToolUser tu = null;
        if (toolUsers.containsKey(player.getUniqueId()))
            tu = toolUsers.get(player.getUniqueId());
        return tu;
    }

    public void addToolUser(ToolUser toolUser)
    {
        toolUsers.put(toolUser.getPlayer().getUniqueId(), toolUser);
    }

    public void removeToolUser(ToolUser toolUser)
    {
        toolUsers.remove(toolUser.getPlayer().getUniqueId());
    }

    public GUI getGUIUser(Player player)
    {
        GUI gui = null;
        if (playerGUIs.containsKey(player.getUniqueId()))
            gui = playerGUIs.get(player.getUniqueId());
        return gui;
    }

    public void addGUIUser(GUI gui)
    {
        playerGUIs.put(gui.getPlayer().getUniqueId(), gui);
    }

    public void removeGUIUser(GUI gui)
    {
        playerGUIs.remove(gui.getPlayer().getUniqueId());
    }

    public WaitForCommand getCommandWaiter(Player player)
    {
        for (WaitForCommand wfc : cmdWaiters)
            if (wfc.getPlayer().equals(player))
                return wfc;
        return null;
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
        if (version.equals("v1_11_R1"))
            fabf     = new FallingBlockFactory_V1_11_R1();
        else if (version.equals("v1_12_R1"))
        {
            fabf     = new FallingBlockFactory_V1_12_R1();
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
    private DoorOpenResult toggleDoor(Door door, double time, boolean instantOpen)
    {
        return getDoorOpener(door.getType()).openDoor(door, time, instantOpen, false);
    }

    // Toggle a door from a doorUID and instantly or not.
    public boolean toggleDoor(long doorUID, boolean instantOpen)
    {
        Door door = getCommander().getDoor(null, doorUID);
        return toggleDoor(door, 0.0, instantOpen) == DoorOpenResult.SUCCESS;
    }

    // Toggle a door from a doorUID and a given time.
    public boolean toggleDoor(long doorUID, double time)
    {
        Door door = getCommander().getDoor(null, doorUID);
        return toggleDoor(door, time, false) == DoorOpenResult.SUCCESS;
    }

    // Toggle a door from a doorUID using default values.
    public boolean toggleDoor(long doorUID)
    {
        Door door = getCommander().getDoor(null, doorUID);
        return toggleDoor(door, 0.0, false) == DoorOpenResult.SUCCESS;
    }

    // Check the open-status of a door.
    private boolean isOpen (Door door)
    {
        return door.isOpen();
    }

    // Check the open-status of a door from a doorUID.
    public boolean isOpen (long doorUID)
    {
        Door door = getCommander().getDoor(null, doorUID);
        return this.isOpen(door);
    }
}