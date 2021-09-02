package nl.pim16aap2.bigdoors.spigot;

import lombok.Getter;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.annotations.Initializer;
import nl.pim16aap2.bigdoors.api.DebugReporter;
import nl.pim16aap2.bigdoors.api.IBlockAnalyzer;
import nl.pim16aap2.bigdoors.api.IChunkManager;
import nl.pim16aap2.bigdoors.api.IEconomyManager;
import nl.pim16aap2.bigdoors.api.IGlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.api.IMessagingInterface;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.IPermissionsManager;
import nl.pim16aap2.bigdoors.api.IPowerBlockRedstoneManager;
import nl.pim16aap2.bigdoors.api.ISoundEngine;
import nl.pim16aap2.bigdoors.api.factories.IBigDoorsEventFactory;
import nl.pim16aap2.bigdoors.api.factories.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IPBlockDataFactory;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.api.factories.IPWorldFactory;
import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.commands.IPServer;
import nl.pim16aap2.bigdoors.doors.DoorOpener;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.events.IBigDoorsEvent;
import nl.pim16aap2.bigdoors.extensions.DoorTypeLoader;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.localization.LocalizationManager;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.logging.PLogger;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DelayedCommandInputManager;
import nl.pim16aap2.bigdoors.managers.DoorRegistry;
import nl.pim16aap2.bigdoors.managers.DoorSpecificationManager;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import nl.pim16aap2.bigdoors.managers.LimitsManager;
import nl.pim16aap2.bigdoors.managers.PowerBlockManager;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.moveblocks.AutoCloseScheduler;
import nl.pim16aap2.bigdoors.moveblocks.DoorActivityManager;
import nl.pim16aap2.bigdoors.spigot.compatiblity.ProtectionCompatManagerSpigot;
import nl.pim16aap2.bigdoors.spigot.config.ConfigLoaderSpigot;
import nl.pim16aap2.bigdoors.spigot.events.BigDoorsSpigotEvent;
import nl.pim16aap2.bigdoors.spigot.factories.BigDoorsEventFactorySpigot;
import nl.pim16aap2.bigdoors.spigot.factories.PLocationFactorySpigot;
import nl.pim16aap2.bigdoors.spigot.factories.PPlayerFactorySpigot;
import nl.pim16aap2.bigdoors.spigot.factories.PWorldFactorySpigot;
import nl.pim16aap2.bigdoors.spigot.implementations.BigDoorsToolUtilSpigot;
import nl.pim16aap2.bigdoors.spigot.listeners.ChunkListener;
import nl.pim16aap2.bigdoors.spigot.listeners.EventListeners;
import nl.pim16aap2.bigdoors.spigot.listeners.LoginMessageListener;
import nl.pim16aap2.bigdoors.spigot.listeners.LoginResourcePackListener;
import nl.pim16aap2.bigdoors.spigot.listeners.RedstoneListener;
import nl.pim16aap2.bigdoors.spigot.listeners.WorldListener;
import nl.pim16aap2.bigdoors.spigot.managers.HeadManager;
import nl.pim16aap2.bigdoors.spigot.managers.PlatformManagerSpigot;
import nl.pim16aap2.bigdoors.spigot.managers.PowerBlockRedstoneManagerSpigot;
import nl.pim16aap2.bigdoors.spigot.managers.UpdateManager;
import nl.pim16aap2.bigdoors.spigot.managers.VaultManager;
import nl.pim16aap2.bigdoors.spigot.util.DebugReporterSpigot;
import nl.pim16aap2.bigdoors.spigot.util.GlowingBlockSpawner;
import nl.pim16aap2.bigdoors.spigot.util.MessagingInterfaceSpigot;
import nl.pim16aap2.bigdoors.spigot.util.PExecutorSpigot;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.spigot.util.api.BigDoorsSpigotAbstract;
import nl.pim16aap2.bigdoors.spigot.util.api.IPlatformManagerSpigot;
import nl.pim16aap2.bigdoors.spigot.util.implementations.ChunkManagerSpigot;
import nl.pim16aap2.bigdoors.spigot.util.implementations.MessageableServerSpigot;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PServer;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PSoundEngineSpigot;
import nl.pim16aap2.bigdoors.storage.IStorage;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Represents the implementation of {@link BigDoorsSpigotAbstract}.
 *
 * @author Pim
 */
// Almost everything is initializer later, because that's how Spigot works.
// This class is just a mess in general and needs a full rewrite.
@SuppressWarnings({"NullAway.Init", "PMD", "ConstantConditions"})
public final class BigDoorsSpigot extends BigDoorsSpigotAbstract
{
    @SuppressWarnings({"squid:S3008", "PMD.FieldNamingConventions"}) // TODO: Remove this.
    private static BigDoorsSpigot INSTANCE;
    @SuppressWarnings({"squid:S3008", "PMD.FieldNamingConventions"}) // TODO: Remove this.
    private static long MAIN_THREAD_ID = -1;

