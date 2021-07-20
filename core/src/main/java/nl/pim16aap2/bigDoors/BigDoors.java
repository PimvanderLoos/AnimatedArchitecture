package nl.pim16aap2.bigDoors;

import nl.pim16aap2.bigDoors.GUI.GUI;
import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory;
import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory_V1_11_R1;
import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory_V1_12_R1;
import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory_V1_13_R1;
import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory_V1_13_R1_5;
import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory_V1_13_R2;
import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory_V1_14_R1;
import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory_V1_15_R1;
import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory_V1_16_R1;
import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory_V1_16_R2;
import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory_V1_16_R3;
import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory_V1_17_R1;
import nl.pim16aap2.bigDoors.NMS.SkullCreator_V1_11_R1;
import nl.pim16aap2.bigDoors.NMS.SkullCreator_V1_12_R1;
import nl.pim16aap2.bigDoors.NMS.SkullCreator_V1_13_R1;
import nl.pim16aap2.bigDoors.NMS.SkullCreator_V1_13_R1_5;
import nl.pim16aap2.bigDoors.NMS.SkullCreator_V1_13_R2;
import nl.pim16aap2.bigDoors.NMS.SkullCreator_V1_14_R1;
import nl.pim16aap2.bigDoors.NMS.SkullCreator_V1_15_R1;
import nl.pim16aap2.bigDoors.NMS.SkullCreator_V1_16_R1;
import nl.pim16aap2.bigDoors.NMS.SkullCreator_V1_16_R2;
import nl.pim16aap2.bigDoors.NMS.SkullCreator_V1_16_R3;
import nl.pim16aap2.bigDoors.NMS.SkullCreator_V1_17_R1;
import nl.pim16aap2.bigDoors.codegeneration.FallbackGenerator;
import nl.pim16aap2.bigDoors.compatiblity.FakePlayerCreator;
import nl.pim16aap2.bigDoors.compatiblity.ProtectionCompatManager;
import nl.pim16aap2.bigDoors.handlers.ChunkUnloadHandler;
import nl.pim16aap2.bigDoors.handlers.CommandHandler;
import nl.pim16aap2.bigDoors.handlers.EventHandlers;
import nl.pim16aap2.bigDoors.handlers.FailureCommandHandler;
import nl.pim16aap2.bigDoors.handlers.GUIHandler;
import nl.pim16aap2.bigDoors.handlers.LoginMessageHandler;
import nl.pim16aap2.bigDoors.handlers.LoginResourcePackHandler;
import nl.pim16aap2.bigDoors.handlers.RedstoneHandler;
import nl.pim16aap2.bigDoors.moveBlocks.Bridge.getNewLocation.GetNewLocationEast;
import nl.pim16aap2.bigDoors.moveBlocks.Bridge.getNewLocation.GetNewLocationNorth;
import nl.pim16aap2.bigDoors.moveBlocks.Bridge.getNewLocation.GetNewLocationSouth;
import nl.pim16aap2.bigDoors.moveBlocks.BridgeOpener;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationWest;
import nl.pim16aap2.bigDoors.moveBlocks.DoorOpener;
import nl.pim16aap2.bigDoors.moveBlocks.Opener;
import nl.pim16aap2.bigDoors.moveBlocks.PortcullisOpener;
import nl.pim16aap2.bigDoors.moveBlocks.SlidingDoorOpener;
import nl.pim16aap2.bigDoors.skulls.HeadManager;
import nl.pim16aap2.bigDoors.storage.sqlite.SQLiteJDBCDriverConnection;
import nl.pim16aap2.bigDoors.toolUsers.ToolUser;
import nl.pim16aap2.bigDoors.toolUsers.ToolVerifier;
import nl.pim16aap2.bigDoors.util.ChunkUtils.ChunkLoadMode;
import nl.pim16aap2.bigDoors.util.ChunkUtils.ChunkLoadResult;
import nl.pim16aap2.bigDoors.util.ConfigLoader;
import nl.pim16aap2.bigDoors.util.DoorOpenResult;
import nl.pim16aap2.bigDoors.util.DoorType;
import nl.pim16aap2.bigDoors.util.Messages;
import nl.pim16aap2.bigDoors.util.TimedCache;
import nl.pim16aap2.bigDoors.util.Util;
import nl.pim16aap2.bigDoors.waitForCommand.WaitForCommand;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

// TODO: Drawbridges that were created when flat, should remember that direction for opening.

