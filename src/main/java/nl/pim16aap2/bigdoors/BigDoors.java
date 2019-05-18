package nl.pim16aap2.bigdoors;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import nl.pim16aap2.bigdoors.commands.CommandBigDoors;
import nl.pim16aap2.bigdoors.commands.CommandManager;
import nl.pim16aap2.bigdoors.commands.CommandMenu;
import nl.pim16aap2.bigdoors.commands.ICommand;
import nl.pim16aap2.bigdoors.commands.SuperCommand;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandAddOwner;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandCancel;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandChangePowerBlock;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandClose;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandDebug;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandDelete;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandFillDoor;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandInfo;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandInspectPowerBlock;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandListDoors;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandMenu;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandNameDoor;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandNew;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandOpen;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandPause;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandRemoveOwner;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandRestart;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandSetAutoCloseTime;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandSetBlocksToMove;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandSetRotation;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandStop;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandToggle;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandVersion;
import nl.pim16aap2.bigdoors.compatiblity.ProtectionCompatManager;
import nl.pim16aap2.bigdoors.gui.GUI;
import nl.pim16aap2.bigdoors.handlers.ChunkUnloadHandler;
import nl.pim16aap2.bigdoors.handlers.EventHandlers;
import nl.pim16aap2.bigdoors.handlers.GUIHandler;
import nl.pim16aap2.bigdoors.handlers.LoginMessageHandler;
import nl.pim16aap2.bigdoors.handlers.LoginResourcePackHandler;
import nl.pim16aap2.bigdoors.handlers.RedstoneHandler;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.moveblocks.BridgeOpener;
import nl.pim16aap2.bigdoors.moveblocks.DoorOpener;
import nl.pim16aap2.bigdoors.moveblocks.ElevatorOpener;
import nl.pim16aap2.bigdoors.moveblocks.FlagOpener;
import nl.pim16aap2.bigdoors.moveblocks.Opener;
import nl.pim16aap2.bigdoors.moveblocks.PortcullisOpener;
import nl.pim16aap2.bigdoors.moveblocks.SlidingDoorOpener;
import nl.pim16aap2.bigdoors.nms.FallingBlockFactory_Vall;
import nl.pim16aap2.bigdoors.nms.v1_14_R1.FallingBlockFactory_V1_14_R1;
import nl.pim16aap2.bigdoors.nms.v1_14_R1.SkullCreator_V1_14_R1;
import nl.pim16aap2.bigdoors.storage.sqlite.SQLiteJDBCDriverConnection;
import nl.pim16aap2.bigdoors.toolusers.ToolUser;
import nl.pim16aap2.bigdoors.toolusers.ToolVerifier;
import nl.pim16aap2.bigdoors.util.ConfigLoader;
import nl.pim16aap2.bigdoors.util.DoorOpenResult;
import nl.pim16aap2.bigdoors.util.DoorType;
import nl.pim16aap2.bigdoors.util.IRestartable;
import nl.pim16aap2.bigdoors.util.Messages;
import nl.pim16aap2.bigdoors.util.Metrics;
import nl.pim16aap2.bigdoors.util.TimedCache;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForCommand;

/*
 * General
 */
