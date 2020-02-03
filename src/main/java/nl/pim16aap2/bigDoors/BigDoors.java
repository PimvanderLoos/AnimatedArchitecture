package nl.pim16aap2.bigDoors;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
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
import nl.pim16aap2.bigDoors.NMS.v1_14_R1.FallingBlockFactory_V1_14_R1;
import nl.pim16aap2.bigDoors.NMS.v1_14_R1.SkullCreator_V1_14_R1;
import nl.pim16aap2.bigDoors.NMS.v1_15_R1.FallingBlockFactory_V1_15_R1;
import nl.pim16aap2.bigDoors.NMS.v1_15_R1.SkullCreator_V1_15_R1;
import nl.pim16aap2.bigDoors.compatiblity.FakePlayerCreator;
import nl.pim16aap2.bigDoors.compatiblity.ProtectionCompatManager;
import nl.pim16aap2.bigDoors.handlers.ChunkUnloadHandler;
import nl.pim16aap2.bigDoors.handlers.CommandHandler;
import nl.pim16aap2.bigDoors.handlers.EventHandlers;
import nl.pim16aap2.bigDoors.handlers.GUIHandler;
import nl.pim16aap2.bigDoors.handlers.LoginMessageHandler;
import nl.pim16aap2.bigDoors.handlers.LoginResourcePackHandler;
import nl.pim16aap2.bigDoors.handlers.RedstoneHandler;
import nl.pim16aap2.bigDoors.moveBlocks.BridgeOpener;
import nl.pim16aap2.bigDoors.moveBlocks.DoorOpener;
import nl.pim16aap2.bigDoors.moveBlocks.ElevatorOpener;
import nl.pim16aap2.bigDoors.moveBlocks.FlagOpener;
import nl.pim16aap2.bigDoors.moveBlocks.Opener;
import nl.pim16aap2.bigDoors.moveBlocks.PortcullisOpener;
import nl.pim16aap2.bigDoors.moveBlocks.SlidingDoorOpener;
import nl.pim16aap2.bigDoors.moveBlocks.Bridge.getNewLocation.GetNewLocationEast;
import nl.pim16aap2.bigDoors.moveBlocks.Bridge.getNewLocation.GetNewLocationNorth;
import nl.pim16aap2.bigDoors.moveBlocks.Bridge.getNewLocation.GetNewLocationSouth;
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
import nl.pim16aap2.bigDoors.util.Util;
import nl.pim16aap2.bigDoors.waitForCommand.WaitForCommand;

public class BigDoors extends JavaPlugin implements Listener
{
    private static BigDoors instance;
    public static final boolean DEVBUILD = true;
    private int buildNumber = -1;

    public static final int MINIMUMDOORDELAY = 15;

    private ToolVerifier tf;
    private SQLiteJDBCDriverConnection db;
    private FallingBlockFactory_Vall fabf;
    private ConfigLoader config;
    private String locale;
    private MyLogger logger;
    private Metrics metrics;
    private File logFile;
    private Messages messages;
    private Commander commander = null;
    private DoorOpener doorOpener;
    private BridgeOpener bridgeOpener;
    private CommandHandler commandHandler;
    private SlidingDoorOpener slidingDoorOpener;
    private PortcullisOpener portcullisOpener;
    private RedstoneHandler redstoneHandler;
    private ElevatorOpener elevatorOpener;
    private boolean validVersion;
    private FlagOpener flagOpener;
    private HashMap<UUID, ToolUser> toolUsers;
    private HashMap<UUID, GUI> playerGUIs;
    private HashMap<UUID, WaitForCommand> cmdWaiters;
    private boolean is1_13 = false;
    private FakePlayerCreator fakePlayerCreator;
    private AutoCloseScheduler autoCloseScheduler;
    private ProtectionCompatManager protCompatMan;
    private LoginResourcePackHandler rPackHandler;
    private TimedCache<Long /* Chunk */, HashMap<Long /* Loc */, Long /* doorUID */>> pbCache = null;
    private HeadManager headManager;
    private VaultManager vaultManager;
    private UpdateManager updateManager;
    private static final MCVersion mcVersion = BigDoors.calculateMCVersion();