public class BigDoors extends JavaPlugin implements Listener
{
    private static BigDoors instance;
    public static final boolean DEVBUILD = true;
    private int buildNumber = -1;

    public static final int MINIMUMDOORDELAY = 15;

    // TODO: Maybe use a whitelist instead?
    private static final Set<String> BLACKLISTED_SERVERS = Collections
        .unmodifiableSet(new HashSet<>(Arrays.asList("CatServer", "Mohist", "Magma", "Glowstone", "Akarin")));

    private static final List<String> BLACKLISTED_PLUGINS = Collections
        .unmodifiableList(new ArrayList<>(Arrays.asList("Geyser-Spigot", "ViaRewind")));

    private ToolVerifier tf;
    private SQLiteJDBCDriverConnection db;
    private FallingBlockFactory fabf;
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
    private FailureCommandHandler failureCommandHandler;
    private SlidingDoorOpener slidingDoorOpener;
    private PortcullisOpener portcullisOpener;
    private RedstoneHandler redstoneHandler;
    private boolean validVersion;
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
    private boolean isEnabled = false;
    private final List<String> loginMessages = new ArrayList<>();

    public BigDoors()
    {
        instance = this;
    }

    @Override
    public void onEnable()
    {
        logFile = new File(getDataFolder(), "log.txt");
        logger = new MyLogger(this, logFile);
        updateManager = new UpdateManager(this);

        buildNumber = readBuildNumber();
        overrideVersion();

        try
        {
            readConfigValues();
        }
        catch (Exception e)
        {
            logger.logMessageToConsoleOnly("Failed to read config file. Plugin disabled!");
            setDisabled("This plugin is disabled because it failed to read config file!");
            return;
        }

        updateManager.setEnabled(getConfigLoader().autoDLUpdate());

        messages = new Messages(this);

        Optional<String> disableReason = isCurrentEnvironmentInvalid();
        if (disableReason.isPresent())
        {
            if (!getConfigLoader().unsafeMode())
            {
                String error = "This plugin is disabled because it is running in an invalid environment: "
                    + disableReason.get();
                logger.logMessage(error, true, true);
                setDisabled(error);
                return;
            }
            loginMessages.add("You are trying to load this plugin in an unsupported environment: \""
                                  + disableReason.get() + "\"!!!");
        }

        logger.logMessageToLogFile("Starting BigDoors version: " + getDescription().getVersion());

        if (DEVBUILD)
            logger.logMessageToConsoleOnly("WARNING! You are running a devbuild (build: " + buildNumber + ")!");

        try
        {
            Bukkit.getPluginManager().registerEvents(new LoginMessageHandler(this), this);

            validVersion = compatibleMCVer();
            // Load the files for the correct version of Minecraft.
            if (!validVersion)
            {
                logger.logMessage("Trying to load the plugin on an incompatible version of Minecraft! (\""
                                      + (Bukkit.getServer().getClass().getPackage().getName().replace(".", ",")
                                               .split(",")[3])
                                      + "\"). This plugin will NOT be enabled!", true, true);
                setDisabled("This version of Minecraft is not supported. Is the plugin up-to-date?");
                return;
            }
            fakePlayerCreator = new FakePlayerCreator(this);

            init();

            headManager.init(config.headCacheTimeout());
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
            bridgeOpener = new BridgeOpener(this);
            commandHandler = new CommandHandler(this);
            portcullisOpener = new PortcullisOpener(this);
            slidingDoorOpener = new SlidingDoorOpener(this);

            registerCommands(commandHandler);

            liveDevelopmentLoad();
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
            logger.logMessage(Util.exceptionToString(exception), true, true);
            setDisabled(
                "This plugin is disabled because an unknown error occurred during startup, please check the logs!");
        }

        isEnabled = true;
    }

    /**
     * Checks if the current environment is invalid. This plugin should not attempt initialization in an invalid
     * environment.
     * <p>
     * Note that it doesn't check if the version is valid. That is done somewhere else and I couldn't be bothered to
     * rewrite it.
     *
     * @return The name of the invalid environment, if one could be found.
     */
    private Optional<String> isCurrentEnvironmentInvalid()
    {
        for (final String pluginName : BLACKLISTED_PLUGINS)
            if (getServer().getPluginManager().getPlugin(pluginName) != null)
                return Optional.of(pluginName);

        if (BigDoors.BLACKLISTED_SERVERS.contains(Bukkit.getName()))
            return Optional.of(Bukkit.getName());

        return Optional.empty();
    }

