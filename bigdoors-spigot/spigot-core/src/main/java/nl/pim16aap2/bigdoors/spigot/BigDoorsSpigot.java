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
import nl.pim16aap2.bigdoors.api.IPermissionsManager;
import nl.pim16aap2.bigdoors.api.IPowerBlockRedstoneManager;
import nl.pim16aap2.bigdoors.api.ISoundEngine;
import nl.pim16aap2.bigdoors.api.factories.IBigDoorsEventFactory;
import nl.pim16aap2.bigdoors.api.factories.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IPBlockDataFactory;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.api.factories.IPWorldFactory;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolderModule;
import nl.pim16aap2.bigdoors.commands.CommandFactory;
import nl.pim16aap2.bigdoors.commands.IPServer;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.events.IBigDoorsEvent;
import nl.pim16aap2.bigdoors.extensions.DoorTypeLoader;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.localization.LocalizationManager;
import nl.pim16aap2.bigdoors.logging.IPLogger;
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
import nl.pim16aap2.bigdoors.spigot.implementations.BigDoorsToolUtilSpigot;
import nl.pim16aap2.bigdoors.spigot.listeners.LoginResourcePackListener;
import nl.pim16aap2.bigdoors.spigot.listeners.RedstoneListener;
import nl.pim16aap2.bigdoors.spigot.listeners.WorldListener;
import nl.pim16aap2.bigdoors.spigot.managers.HeadManager;
import nl.pim16aap2.bigdoors.spigot.managers.PlatformManagerSpigot;
import nl.pim16aap2.bigdoors.spigot.managers.UpdateManager;
import nl.pim16aap2.bigdoors.spigot.managers.VaultManager;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.spigot.util.api.BigDoorsSpigotAbstract;
import nl.pim16aap2.bigdoors.spigot.util.api.IPlatformManagerSpigot;
import nl.pim16aap2.bigdoors.util.Constants;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.List;
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
@Singleton
@Getter
public final class BigDoorsSpigot extends BigDoorsSpigotAbstract
{
    @SuppressWarnings({"squid:S3008", "PMD.FieldNamingConventions"}) // TODO: Remove this.
    private static BigDoorsSpigot INSTANCE;
    @SuppressWarnings({"squid:S3008", "PMD.FieldNamingConventions"}) // TODO: Remove this.
    private static long MAIN_THREAD_ID = -1;

    private boolean validVersion;
    private boolean successfulInit;

    private final IPLogger pLogger;
    private final ConfigLoaderSpigot configLoader;
    private final RedstoneListener redstoneListener;
    private final LoginResourcePackListener loginResourcePackListener;
    private final IPExecutor pExecutor;
    private final ProtectionCompatManagerSpigot protectionCompatManager;
    private final LoginResourcePackListener rPackHandler;
    private final PowerBlockManager powerBlockManager;
    private final WorldListener worldListener;
    private final VaultManager vaultManager;
    private final IGlowingBlockSpawner glowingBlockSpawner;
    private final LimitsManager limitsManager;
    private final HeadManager headManager;
    private final UpdateManager updateManager;
    private final IPServer pServer;
    private final IPLocationFactory pLocationFactory;
    private final IPWorldFactory pWorldFactory;
    private final IPPlayerFactory pPlayerFactory;
    private final ISoundEngine soundEngine;
    private final IMessagingInterface messagingInterface;
    private final IMessageable messageableServer;
    private final IChunkManager chunkManager;
    private final IBigDoorsEventFactory bigDoorsEventFactory;
    private final IPBlockDataFactory blockDataFactory;
    private final IFallingBlockFactory fallingBlockFactory;
    private final IBlockAnalyzer blockAnalyzer;
    private final DoorTypeLoader doorTypeLoader;
    private final IPowerBlockRedstoneManager powerBlockRedstoneManager;
    private final BigDoorsToolUtilSpigot bigDoorsToolUtil;
    private final DatabaseManager databaseManager;
    private final DoorRegistry doorRegistry;
    private final AutoCloseScheduler autoCloseScheduler;
    private final DoorActivityManager doorActivityManager;
    private final DoorSpecificationManager doorSpecificationManager;
    private final DoorTypeManager doorTypeManager;
    private final ToolUserManager toolUserManager;
    private final DelayedCommandInputManager delayedCommandInputManager;
    private final ILocalizer localizer;
    private final LocalizationManager localizationManager;
    private final BigDoorsSpigotComponent bigDoorsSpigotComponent;
    private final CommandFactory commandFactory;

