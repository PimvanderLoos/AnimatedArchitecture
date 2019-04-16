package nl.pim16aap2.bigDoors;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import nl.pim16aap2.bigDoors.GUI.GUI;
import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory_Vall;
import nl.pim16aap2.bigDoors.NMS.v1_11_R1.FallingBlockFactory_V1_11_R1;
import nl.pim16aap2.bigDoors.NMS.v1_11_R1.SkullCreator_V1_11_R1;
import nl.pim16aap2.bigDoors.NMS.v1_12_R1.FallingBlockFactory_V1_12_R1;
import nl.pim16aap2.bigDoors.NMS.v1_12_R1.SkullCreator_V1_12_R1;
import nl.pim16aap2.bigDoors.NMS.v1_13_R1.FallingBlockFactory_V1_13_R1;
import nl.pim16aap2.bigDoors.NMS.v1_13_R1.SkullCreator_V1_13_R1;
import nl.pim16aap2.bigDoors.NMS.v1_13_R2.FallingBlockFactory_V1_13_R2;
import nl.pim16aap2.bigDoors.NMS.v1_13_R2.SkullCreator_V1_13_R2;
import nl.pim16aap2.bigDoors.compatiblity.FakePlayerCreator;
import nl.pim16aap2.bigDoors.compatiblity.ProtectionCompatManager;
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
// TODO: Make sure the abortable's BukkitTask isn't null. upgr
// TODO: Make invalid input stuff more informative (e.g. int, float etc).
// TODO: Improve recovering from invalid input. When people use a float instead of an int, cast to int.
// TODO: Split up SQL functions into 2: One taking a connection and one without the connection, to get rid of code duplication.
// TODO: Use generic player heads before the skins are loaded. Then refresh once they are.
// TODO: Add help menu for every command separately. Use that when a mistake was made.
// TODO: Add option to delete yourself as owner from a door you're not the creator of.
// TODO: SQL: Use nested statements.
// TODO: SQL: Use preparedStatements for everything (with values(?,?,?) etc).
// TODO: SQL: See if inserting into doors works when adding another question mark for the UUID (but leaving it empty).
//            Then +1 won't have to be appended to everything.
// TODO: SQL: Use proper COUNT operation for getting the number of doors.
// TODO: Implement TPS limit. Below a certain TPS, doors cannot be opened.


// TODO: Allow adding owners to doors from console.
// TODO: Catch NPE when listing doors from invalid name (e.g. Pim16aap2).
// TODO: DO NOT FUCKING AUTO DOWNGRADE WHEN RUNNING DEV BUILDS!!!!11 DISABLE auto updates when a dev build is
//       being used!
// TODO: For v5 of the database, make playerName a non-null attribute and if needed, update ALL players on another
//       thread, while locking the db in the meantime.
// TODO: Add new doortypes to config for speed configuration.
// TODO: Log startup error to log. Only load logger in onEnable, then try/catch to load the rest.
// TODO: Abort commandWaiter if someone used the command directly (e.g. /setblockstomove randomDoor 7" after initiating
//       the BTM process via the GUI).
// TODO: Create "creator" abstract class from which all creators can be derived, so the finishUp() method can
//       be safely used from all class types.
// TODO: Look into CommandHandler.setDoorOpenTime(Player, long, int) method. It either doesn't need a player object,
//       Or it needs to do some checks. There are a few more methods like this.
// TODO: Figure out what's up with chunbkHash == -1 for snail and elevator.
// TODO: When the plugin cannot be enabled, register alternative command handler that informs the player
//       that the plugin couldn't be enabled.
// TODO: Remove successful hooking from log file.
// TODO: Instead of always using global wallet for economy, use world wallet instead.
// TODO: Restarting while a toolusers is active fucks it up.

public class BigDoors extends JavaPlugin implements Listener
{
    public static final boolean DEVBUILD = true;


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
    private Commander                 commander = null;
    private Vector<WaitForCommand>   cmdWaiters;
    private DoorOpener               doorOpener;
    private Vector<BlockMover>      blockMovers;
    private BridgeOpener           bridgeOpener;
    private CommandHandler       commandHandler;
    private SlidingDoorOpener slidingDoorOpener;
    private PortcullisOpener   portcullisOpener;
    private RedstoneHandler     redstoneHandler;
    private ElevatorOpener       elevatorOpener;
    private boolean                validVersion;
    private String                  loginString;
    @SuppressWarnings("unused")
    private FlagOpener               flagOpener;
    private HashMap<UUID, ToolUser>   toolUsers;
    private HashMap<UUID, GUI>       playerGUIs;
    private boolean              is1_13 = false;
    private FakePlayerCreator fakePlayerCreator;