    private void registerCommands(CommandExecutor commandExecutor)
    {
        getCommand("recalculatepowerblocks").setExecutor(commandExecutor);
        getCommand("killbigdoorsentities").setExecutor(commandExecutor);
        getCommand("inspectpowerblockloc").setExecutor(commandExecutor);
        getCommand("changepowerblockloc").setExecutor(commandExecutor);
        getCommand("setautoclosetime").setExecutor(commandExecutor);
        getCommand("setdoorrotation").setExecutor(commandExecutor);
        getCommand("setblockstomove").setExecutor(commandExecutor);
        getCommand("listplayerdoors").setExecutor(commandExecutor);
        getCommand("setnotification").setExecutor(commandExecutor);
        getCommand("newportcullis").setExecutor(commandExecutor);
        getCommand("toggledoor").setExecutor(commandExecutor);
        getCommand("pausedoors").setExecutor(commandExecutor);
        getCommand("closedoor").setExecutor(commandExecutor);
        getCommand("doordebug").setExecutor(commandExecutor);
        getCommand("listdoors").setExecutor(commandExecutor);
        getCommand("stopdoors").setExecutor(commandExecutor);
        getCommand("bdcancel").setExecutor(commandExecutor);
        getCommand("filldoor").setExecutor(commandExecutor);
        getCommand("doorinfo").setExecutor(commandExecutor);
        getCommand("opendoor").setExecutor(commandExecutor);
        getCommand("nameDoor").setExecutor(commandExecutor);
        getCommand("bigdoors").setExecutor(commandExecutor);
        getCommand("newdoor").setExecutor(commandExecutor);
        getCommand("deldoor").setExecutor(commandExecutor);
        getCommand("bdm").setExecutor(commandExecutor);
    }

    private void setDisabled(String reason)
    {
        failureCommandHandler = new FailureCommandHandler(reason);
        registerCommands(failureCommandHandler);
    }

    private void init()
    {
        if (!validVersion)
            return;

        // Don't read the config if the plugin hasn't been enabled yet.
        // In other words: Skip reading the config on the first run (because it's
        // already read in onEnable).
        if (isEnabled)
            readConfigValues();

        Util.processConfig(getConfigLoader());
        messages.reloadMessages();
        toolUsers = new HashMap<>();
        playerGUIs = new HashMap<>();
        cmdWaiters = new HashMap<>();
        tf = new ToolVerifier(messages.getString("CREATOR.GENERAL.StickName"));

        if (config.enableRedstone())
        {
            redstoneHandler = new RedstoneHandler(this);
            Bukkit.getPluginManager().registerEvents(redstoneHandler, this);
        }

        if (config.resourcePackEnabled())
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
            setupMetrics();
        }
        else
        {
            // Y u do dis? :(
            metrics = null;
            logger
                .myLogger(Level.INFO,
                          "Stats disabled, not laoding stats :(... Please consider enabling it! I am a simple man, seeing higher user numbers helps me stay motivated!");
        }

        updateManager.setEnabled(getConfigLoader().autoDLUpdate());