    private final PLogger pLogger = new PLogger(new File(getDataFolder(), "log.txt"));

    @Getter
    private ConfigLoaderSpigot configLoader;
    private Metrics metrics;
    private RedstoneListener redstoneListener;
    private LoginResourcePackListener loginResourcePackListener;

    private boolean validVersion = false;
    private final IPExecutor pExecutor;
    private final Set<IRestartable> restartables = new LinkedHashSet<>();

    @Getter
    private ProtectionCompatManagerSpigot protectionCompatManager;
    private @Nullable LoginResourcePackListener rPackHandler;

    @Getter
    private PowerBlockManager powerBlockManager;
    private WorldListener worldListener;

    @Getter
    private VaultManager vaultManager;

    private IGlowingBlockSpawner glowingBlockSpawner;

    @Getter
    private final LimitsManager limitsManager = new LimitsManager();

    @Getter
    private HeadManager headManager;
    private UpdateManager updateManager;

    private boolean successfulInit = true;

    @Getter
    private final IPServer pServer = new PServer(this);

    @Getter
    private final IPLocationFactory pLocationFactory = new PLocationFactorySpigot();

    @Getter
    private final IPWorldFactory pWorldFactory = new PWorldFactorySpigot();

    @Getter
    private final IPPlayerFactory pPlayerFactory = new PPlayerFactorySpigot();

    @Getter
    private final ISoundEngine soundEngine = new PSoundEngineSpigot();

    @Getter
    private final IMessagingInterface messagingInterface = new MessagingInterfaceSpigot(this);

    @Getter
    private final IChunkManager chunkManager = ChunkManagerSpigot.get();

    @Getter
    private final IBigDoorsEventFactory bigDoorsEventFactory = new BigDoorsEventFactorySpigot();

    @Getter
    private final IPowerBlockRedstoneManager powerBlockRedstoneManager = PowerBlockRedstoneManagerSpigot.get();

    @Getter
    private final BigDoorsToolUtilSpigot bigDoorsToolUtil;

    @Getter
    private DatabaseManager databaseManager;

    @Getter
    private final DoorOpener doorOpener;

    @Getter
    private final DoorRegistry doorRegistry = new DoorRegistry();

    @Getter
    private final AutoCloseScheduler autoCloseScheduler = new AutoCloseScheduler();

    @Getter
    private final DoorActivityManager doorActivityManager = new DoorActivityManager(this);

    @Getter
    private final DoorSpecificationManager doorSpecificationManager = new DoorSpecificationManager();

    @Getter
    private final DoorTypeManager doorTypeManager = new DoorTypeManager();

    @Getter
    private final ToolUserManager toolUserManager = new ToolUserManager(this);

    @Getter
    private final DelayedCommandInputManager delayedCommandInputManager = new DelayedCommandInputManager();

    @Getter
    private ILocalizer localizer;

    private LocalizationManager localizationManager;

    public BigDoorsSpigot()
    {
        INSTANCE = this;
        BigDoors.get().setBigDoorsPlatform(this);
        BigDoors.get().registerRestartable(this);

        MAIN_THREAD_ID = Thread.currentThread().getId();
        pExecutor = new PExecutorSpigot(this);
        bigDoorsToolUtil = new BigDoorsToolUtilSpigot();

        doorOpener = new DoorOpener();
    }