    @Override
    public void onEnable()
    {
        instance = this;

        logFile = new File(getDataFolder(), "log.txt");
        logger = new MyLogger(this, logFile);
        updateManager = new UpdateManager(this, 58669);
        if (DEVBUILD)
        {
            buildNumber = readBuildNumber();
            logger.logMessageToConsoleOnly("WARNING! You are running a devbuild (build: " + buildNumber + ")! "
                + "Update checking + downloading has been enabled (overrides config options)!");
        }

        try
        {
            Bukkit.getPluginManager().registerEvents(new LoginMessageHandler(this), this);

            validVersion = compatibleMCVer();
            // Load the files for the correct version of Minecraft.
            if (!validVersion)
            {
                logger.logMessage("Trying to load the plugin on an incompatible version of Minecraft! (\""
                    + (Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3])
                    + "\"). This plugin will NOT be enabled!", true, true);
                return;
            }

            fakePlayerCreator = new FakePlayerCreator(this);

            init();

            headManager.init();
            vaultManager = new VaultManager(this);
            autoCloseScheduler = new AutoCloseScheduler(this);

            Bukkit.getPluginManager().registerEvents(new EventHandlers(this), this);
            Bukkit.getPluginManager().registerEvents(new GUIHandler(this), this);
            Bukkit.getPluginManager().registerEvents(new ChunkUnloadHandler(this), this);

            // No need to put these in init, as they should not be reloaded.
            pbCache = new TimedCache<>(this, config.cacheTimeout());
            protCompatMan = new ProtectionCompatManager(this);
            Bukkit.getPluginManager().registerEvents(protCompatMan, this);
            db = new SQLiteJDBCDriverConnection(this, config.dbFile());
            commander = new Commander(this, db);
            doorOpener = new DoorOpener(this);
            flagOpener = new FlagOpener(this);
            bridgeOpener = new BridgeOpener(this);
            commandHandler = new CommandHandler(this);
            elevatorOpener = new ElevatorOpener(this);
            portcullisOpener = new PortcullisOpener(this);
            slidingDoorOpener = new SlidingDoorOpener(this);

            registerCommand("recalculatepowerblocks");
            registerCommand("killbigdoorsentities");
            registerCommand("inspectpowerblockloc");
            registerCommand("changepowerblockloc");
            registerCommand("setautoclosetime");
            registerCommand("shadowtoggledoor");
            registerCommand("setdoorrotation");
            registerCommand("setblockstomove");
            registerCommand("newportcullis");
            registerCommand("toggledoor");
            registerCommand("pausedoors");
            registerCommand("closedoor");
            registerCommand("doordebug");
            registerCommand("listdoors");
            registerCommand("stopdoors");
            registerCommand("bdcancel");
            registerCommand("filldoor");
            registerCommand("doorinfo");
            registerCommand("opendoor");
            registerCommand("nameDoor");
            registerCommand("bigdoors");
            registerCommand("newdoor");
            registerCommand("deldoor");
            registerCommand("bdm");

            liveDevelopmentLoad();
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
            logger.logMessage(Util.exceptionToString(exception), true, true);
        }
        overrideVersion();
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
        messages = new Messages(this);
        toolUsers = new HashMap<>();
        playerGUIs = new HashMap<>();
        cmdWaiters = new HashMap<>();
        tf = new ToolVerifier(messages.getString("CREATOR.GENERAL.StickName"));

        if (config.enableRedstone())
        {
            redstoneHandler = new RedstoneHandler(this);
            Bukkit.getPluginManager().registerEvents(redstoneHandler, this);
        }

        if (!config.resourcePackEnabled())
        {
            // If a resource pack was set for the current version of Minecraft, send that
            // pack to the client on login.
            rPackHandler = new LoginResourcePackHandler(this, config.resourcePack());
            Bukkit.getPluginManager().registerEvents(rPackHandler, this);
        }