    public BigDoorsSpigot()
    {
        INSTANCE = this;
        MAIN_THREAD_ID = Thread.currentThread().getId();

        BigDoors.get().setBigDoorsPlatform(this);
        BigDoors.get().registerRestartable(this);

        final PlatformManagerSpigot platformManagerSpigot;
        try
        {
            platformManagerSpigot = new PlatformManagerSpigot(this);
        }
        catch (InstantiationException e)
        {
            throw new RuntimeException(e);
        }

        bigDoorsSpigotComponent = DaggerBigDoorsSpigotComponent
            .builder()
            .bigDoorsSpigotModule(new BigDoorsSpigotModule(this, this, platformManagerSpigot.getSpigotPlatform()))
            .restartableHolderModule(new RestartableHolderModule(BigDoors.get()))
            .build();


        pLogger = bigDoorsSpigotComponent.getLogger();
        protectionCompatManager = bigDoorsSpigotComponent.getProtectionCompatManager();

        configLoader = bigDoorsSpigotComponent.getConfig();
        redstoneListener = bigDoorsSpigotComponent.getRedstoneListener();
        loginResourcePackListener = bigDoorsSpigotComponent.getLoginResourcePackListener();
        pExecutor = bigDoorsSpigotComponent.getPExecutor();
        rPackHandler = bigDoorsSpigotComponent.getLoginResourcePackListener();
        powerBlockManager = bigDoorsSpigotComponent.getPowerBlockManager();
        worldListener = bigDoorsSpigotComponent.getWorldListener();
        vaultManager = bigDoorsSpigotComponent.getVaultManager();
        glowingBlockSpawner = bigDoorsSpigotComponent.getIGlowingBlockSpawner();
        limitsManager = bigDoorsSpigotComponent.getLimitsManager();
        headManager = bigDoorsSpigotComponent.getHeadManager();
        updateManager = bigDoorsSpigotComponent.getUpdateManager();
        pServer = bigDoorsSpigotComponent.getIPServer();
        pLocationFactory = bigDoorsSpigotComponent.getIPLocationFactory();
        pWorldFactory = bigDoorsSpigotComponent.getIPWorldFactory();
        pPlayerFactory = bigDoorsSpigotComponent.getIPPlayerFactory();
        soundEngine = bigDoorsSpigotComponent.getISoundEngine();
        messagingInterface = bigDoorsSpigotComponent.getIMessagingInterface();
        messageableServer = bigDoorsSpigotComponent.getMessageable();
        chunkManager = bigDoorsSpigotComponent.getIChunkManager();
        bigDoorsEventFactory = bigDoorsSpigotComponent.getIBigDoorsEventFactory();
        powerBlockRedstoneManager = bigDoorsSpigotComponent.getIPowerBlockRedstoneManager();
        bigDoorsToolUtil = bigDoorsSpigotComponent.getBigDoorsToolUtilSpigot();
        databaseManager = bigDoorsSpigotComponent.getDatabaseManager();
        doorRegistry = bigDoorsSpigotComponent.getDoorRegistry();
        autoCloseScheduler = bigDoorsSpigotComponent.getAutoCloseScheduler();
        doorActivityManager = bigDoorsSpigotComponent.getDoorActivityManager();
        doorSpecificationManager = bigDoorsSpigotComponent.getDoorSpecificationManager();
        doorTypeManager = bigDoorsSpigotComponent.getDoorTypeManager();
        toolUserManager = bigDoorsSpigotComponent.getToolUserManager();
        delayedCommandInputManager = bigDoorsSpigotComponent.getDelayedCommandInputManager();
        localizer = bigDoorsSpigotComponent.getILocalizer();
        localizationManager = bigDoorsSpigotComponent.getLocalizationManager();
        blockDataFactory = bigDoorsSpigotComponent.getBlockDataFactory();
        fallingBlockFactory = bigDoorsSpigotComponent.getFallingBlockFactory();
        blockAnalyzer = bigDoorsSpigotComponent.getBlockAnalyzer();
        doorTypeLoader = bigDoorsSpigotComponent.getDoorTypeLoader();
        commandFactory = bigDoorsSpigotComponent.getCommandFactory();
    }