    @Override
    @Initializer
    public void onEnable()
    {
        Bukkit.getLogger().setLevel(Level.FINER);

        try
        {
            if (glowingBlockSpawner == null)
                glowingBlockSpawner = new GlowingBlockSpawner(this);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            // Register this here so it can check for updates even when loaded on an incorrect version.
            updateManager = new UpdateManager(this, 58_669);

            databaseManager = new DatabaseManager(this, new File(super.getDataFolder(), "doorDB.db"));
            registerDoorTypes();

            Bukkit.getPluginManager().registerEvents(new LoginMessageListener(this), this);
            validVersion = PlatformManagerSpigot.get().initPlatform(this);

            // Load the files for the correct version of Minecraft.
            if (!validVersion)
            {
                pLogger.severe("Trying to load the plugin on an incompatible version of Minecraft! (\""
                                   + (Bukkit.getServer().getClass().getPackage().getName().replace(".", ",")
                                            .split(",")[3])
                                   + "\"). This plugin will NOT be enabled!");
                disablePlugin();
                return;
            }

            configLoader = ConfigLoaderSpigot.init(this, getPLogger());
            init();

            redstoneListener = redstoneListener == null ? new RedstoneListener(this) : redstoneListener;
            loginResourcePackListener = loginResourcePackListener == null ?
                                        new LoginResourcePackListener(this, configLoader.resourcePack()) :
                                        loginResourcePackListener;

            final IStorage.DatabaseState databaseState = databaseManager.getDatabaseState();
            if (databaseState != IStorage.DatabaseState.OK)
            {
                BigDoors.get().getPLogger()
                        .severe("Failed to load database! Found it in the state: " + databaseState.name() +
                                    ". Plugin initialization has been aborted!");
                disablePlugin();
                return;
            }

            vaultManager = VaultManager.init(this);

            headManager = headManager == null ? new HeadManager(this, getConfigLoader()) : headManager;

            Bukkit.getPluginManager().registerEvents(new EventListeners(this), this);
            Bukkit.getPluginManager().registerEvents(new ChunkListener(this), this);

            protectionCompatManager = protectionCompatManager == null ?
                                      new ProtectionCompatManagerSpigot(this) : protectionCompatManager;
            Bukkit.getPluginManager().registerEvents(protectionCompatManager, this);

            powerBlockManager = new PowerBlockManager(this, configLoader, databaseManager, getPLogger());

            //noinspection ConstantConditions
            if (worldListener == null)
                worldListener = new WorldListener(powerBlockManager);

            Bukkit.getPluginManager().registerEvents(worldListener, this);

            pLogger.info("Successfully enabled BigDoors " + getDescription().getVersion());
        }
        catch (Exception exception)
        {
            successfulInit = false;
            pLogger.logThrowable(exception);
        }
    }

    /**
     * Disables this plugin.
     */
    private void disablePlugin()
    {
        successfulInit = false;
        Bukkit.getPluginManager().disablePlugin(this);
    }

    /**
     * Registers all BigDoor's own door types.
     */
    private void registerDoorTypes()
    {
        final File extensionsDir = new File(BigDoors.get().getPlatform().getDataDirectory() +
                                                Constants.BIGDOORS_EXTENSIONS_FOLDER);
        if (!extensionsDir.exists() && !extensionsDir.mkdirs())
        {
            BigDoors.get().getPLogger()
                    .logThrowable(new IOException("Failed to create folder: " + extensionsDir));
            return;
        }

        Bukkit.getLogger().setLevel(Level.ALL);
        DoorTypeLoader.get().loadDoorTypesFromDirectory();
    }

    @Override
    public IPlatformManagerSpigot getPlatformManagerSpigot()
    {
        return PlatformManagerSpigot.get();
    }

    public static BigDoorsSpigot get()
    {
        return INSTANCE;
    }

    private void init()
    {
        if (!validVersion)
            return;

        configLoader.restart();

        initLocalization();

        updateManager.setEnabled(getConfigLoader().checkForUpdates(), getConfigLoader().autoDLUpdate());
    }

    private void initLocalization()
    {
        if (localizationManager != null)
            return;

        final List<Class<?>> types = doorTypeManager.getEnabledDoorTypes().stream()
                                                    .map(DoorType::getDoorClass)
                                                    .collect(Collectors.toList());
        localizationManager = new LocalizationManager(this, getDataDirectory().toPath().resolve(""), "translations",
                                                      getConfigLoader());
        localizationManager.addResourcesFromClass(types);
        localizationManager.addResourcesFromClass(List.of(getClass()));
        localizer = localizationManager.getLocalizer();
    }

    @Override
    public File getDataDirectory()
    {
        return getDataFolder();
    }

    @Override
    public IPBlockDataFactory getPBlockDataFactory()
    {
        return PlatformManagerSpigot.get().getSpigotPlatform().getPBlockDataFactory();
    }

    @Override
    public IFallingBlockFactory getFallingBlockFactory()
    {
        return PlatformManagerSpigot.get().getSpigotPlatform().getFallingBlockFactory();
    }

    @Override
    public IMessageable getMessageableServer()
    {
        return MessageableServerSpigot.get();
    }

    @Override
    public IBlockAnalyzer getBlockAnalyzer()
    {
        return PlatformManagerSpigot.get().getSpigotPlatform().getBlockAnalyzer();
    }

    @Override
    public boolean isMainThread(long compareThread)
    {
        return compareThread == MAIN_THREAD_ID;
    }

    @Override
    public IPExecutor getPExecutor()
    {
        return pExecutor;
    }

    public Optional<String> canBreakBlock(IPPlayer player, IPLocation loc)
    {
        return protectionCompatManager.canBreakBlock(player, loc);
    }