// TODO: Put @Nullable everywhere where applicable.
// TODO: Put all listeners and command stuff (basically everything users can interact with) in try-catch blocks, so I can
//       Dump more errors into the log file.
// TODO: Add command to upload log file to PasteBin or something.
// TODO: Catch specific exceptions in update checker. Or at least ssl exception, it's very spammy.
// TODO: Implement TPS limit. Below a certain TPS, doors cannot be opened.
//       double tps = ((CraftServer) Bukkit.getServer()).getServer().recentTps[0]; // 3 values: last 1, 5, 15 mins.
// TODO: Add javadoc (@ param) stuff etc to "api" and replace any method comment by jdoc stuff.
// TODO: Split up project POM into modules. Use API to get all implementation (Bukkit, Forge, different versions) specific stuff.
//       https://bukkit.org/threads/support-multiple-minecraft-versions-with-abstraction-maven.115810/
// TODO: When using HashMaps/HashTables, make sure that the keys are actually properly hashable. By default the hashCode method returns 1,
//       So lookup becomes linear, so that's just a waste of performance.
// TODO: Update version checker. Start using the new format. Also, decide what the new format will be. R200? 200R? %100 = Stable?
// TODO: Version-specific isAllowedBlock etc.
// TODO: Instead of a commander, use a database manager. Or nothing at all.
// TODO: Use Event for restart command. Then let stuff that needs to do stuff on restart subscribe.
// TODO: Rename RotateDirection to moveDirection. Lifts don't rotate. They lift.
// TODO: Update rotatable blocks after finishing rotation etc.
// TODO: Cache all doors. Instead of constantly getting updated doors etc from the database, just get a cached door. 
//       This also means that a set of owners (and their permission) should be stored in a door object. 
//       Add a sync() method to sync any changes to the door with the database or send changes via commander. Might be faster.

/*
 * GUI
 */
// TODO: Look into playerheads for GUI buttons. Example: https://minecraft-heads.com/player-heads/alphabet/2762-arrow-left
// TODO: Make GUI options always use the correct subCommand.
// TODO: Get rid of repeated initialization for stuff like the options in the GUIPageDoorInfo. Just initialize it once in the constructor.

/*
 * SQL
 */
// TODO: Split up SQL functions into 2: One taking a connection and one without the connection, to get rid of code duplication.
// TODO: Use nested statements.
// TODO: Use preparedStatements for everything (with values(?,?,?) etc).
// TODO: See if inserting into doors works when adding another question mark for the UUID (but leaving it empty).
//       Then +1 won't have to be appended to everything.
// TODO: Use proper COUNT operation for getting the number of doors.

/*
 * Commands
 */
// TODO: Add help menu for every command separately. Use that when a mistake was made.
// TODO: Add /BDM [PlayerName (when online) || PlayerUUID || Server] to open a doorMenu for a specific player
// TODO: Make invalid input stuff more informative (e.g. int, float etc).
// TODO: Improve recovering from invalid input. When people use a float instead of an int, cast to int.
// TODO: Allow adding owners to doors from console.
// TODO: When the plugin fails to initialize properly, register alternative command handler to display this info.
// TODO: Differentiate between cancelling and timing out on an abortable.
// TODO: Move stuff such as StartTimerForAbortable into appropriate command classes.
// TODO: When retrieving player argument (SubCommandAddOwner, SubCommandListPlayerDoors) don't just try to convert
//       the provided playerArg to a UUID. That's not very user friendly at all.
// TODO: Add CommandNotFoundException for when a subCommand could not be found.
// TODO: Fix openndoor <uid> as a user still returning that it could not be found, even if it was found.
// TODO: Test permissions.
// TODO: Check if force unlock door as admin still exists.
// TODO: Create enum of commands, so I don't have to use strings.
// TODO: Add "success()" method to commands. To print messages like "you've successfully deleted the door!" etc.
// TODO: Do not use the commander for anything command-related that isn't strict database abstraction.
// TODO: Modify the system so that you can have as many subcommands as you want.

/*
 * Openers / Movers
 */
// TODO: Rewrite Openers to get rid of code duplication.
// TODO: Use lambda for block movement to get rid of code duplication (all the iterators).
// TODO: When a door isn't set to open in a specific direction and therefor naively tries to find the first possible
//       free location, automatically set the openDirection for this door. HOWEVER, this value must be reset when it is
//       closed again, but ONLY if it used to be unset. So instead of storing value, add flag for un/intentionally set.
// TODO: Rotate Sea Pickle and turtle egg.
// TODO: Don't do any replacing by air stuff in the openers/movers. Instead, do it in the NMSBlock part. Also make sure
//       to copy all rotational blockdata stuff properly!
// TODO: Add door opening/closing scheduler. This should 
// TODO: Schedule doorAutoCloseTimer properly. When it is closed manually, the timer should be cancelled!

/*
 * ToolUsers
 */