        if (commander != null)
            commander.setCanGo(true);
    }

    public static BigDoors get()
    {
        return instance;
    }

    public @NotNull ClassLoader getBigDoorsClassLoader()
    {
        return super.getClassLoader();
    }

    /**
     * For this plugin, there is a difference between being enabled and being enabled successfully.
     * <p>
     * This plugin will always try to enable itself, even if it won't function at all. This is done so that an
     * alternative command handler can be registered to inform users that the plugin is broken and admins WHY it failed
     * to enable properly.
     * <p>
     * So, if you inted to use this plugin, make sure to check this function to avoid running into issues.
     *
     * @return True if this plugin was enabled successfully.
     */
    public boolean isEnabledSuccessfully()
    {
        return isEnabled;
    }

    /**
     * Gets the number of the current build. Higher number == newer build. This can be used to check for feature
     * support, for example. Released builds do not use branches, so if feature x is supported in build y, feature x
     * will also be supported in build y+1, unless intentionally removed.
     *
     * @return The id of the current build.
     */
    public int getBuild()
    {
        return buildNumber;
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

    public String canBreakBlock(UUID playerUUID, String playerName, Location loc)
    {
        return protCompatMan.canBreakBlock(playerUUID, playerName, loc);
    }

    public String canBreakBlocksBetweenLocs(UUID playerUUID, String playerName, World world, Location loc1,
                                            Location loc2)
    {
        return protCompatMan.canBreakBlocksBetweenLocs(playerUUID, playerName, world, loc1, loc2);
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
        headManager.reload(config.headCacheTimeout());
    }

    @Override
    public void onDisable()
    {
        if (!validVersion)
            return;

        closeGUIs();

        // Stop all toolUsers and take all BigDoor tools from players.
        commander.setCanGo(false);
        commander.stopMovers(true);

        Iterator<Entry<UUID, ToolUser>> it = toolUsers.entrySet().iterator();
        while (it.hasNext())
        {
            Entry<UUID, ToolUser> entry = it.next();
            entry.getValue().abort();
        }

        toolUsers.clear();
        cmdWaiters.clear();
    }

    private void closeGUIs()
    {
        Iterator<UUID> it = playerGUIs.keySet().iterator();
        while (it.hasNext())
        {
            UUID uuid = it.next();
            Player player = Bukkit.getPlayer(uuid);
            if (player == null)
                continue;
            player.closeInventory();
        }
        playerGUIs.clear();
    }

    private void setupMetrics()
    {
        if (metrics != null)
            return;

        metrics = new Metrics(this, 2887);

        metrics.addCustomChart(new AdvancedPie("doors_per_type", () ->
        {
            DoorType[] doorTypes = DoorType.values();
            Map<String, Integer> output = new HashMap<>(doorTypes.length - 1);
            Map<DoorType, Integer> stats = db.getDatabaseStatistics();
            for (DoorType type : doorTypes)
                output.put(DoorType.getFriendlyName(type), stats.getOrDefault(type, 0));
            return output;
        }));
    }

    public String getUpdateURL()
    {
        return null;
    }

    public String getLoginMessage()
    {
        StringBuilder sb = new StringBuilder();
        if (DEVBUILD)
        {
            sb.append("[BigDoors] Warning: You are running a devbuild!\n");
            sb.append("[BigDoors] Remember to check for new updates regularly!\n");
        }
        if (updateManager.updateAvailable())
        {
            if (getConfigLoader().autoDLUpdate() && updateManager.hasUpdateBeenDownloaded())
                sb.append("[BigDoors] A new update (" + updateManager.getNewestVersion() + ") has been downloaded! "
                              + "Restart your server to apply the update!\n");
            else if (updateManager.updateAvailable())
                sb.append("[BigDoors] A new update is available: " + updateManager.getNewestVersion() + "\n");
        }
        if (failureCommandHandler != null)
            sb.append("[BigDoors] " + failureCommandHandler.getError() + "\n");
        loginMessages.forEach(str -> sb.append("[BigDoors] " + str + "\n"));
        return sb.toString();
    }

    public UpdateManager getUpdateManager()
    {
        return updateManager;
    }

    public TimedCache<Long, HashMap<Long, Long>> getPBCache()
    {
        return pbCache;
    }

    public FallingBlockFactory getFABF()
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

        if (config.unsafeMode())
        {
            logger.warn("╔═══════════════════════════════════════════════════════╗");
            logger.warn("║                                                       ║");
            logger.warn("║                    !!  WARNING  !!                    ║");
            logger.warn("║                                                       ║");
            logger.warn("║                                                       ║");
            logger.warn("║            You have enabled \"unsafe mode\"!            ║");
            logger.warn("║                                                       ║");
            logger.warn("║   THIS IS NOT SUPPORTED! USE THIS AT YOUR OWN RISK!   ║");
            logger.warn("║                                                       ║");
            logger.warn("╚═══════════════════════════════════════════════════════╝");

            loginMessages.add("You are running this plugin in unsafe mode! THIS IS NOT SUPPORTED!");
        }
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

        MCVersion mcVersion = MCVersion.v1_17;


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
        else if (version.equals("v1_16_R1") || version.equals("v1_16_R2") || version.equals("v1_16_R3"))
            mcVersion = MCVersion.v1_16;
        else if (version.equals("v1_17_R1"))
            mcVersion = MCVersion.v1_17;
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
        switch (version)
        {
            case "v1_8_R1":
            case "v1_8_R2":
            case "v1_8_R3":
            case "v1_9_R1":
            case "v1_9_R2":
            case "v1_10_R1":
                return false;
            case "v1_11_R1":
                fabf = new FallingBlockFactory_V1_11_R1();
                headManager = new SkullCreator_V1_11_R1(this);
                break;
            case "v1_12_R1":
                fabf = new FallingBlockFactory_V1_12_R1();
                headManager = new SkullCreator_V1_12_R1(this);
                break;
            case "v1_13_R1":
                is1_13 = true;
                fabf = new FallingBlockFactory_V1_13_R1();
                headManager = new SkullCreator_V1_13_R1(this);
                break;
            case "v1_13_R2":
                String[] split = Bukkit.getBukkitVersion().split("-")[0].split("\\.");
                final int minorVersion = Util.parseInt(split.length > 2 ? split[2] : null).orElse(-1);
                if (minorVersion == -1)
                {
                    logger.severe("Failed to parse minor version from: \"" + Bukkit.getBukkitVersion() + "\"");
                    return false;
                }
                is1_13 = true;
                // 1.13.1 has the same package version as 1.13.2, but there are actual NMS changes between them.
                // That's why 1.13.1 is a kinda R1.5 package.
                if (minorVersion == 1)
                {
                    fabf = new FallingBlockFactory_V1_13_R1_5();
                    headManager = new SkullCreator_V1_13_R1_5(this);
                }
                else
                {
                    fabf = new FallingBlockFactory_V1_13_R2();
                    headManager = new SkullCreator_V1_13_R2(this);
                }
                break;
            case "v1_14_R1":
                is1_13 = true; // Yeah, it's actually 1.14, but it still needs to use new stuff.

                fabf = new FallingBlockFactory_V1_14_R1();
                headManager = new SkullCreator_V1_14_R1(this);
                break;
            case "v1_15_R1":
                is1_13 = true; // Yeah, it's actually 1.15, but it still needs to use new stuff.

                fabf = new FallingBlockFactory_V1_15_R1();
                headManager = new SkullCreator_V1_15_R1(this);
                break;
            case "v1_16_R1":
                is1_13 = true; // Yeah, it's not actually 1.13, but it still needs to use new stuff.

                fabf = new FallingBlockFactory_V1_16_R1();
                headManager = new SkullCreator_V1_16_R1(this);
                break;
            case "v1_16_R2":
                is1_13 = true; // Yeah, it's not actually 1.13, but it still needs to use new stuff.

                fabf = new FallingBlockFactory_V1_16_R2();
                headManager = new SkullCreator_V1_16_R2(this);
                break;
            case "v1_16_R3":
                is1_13 = true; // Yeah, it's not actually 1.13, but it still needs to use new stuff.

                fabf = new FallingBlockFactory_V1_16_R3();
                headManager = new SkullCreator_V1_16_R3(this);
                break;
            case "v1_17_R1":
                is1_13 = true; // Yeah, it's not actually 1.13, but it still needs to use new stuff.

                fabf = new FallingBlockFactory_V1_17_R1();
                headManager = new SkullCreator_V1_17_R1(this);
                break;
            default:
                getMyLogger()
                    .warn("The plugin has not been tested for this version! We'll try to make it work, though...");
                is1_13 = true; // Yeah, it's not actually 1.13, but it still needs to use new stuff.
                fabf = new FallbackGenerator().getFallingBlockFactory();
                // TODO: Implement SkullCreator.
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
                                                                                  .getResourceAsStream(
                                                                                      "/build.number"))))
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

    /**
     * Checks if all chunks a door could interact with if it were to be toggled right now. Depending on how much is
     * known about the door (e.g. open direction, blocksToMove) and its type, the result can be more or less reliable.
     *
     * @param door The door for which to check which chunks it could interact with.
     * @return True if all chunks the door could interact with are currently loaded.
     */
    public boolean areChunksLoadedForDoor(Door door)
    {
        Opener opener = getDoorOpener(door.getType());
        return opener == null ? false : opener.chunksLoaded(door, ChunkLoadMode.VERIFY_LOADED) == ChunkLoadResult.PASS;
    }

    // (Instantly?) Toggle a door with a given time.
    public DoorOpenResult toggleDoor(Door door, double time, boolean instantOpen)
    {
        Opener opener = getDoorOpener(door.getType());
        return opener == null ? DoorOpenResult.TYPEDISABLED : opener.openDoor(door, time, instantOpen);
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
        v1_15,
        v1_16,
        v1_17,
        v1_18,
        v1_19,
        v1_20,
    }

//    public long createNewDoor(Location min, Location max, Location engine,
//                              Location powerBlock, DoorType type, )
}