    public Optional<String> canBreakBlocksBetweenLocs(IPPlayer player, Vector3Di pos1, Vector3Di pos2, IPWorld world)
    {
        return protectionCompatManager.canBreakBlocksBetweenLocs(player, pos1, pos2, world);
    }

    @Override
    public void registerRestartable(IRestartable restartable)
    {
        restartables.add(restartable);
    }

    @Override
    public boolean isRestartableRegistered(IRestartable restartable)
    {
        return restartables.contains(restartable);
    }

    @Override
    public void deregisterRestartable(IRestartable restartable)
    {
        restartables.remove(restartable);
    }

    @Override
    public void restart()
    {
        if (!validVersion)
            return;

        configLoader.restart();

        shutdown();

        HandlerList.unregisterAll(rPackHandler);
        rPackHandler = null;

        init();

        restartables.forEach(IRestartable::restart);
    }

    @Override
    public void shutdown()
    {
    }

    @Override
    public void onDisable()
    {
        shutdown();
        restartables.forEach(IRestartable::shutdown);
    }

    @Override
    public IEconomyManager getEconomyManager()
    {
        return vaultManager;
    }

    @Override
    public IPermissionsManager getPermissionsManager()
    {
        return vaultManager;
    }

    public IFallingBlockFactory getFABF()
    {
        return PlatformManagerSpigot.get().getSpigotPlatform().getFallingBlockFactory();
    }

    @Override
    public Optional<IGlowingBlockSpawner> getGlowingBlockSpawner()
    {
        return Optional.ofNullable(glowingBlockSpawner);
    }

    public BigDoorsSpigot getPlugin()
    {
        return this;
    }

    public void onPlayerLogout(Player player)
    {
        getDelayedCommandInputManager().cancelAll(SpigotAdapter.wrapPlayer(player));
        toolUserManager.abortToolUser(player.getUniqueId());
    }

    @Override
    public String getVersion()
    {
        return BigDoorsSpigot.get().getDescription().getVersion();
    }

    @Override
    public DebugReporter getDebugReporter()
    {
        return new DebugReporterSpigot(this);
    }

    // Get the logger.
    @Override
    public IPLogger getPLogger()
    {
        return pLogger;
    }

    /**
     * Gets the message to send to admins and OPs when they log in. This message can contain all kinds of information,
     * including but not limited to: The current build is a dev build, the plugin could not be initialized properly, an
     * update is available.
     *
     * @return The message to send to admins and OPs when they log in.
     */
    public String getLoginMessage()
    {
        String ret = "";
        if (Constants.DEV_BUILD)
            ret += "[BigDoors] Warning: You are running a devbuild!\n";
        if (!validVersion)
            ret += "[BigDoors] Error: Trying to load the game on an invalid version! Plugin disabled!\n";
        if (!successfulInit)
            ret += "[BigDoors] Error: Failed to initialize the plugin! Some functions may not work as expected. " +
                "Please contact pim16aap2! Don't forget to attach both the server log AND the BigDoors log!\n";
        if (updateManager.updateAvailable())
        {
            if (getConfigLoader().autoDLUpdate() && updateManager.hasUpdateBeenDownloaded())
                ret += "[BigDoors] A new update (" + updateManager.getNewestVersion() +
                    ") has been downloaded! "
                    + "Restart your server to apply the update!\n";
            else if (updateManager.updateAvailable())
                ret += "[BigDoors] A new update is available: " + updateManager.getNewestVersion() + "\n";
        }
        return ret;
    }

    @Override
    public void callDoorEvent(IBigDoorsEvent doorEvent)
    {
        if (!(doorEvent instanceof BigDoorsSpigotEvent))
        {
            getPLogger().logThrowable(new IllegalArgumentException(
                "Event " + doorEvent.getEventName() +
                    ", is not a Spigot event, but it was called on the Spigot platform!"));
            return;
        }

        // Async events can only be called asynchronously and Sync events can only be called from the main thread.
        final boolean isMainThread = isMainThread(Thread.currentThread().getId());
        if (isMainThread && doorEvent.isAsynchronous())
            BigDoors.get().getPlatform().getPExecutor()
                    .runAsync(() -> Bukkit.getPluginManager().callEvent((BigDoorsSpigotEvent) doorEvent));
        else if ((!isMainThread) && (!doorEvent.isAsynchronous()))
            BigDoors.get().getPlatform().getPExecutor()
                    .runSync(() -> Bukkit.getPluginManager().callEvent((BigDoorsSpigotEvent) doorEvent));
        else
            Bukkit.getPluginManager().callEvent((BigDoorsSpigotEvent) doorEvent);
    }
}