// TODO: Make sure the abortable's BukkitTask isn't null. -When would it be?? What is this?
// TODO: Make sure timers don't give an error when the player disconnects before finishing it.
// TODO: Maybe check all player inventories on login/logout to make sure they don't have any leftover creators sticks.
// TODO: Create "creator" abstract class as subclass of ToolUser from which all creators can be derived, so
//       the finishUp() method can be safely used from all class types.





/* "Rewrite" todo list

- Get rid of the commander.
- Get rid of the command handler.
- Give every GUI page its own class.
- Rewrite all block movers etc.
  - When rewriting, make sure that absolutely 0 implementation-specific code ends up in the new movers.
- Use Maven modules to be able to support multiple versions and perhaps even Forge / Sponge / whatever.
- Clean up SQL class. Try to move as many shared items to private classes and/or use single statements.
- Allow 1-wide drawbridges. Finally.
 */

public class BigDoors extends JavaPlugin implements Listener
{
    public static final boolean DEVBUILD = true;


    private ToolVerifier                     tf;
    private SQLiteJDBCDriverConnection       db;
    private FallingBlockFactory_Vall       fabf;
    private ConfigLoader                 config;
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
//    private CommandHandler       commandHandler;
    private SlidingDoorOpener slidingDoorOpener;
    private PortcullisOpener   portcullisOpener;
    private RedstoneHandler     redstoneHandler;
    private ElevatorOpener       elevatorOpener;
    private boolean                validVersion;
    private String                  loginString;
    private CommandManager       commandManager;
    @SuppressWarnings("unused")
    private FlagOpener               flagOpener;
    private HashMap<UUID, ToolUser>   toolUsers;
    private HashMap<UUID, GUI>       playerGUIs;
    private List<IRestartable>     restartables;
    private boolean              is1_13 = false;

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