    @Override
    @Initializer
    public void onEnable()
    {
        Bukkit.getLogger().setLevel(Level.FINER);

//        try
//        {
//            // Register this here so it can check for updates even when loaded on an incorrect version.
//            updateManager = new UpdateManager(this, 58_669, getPLogger());
//
//            databaseManager = new DatabaseManager(this, new File(super.getDataFolder(), "doorDB.db"));
//            registerDoorTypes();
//
//            Bukkit.getPluginManager().registerEvents(new LoginMessageListener(this), this);
//            validVersion = PlatformManagerSpigot.get().initPlatform(this);
//
//            // Load the files for the correct version of Minecraft.
//            if (!validVersion)
//            {
//                pLogger.severe("Trying to load the plugin on an incompatible version of Minecraft! (\""
//                                   + (Bukkit.getServer().getClass().getPackage().getName().replace(".", ",")
//                                            .split(",")[3])
//                                   + "\"). This plugin will NOT be enabled!");
//                disablePlugin();
//                return;
//            }
//
//            configLoader = ConfigLoaderSpigot.init(this, getPLogger());
//            init();
//
//            redstoneListener = redstoneListener == null ? new RedstoneListener(this) : redstoneListener;
//            loginResourcePackListener = loginResourcePackListener == null ?
//                                        new LoginResourcePackListener(this, configLoader.resourcePack()) :
//                                        loginResourcePackListener;
//
//            final IStorage.DatabaseState databaseState = databaseManager.getDatabaseState();
//            if (databaseState != IStorage.DatabaseState.OK)
//            {
//                BigDoors.get().getPLogger()
//                        .severe("Failed to load database! Found it in the state: " + databaseState.name() +
//                                    ". Plugin initialization has been aborted!");
//                disablePlugin();
//                return;
//            }
//
//            vaultManager = new VaultManager(localizer, pLogger, configLoader);
//
//            headManager = headManager == null ? new HeadManager(this, getConfigLoader()) : headManager;
//
//            Bukkit.getPluginManager().registerEvents(new EventListeners(this), this);
//            Bukkit.getPluginManager().registerEvents(new ChunkListener(this), this);
//
//            protectionCompatManager = protectionCompatManager == null ?
//                                      new ProtectionCompatManagerSpigot(this) : protectionCompatManager;
//            Bukkit.getPluginManager().registerEvents(protectionCompatManager, this);
//
//            powerBlockManager = new PowerBlockManager(this, configLoader, databaseManager, getPLogger());
//
//            //noinspection ConstantConditions
//            if (worldListener == null)
//                worldListener = new WorldListener(powerBlockManager);
//
//            Bukkit.getPluginManager().registerEvents(worldListener, this);
//
//            pLogger.info("Successfully enabled BigDoors " + getDescription().getVersion());
//        }
//        catch (Exception exception)
//        {
//            successfulInit = false;
//            pLogger.logThrowable(exception);
//        }
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
        final File extensionsDir = new File(getDataDirectory() + Constants.BIGDOORS_EXTENSIONS_FOLDER);
        if (!extensionsDir.exists() && !extensionsDir.mkdirs())
        {
            pLogger.logThrowable(new IOException("Failed to create folder: " + extensionsDir));
            return;
        }

        Bukkit.getLogger().setLevel(Level.ALL);
        doorTypeLoader.loadDoorTypesFromDirectory();
    }

    @Override
    public IPlatformManagerSpigot getPlatformManagerSpigot()
    {
        return null;
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
        localizationManager.addResourcesFromClass(types);
        localizationManager.addResourcesFromClass(List.of(getClass()));
    }

    @Override
    public boolean isMainThread(long compareThread)
    {
        return compareThread == MAIN_THREAD_ID;
    }

    @Override
    public DebugReporter getDebugReporter()
    {
        return null;
    }

    @Override
    public String getVersion()
    {
        return null;
    }

    @Override
    public void restart()
    {
        if (!validVersion)
            return;

        configLoader.restart();

        shutdown();

        init();
    }

    @Override
    public void shutdown()
    {
    }

    @Override
    public void onDisable()
    {
        shutdown();
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
    public File getDataDirectory()
    {
        return getDataFolder();
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

    @Override
    public IPBlockDataFactory getPBlockDataFactory()
    {
        return blockDataFactory;
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