        // Load stats collector if allowed, otherwise unload it if needed or simply
        // don't load it in the first place.
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
            logger
                .myLogger(Level.INFO,
                          "Stats disabled, not laoding stats :(... Please consider enabling it! I am a simple man, seeing higher user numbers helps me stay motivated!");
        }

        updateManager.setEnabled(getConfigLoader().checkForUpdates(), getConfigLoader().autoDLUpdate());

        if (commander != null)
            commander.setCanGo(true);
    }

    public static final BigDoors get()
    {
        return instance;
    }

    public void onPlayerLogout(final Player player)
    {
        WaitForCommand cw = getCommandWaiter(player);
        if (cw != null)
            cw.abortSilently();

        playerGUIs.remove(player.getUniqueId());
        ToolUser tu = getToolUser(player);
        if (tu != null)
            tu.abortSilently();
    }

    public String canBreakBlock(UUID playerUUID, Location loc)
    {
        return protCompatMan.canBreakBlock(playerUUID, loc);
    }

    public String canBreakBlocksBetweenLocs(UUID playerUUID, Location loc1, Location loc2)
    {
        return protCompatMan.canBreakBlocksBetweenLocs(playerUUID, loc1, loc2);
    }

    public void restart()
    {
        if (!validVersion)
            return;
        reloadConfig();

        onDisable();
        protCompatMan.restart();
        playerGUIs.forEach((key, value) -> value.close());
        playerGUIs.clear();

        HandlerList.unregisterAll(redstoneHandler);
        redstoneHandler = null;
        HandlerList.unregisterAll(rPackHandler);
        rPackHandler = null;

        init();

        vaultManager.init();
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
        commander.stopMovers();

        Iterator<Entry<UUID, ToolUser>> it = toolUsers.entrySet().iterator();
        while (it.hasNext())
        {
            Entry<UUID, ToolUser> entry = it.next();
            entry.getValue().abort();
        }

        toolUsers.clear();
        cmdWaiters.clear();
    }

    public String getUpdateURL()
    {
        return null;
    }

    public String getLoginMessage()
    {
        String ret = "";
        if (DEVBUILD)
            ret += "[BigDoors] Warning: You are running a devbuild!\n";
        if (updateManager.updateAvailable())
        {
            if (getConfigLoader().autoDLUpdate() && updateManager.hasUpdateBeenDownloaded())
                ret += "[BigDoors] A new update (" + updateManager.getNewestVersion() + ") has been downloaded! "
                    + "Restart your server to apply the update!\n";
            else if (updateManager.updateAvailable())
                ret += "[BigDoors] A new update is available: " + updateManager.getNewestVersion() + "\n";
        }
        return ret;
    }

    public UpdateManager getUpdateManager()
    {
        return updateManager;
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

    public AutoCloseScheduler getAutoCloseScheduler()
    {
        return autoCloseScheduler;
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
            return flagOpener;
        default:
            return null;
        }
    }

    public ToolUser getToolUser(Player player)
    {
        return toolUsers.get(player.getUniqueId());
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
        return playerGUIs.get(player.getUniqueId());
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
//        if (cmdWaiters.containsKey(player.getUniqueId()))
//            return cmdWaiters.get(player.getUniqueId());
//        return null;
        return cmdWaiters.get(player.getUniqueId());
    }

    public void addCommandWaiter(WaitForCommand cmdWaiter)
    {
        cmdWaiters.put(cmdWaiter.getPlayer().getUniqueId(), cmdWaiter);
    }

    public void removeCommandWaiter(WaitForCommand cmdWaiter)
    {
        cmdWaiters.remove(cmdWaiter.getPlayer().getUniqueId());
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

    public VaultManager getVaultManager()
    {
        return vaultManager;
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

    public static MCVersion getMCVersion()
    {
        return mcVersion;
    }

    private void readConfigValues()
    {
        // Load the settings from the config file.
        config = new ConfigLoader(this);
        locale = config.languageFile();
    }

    // This function simply loads these classes to make my life a bit less hell-ish
    // with live development.
    @SuppressWarnings("unused")
    private void liveDevelopmentLoad()
    {
        new GetNewLocationNorth();
        new GetNewLocationEast();
        new GetNewLocationSouth();
        new GetNewLocationWest();
        commandHandler.stopDoors();
    }

    public boolean is1_13()
    {
        return is1_13;
    }

    private static MCVersion calculateMCVersion()
    {
        String version;

        try
        {
            version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
        }
        catch (final ArrayIndexOutOfBoundsException useAVersionMentionedInTheDescriptionPleaseException)
        {
            useAVersionMentionedInTheDescriptionPleaseException.printStackTrace();
            return null;
        }

        MCVersion mcVersion = null;
        if (version.equals("v1_11_R1"))
            mcVersion = MCVersion.v1_11;
        else if (version.equals("v1_12_R1"))
            mcVersion = MCVersion.v1_12;
        else if (version.equals("v1_13_R1"))
            mcVersion = MCVersion.v1_13;
        else if (version.equals("v1_13_R2"))
            mcVersion = MCVersion.v1_13;
        else if (version.equals("v1_14_R1"))
            mcVersion = MCVersion.v1_14;
        else if (version.equals("v1_15_R1"))
            mcVersion = MCVersion.v1_15;
        return mcVersion;
    }

    // Check + initialize for the correct version of Minecraft.
    private boolean compatibleMCVer()
    {
        String version;

        try
        {
            version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
        }
        catch (final ArrayIndexOutOfBoundsException useAVersionMentionedInTheDescriptionPleaseException)
        {
            useAVersionMentionedInTheDescriptionPleaseException.printStackTrace();
            return false;
        }

        fabf = null;
        if (version.equals("v1_11_R1"))
        {
            fabf = new FallingBlockFactory_V1_11_R1();
            headManager = new SkullCreator_V1_11_R1(this);
        }
        else if (version.equals("v1_12_R1"))
        {
            fabf = new FallingBlockFactory_V1_12_R1();
            headManager = new SkullCreator_V1_12_R1(this);
        }
        else if (version.equals("v1_13_R1"))
        {
            is1_13 = true;
            fabf = new FallingBlockFactory_V1_13_R1();
            headManager = new SkullCreator_V1_13_R1(this);
        }
        else if (version.equals("v1_13_R2"))
        {
            is1_13 = true;
            fabf = new FallingBlockFactory_V1_13_R2();
            headManager = new SkullCreator_V1_13_R2(this);
        }
        else if (version.equals("v1_14_R1"))
        {
            is1_13 = true; // Yeah, it's actually 1.14, but it still needs to use new stuff.
            fabf = new FallingBlockFactory_V1_14_R1();
            headManager = new SkullCreator_V1_14_R1(this);
        }
        else if (version.equals("v1_15_R1"))
        {
            is1_13 = true; // Yeah, it's actually 1.15, but it still needs to use new stuff.
            fabf = new FallingBlockFactory_V1_15_R1();
            headManager = new SkullCreator_V1_15_R1(this);
        }
        // Return true if compatible.
        return fabf != null;
    }

    public ItemStack getPlayerHead(UUID playerUUID, String playerName, int x, int y, int z, Player player)
    {
        return headManager.getPlayerHead(playerUUID, playerName, x, y, z, player);
    }

    private int readBuildNumber()
    {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass()
            .getResourceAsStream("/build.number"))))
        {
            for (int idx = 0; idx != 2; ++idx)
                reader.readLine();
            return Integer.parseInt(reader.readLine().replace("build.number=", ""));
        }
        catch (Exception e)
        {
            return -1;
        }
    }

    private void overrideVersion()
    {
        if (!DEVBUILD)
            return;
        try
        {
            String version = getDescription().getVersion() + " (b" + buildNumber + ")";
            final Field field = PluginDescriptionFile.class.getDeclaredField("version");
            field.setAccessible(true);
            field.set(getDescription(), version);
        }
        catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e)
        {
            getMyLogger().logMessage(Util.exceptionToString(e), true, false);
        }
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
    private boolean isOpen(Door door)
    {
        return door.isOpen();
    }

    // Check the open-status of a door from a doorUID.
    public boolean isOpen(long doorUID)
    {
        final Door door = getCommander().getDoor(null, doorUID);
        return this.isOpen(door);
    }

    public int getMinimumDoorDelay()
    {
        return MINIMUMDOORDELAY;
    }

    /**
     * @return
     */
    public RedstoneHandler getRedstoneHandler()
    {
        return redstoneHandler;
    }

    public enum MCVersion
    {
        v1_11,
        v1_12,
        v1_13,
        v1_14,
        v1_15
    }

//    public long createNewDoor(Location min, Location max, Location engine,
//                              Location powerBlock, DoorType type, )
}