        try
        {
            Bukkit.getPluginManager().registerEvents(new LoginMessageHandler(this), this);
            if (DEVBUILD)
                setLoginString("[BigDoors] Warning: You are running a devbuild!");

            validVersion = compatibleMCVer();
            // Load the files for the correct version of Minecraft.
            if (!validVersion)
            {
                logger.logMessage("Trying to load the plugin on an incompatible version of Minecraft! (\""  +
                    (Bukkit.getServer().getClass().getPackage().getName().replace(".",  ",").split(",")[3]) +
                                  "\"). This plugin will NOT be enabled!", true, true);
                return;
            }

            restartables = new ArrayList<>();

            init();
            headManager.init();
            economyManager    = new EconomyManager(this);

            Bukkit.getPluginManager().registerEvents(new EventHandlers      (this), this);
            Bukkit.getPluginManager().registerEvents(new GUIHandler         (this), this);
            Bukkit.getPluginManager().registerEvents(new ChunkUnloadHandler (this), this);
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
            elevatorOpener    = new ElevatorOpener(this);
            portcullisOpener  = new PortcullisOpener(this);
            slidingDoorOpener = new SlidingDoorOpener(this);


            commandManager = new CommandManager(this);
            SuperCommand commandBigDoors = new CommandBigDoors(this, commandManager);
            {
                commandBigDoors.registerSubCommand(new SubCommandAddOwner(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandCancel(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandChangePowerBlock(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandClose(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandDebug(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandDelete(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandFillDoor(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandInfo(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandInspectPowerBlock(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandListDoors(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandMenu(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandNameDoor(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandNew(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandOpen(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandPause(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandRemoveOwner(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandRestart(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandSetAutoCloseTime(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandSetBlocksToMove(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandSetRotation(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandStop(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandToggle(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandVersion(this, commandManager));
            }
            commandManager.registerCommand(commandBigDoors);
            commandManager.registerCommand(new CommandMenu(this, commandManager));
        }
        catch(Exception exception)
        {
            exception.printStackTrace();
            logger.logMessage(Util.exceptionToString(exception), true, true);
        }
    }

    @SuppressWarnings("unused")
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
        if (config.checkForUpdates() && !DEVBUILD)
        {
            if (updater == null)
                updater = new SpigotUpdater(this, 58669);
        }
        else
            updater = null;

        if (commander != null)
            commander.setCanGo(true);
    }

    public ICommand getCommand(String name, String ...subCommandNames)
    {
        ICommand command = commandManager.getCommand(name);
        for (String subCommandName : subCommandNames)
            command = ((SuperCommand) command).getCommand(subCommandName);
        return command;
    }

    public void addCommandWaiter(final Class<WaitForCommand> waiterClass)
    {
        try
        {
            this.addCommandWaiter(waiterClass.newInstance());
        }
        catch (InstantiationException e)
        {
            handleMyStackTrace(new MyException(e));
        }
        catch (IllegalAccessException e)
        {
            handleMyStackTrace(new MyException(e));
        }
    }

    public boolean canBreakBlock(UUID playerUUID, Location loc)
    {
        return protCompatMan.canBreakBlock(playerUUID, loc);
    }

    public boolean canBreakBlocksBetweenLocs(UUID playerUUID, Location loc1, Location loc2)
    {
        return protCompatMan.canBreakBlocksBetweenLocs(playerUUID, loc1, loc2);
    }

    public void registerRestartable(IRestartable restartable)
    {
        restartables.add(restartable);
    }

    public void restart()
    {
        if (!validVersion)
            return;
        reloadConfig();

        onDisable();
        playerGUIs.forEach((key,value) -> value.close());
        playerGUIs.clear();

        HandlerList.unregisterAll(redstoneHandler);
        redstoneHandler = null;
        HandlerList.unregisterAll(rPackHandler);
        rPackHandler = null;

        init();

        restartables.forEach((K) -> K.restart());
    }

    @Override
    public void onDisable()
    {
        if (!validVersion)
            return;

        // Stop all toolUsers and take all BigDoor tools from players.
        commander.setCanGo(false);

        Iterator<Entry<UUID, ToolUser>> it = toolUsers.entrySet().iterator();
        while (it.hasNext())
        {
            Entry<UUID, ToolUser> entry = it.next();
            entry.getValue().abort();
        }

        for (final BlockMover bm : blockMovers)
            bm.putBlocks(true);

        toolUsers.clear();
        cmdWaiters.clear();
        blockMovers.clear();
    }

    public void dumpStackTrace(@Nullable String message)
    {
        getMyLogger().logMessage((message == null ? "" : message + "\n") +
                                 Arrays.toString((new Exception()).getStackTrace()), true, true);
    }

    public void handleMyStackTrace(MyException e)
    {
        if (e.hasWarningMessage())
            getMyLogger().warn(e.getWarningMessage());
        e.printStackTrace();
        getMyLogger().logMessageToLogFile(Util.exceptionToString(e));
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

    public boolean isPlayerBusy(Player player)
    {
        boolean isBusy = (getToolUser(player) != null || isCommandWaiter(player) != null);
        if (isBusy)
            Util.messagePlayer(player, getMessages().getString("GENERAL.IsBusy"));
        return isBusy;
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

    public WaitForCommand isCommandWaiter(Player player)
    {
        for (WaitForCommand cw : cmdWaiters)
            if (cw.getPlayer() == player)
                return cw;
        return null;
    }

    public void addCommandWaiter(WaitForCommand cmdWaiter)
    {
        cmdWaiters.add(cmdWaiter);
    }

    public void removeCommandWaiter(WaitForCommand cmdWaiter)
    {
        cmdWaiters.remove(cmdWaiter);
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

    private void readConfigValues()
    {
        // Load the settings from the config file.
        config = new ConfigLoader(this);
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
            useAVersionMentionedInTheDescriptionPleaseException.printStackTrace();
            return false;
        }

        fabf  = null;
        if (version.equals("v1_14_R1"))
        {
            is1_13      = true; // Yeah, it's actually 1.14, but it still needs to use new stuff.
            fabf        = new FallingBlockFactory_V1_14_R1();
            headManager = new SkullCreator_V1_14_R1(this);
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