    private ProtectionCompatManager protCompatMan;
    private LoginResourcePackHandler rPackHandler;
    //                 Chunk         Location DoorUID
    private TimedCache<Long, HashMap<Long,    Long>> pbCache = null;
    private HeadManager headManager;

    private EconomyManager economyManager;

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

        fakePlayerCreator = new FakePlayerCreator();

        init();
        headManager.init();
        economyManager    = new EconomyManager(this);

        // No need to put these in init, as they should not be reloaded.
        pbCache           = new TimedCache<>(this, config.cacheTimeout());
        protCompatMan     = new ProtectionCompatManager(this);
        Bukkit.getPluginManager().registerEvents(protCompatMan, this);
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

    private void init()
    {
        if (!validVersion)
            return;

        readConfigValues();
        messages    = new Messages(this);
        toolUsers   = new HashMap<>();
        playerGUIs  = new HashMap<>();
        blockMovers = new Vector<>(2);
        cmdWaiters  = new Vector<>(2);
        tf          = new ToolVerifier(messages.getString("CREATOR.GENERAL.StickName"));
        loginString = "";

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

        if (commander != null)
            commander.setCanGo(true);
    }

    public boolean canBreakBlock(UUID playerUUID, Location loc)
    {
        return protCompatMan.canBreakBlock(playerUUID, loc);
    }

    public boolean canBreakBlocksBetweenLocs(UUID playerUUID, Location loc1, Location loc2)
    {
        return protCompatMan.canBreakBlocksBetweenLocs(playerUUID, loc1, loc2);
    }

    public void restart()
    {
        if (!validVersion)
            return;
        reloadConfig();
        // Stop all tool users, end all blockmovers, and clear all command waiters.
        toolUsers.forEach((key,value) -> value.abort());
        onDisable();
        protCompatMan.reload();
        toolUsers = null;
        cmdWaiters = null;
        blockMovers = null;
        playerGUIs.forEach((key,value) -> value.close());
        playerGUIs = null;

        HandlerList.unregisterAll(redstoneHandler);
        redstoneHandler = null;
        HandlerList.unregisterAll(rPackHandler);
        rPackHandler = null;

        init();

        economyManager.init();
        pbCache.reinit(config.cacheTimeout());
        headManager.reload();
    }

    @Override
    public void onDisable()
    {
        if (!validVersion)
            return;

        // Stop all toolUsers and take all BigDoor tools from players.
        commander.setCanGo(false);

        try
        {
            toolUsers.forEach((key,value) -> value.setIsDone(true));
        }
        // Don't care if it fails.
        catch (final Exception uncaught)
        {}

        for (final BlockMover bm : blockMovers)
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

    public FakePlayerCreator getFakePlayerCreator()
    {
        return fakePlayerCreator;
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
        for (final WaitForCommand wfc : cmdWaiters)
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

    public EconomyManager getEconomyManager()
    {
        return economyManager;
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
    @SuppressWarnings("unused")
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
        catch (final ArrayIndexOutOfBoundsException useAVersionMentionedInTheDescriptionPleaseException)
        {
            return false;
        }

        fabf  = null;
        if (version.equals("v1_11_R1"))
        {
            fabf        = new FallingBlockFactory_V1_11_R1();
            headManager = new SkullCreator_V1_11_R1(this);
        }
        else if (version.equals("v1_12_R1"))
        {
            fabf        = new FallingBlockFactory_V1_12_R1();
            headManager = new SkullCreator_V1_12_R1(this);
        }
        else if (version.equals("v1_13_R1"))
        {
            is1_13      = true;
            fabf        = new FallingBlockFactory_V1_13_R1();
            headManager = new SkullCreator_V1_13_R1(this);
        }
        else if (version.equals("v1_13_R2"))
        {
            is1_13      = true;
            fabf        = new FallingBlockFactory_V1_13_R2();
            headManager = new SkullCreator_V1_13_R2(this);
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

    public ItemStack getPlayerHead(UUID playerUUID, String playerName, int x, int y, int z, Player player)
    {
        return headManager.getPlayerHead(playerUUID, playerName, x, y, z, player);
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
        final Door door = getCommander().getDoor(null, doorUID);
        return toggleDoor(door, 0.0, instantOpen) == DoorOpenResult.SUCCESS;
    }

    // Toggle a door from a doorUID and a given time.
    public boolean toggleDoor(long doorUID, double time)
    {
        final Door door = getCommander().getDoor(null, doorUID);
        return toggleDoor(door, time, false) == DoorOpenResult.SUCCESS;
    }

    // Toggle a door from a doorUID using default values.
    public boolean toggleDoor(long doorUID)
    {
        final Door door = getCommander().getDoor(null, doorUID);
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
        final Door door = getCommander().getDoor(null, doorUID);
        return this.isOpen(door);
    }

//    public long createNewDoor(Location min, Location max, Location engine,
//                              Location powerBlock, DoorType type, )
}