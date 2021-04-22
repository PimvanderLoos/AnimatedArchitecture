package nl.pim16aap2.bigdoors.spigot;

import lombok.Getter;
import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.DebugReporter;
import nl.pim16aap2.bigdoors.api.IBlockAnalyzer;
import nl.pim16aap2.bigdoors.api.IChunkManager;
import nl.pim16aap2.bigdoors.api.IEconomyManager;
import nl.pim16aap2.bigdoors.api.IGlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.api.IMessagingInterface;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPLocationConst;
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
import nl.pim16aap2.bigdoors.doors.DoorOpener;
import nl.pim16aap2.bigdoors.events.IBigDoorsEvent;
import nl.pim16aap2.bigdoors.extensions.DoorTypeLoader;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.logging.PLogger;
import nl.pim16aap2.bigdoors.managers.AutoCloseScheduler;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DelayedCommandInputManager;
import nl.pim16aap2.bigdoors.managers.DoorActivityManager;
import nl.pim16aap2.bigdoors.managers.DoorRegistry;
import nl.pim16aap2.bigdoors.managers.DoorSpecificationManager;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import nl.pim16aap2.bigdoors.managers.PowerBlockManager;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.spigot.compatiblity.ProtectionCompatManagerSpigot;
import nl.pim16aap2.bigdoors.spigot.config.ConfigLoaderSpigot;
import nl.pim16aap2.bigdoors.spigot.events.BigDoorsSpigotEvent;
import nl.pim16aap2.bigdoors.spigot.factories.BigDoorsEventFactorySpigot;
import nl.pim16aap2.bigdoors.spigot.factories.PLocationFactorySpigot;
import nl.pim16aap2.bigdoors.spigot.factories.PPlayerFactorySpigot;
import nl.pim16aap2.bigdoors.spigot.factories.PWorldFactorySpigot;
import nl.pim16aap2.bigdoors.spigot.gui.GUI;
import nl.pim16aap2.bigdoors.spigot.implementations.BigDoorsToolUtilSpigot;
import nl.pim16aap2.bigdoors.spigot.listeners.ChunkListener;
import nl.pim16aap2.bigdoors.spigot.listeners.EventListeners;
import nl.pim16aap2.bigdoors.spigot.listeners.GUIListener;
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
import nl.pim16aap2.bigdoors.spigot.util.implementations.PSoundEngineSpigot;
import nl.pim16aap2.bigdoors.storage.IStorage;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.messages.Messages;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Represents the implementation of {@link BigDoorsSpigotAbstract}.
 *
 * @author Pim
 */
public final class BigDoorsSpigot extends BigDoorsSpigotAbstract
{
    private static BigDoorsSpigot INSTANCE;
    private static long MAINTHREADID = -1;

    private final @NonNull PLogger pLogger = new PLogger(new File(getDataFolder(), "log.txt"));

    @Getter
    private ConfigLoaderSpigot configLoader;
    private Metrics metrics;

    @Getter
    private Messages messages;

    private boolean validVersion = false;
    private final @NonNull IPExecutor pExecutor;
    private Map<UUID, GUI> playerGUIs;
    private final Set<IRestartable> restartables = new HashSet<>();

    @Getter
    private ProtectionCompatManagerSpigot protectionCompatManager;
    private LoginResourcePackListener rPackHandler;

    @Getter
    private PowerBlockManager powerBlockManager;

    @Getter
    private VaultManager vaultManager;

    private IGlowingBlockSpawner glowingBlockSpawner;

    @Getter
    private HeadManager headManager;
    private UpdateManager updateManager;

    private boolean successfulInit = true;

    @Getter
    private final @NonNull IPLocationFactory pLocationFactory = new PLocationFactorySpigot();

    @Getter
    private final @NonNull IPWorldFactory pWorldFactory = new PWorldFactorySpigot();

    @Getter
    private final @NonNull IPPlayerFactory pPlayerFactory = new PPlayerFactorySpigot();

    @Getter
    private final @NonNull ISoundEngine soundEngine = new PSoundEngineSpigot();

    @Getter
    private final @NonNull IMessagingInterface messagingInterface = new MessagingInterfaceSpigot(this);

    @Getter
    private final @NonNull IChunkManager chunkManager = ChunkManagerSpigot.get();

    @Getter
    private final @NonNull IBigDoorsEventFactory doorActionEventFactory = new BigDoorsEventFactorySpigot();

    @Getter
    private final @NonNull IPowerBlockRedstoneManager powerBlockRedstoneManager = PowerBlockRedstoneManagerSpigot.get();

    @Getter
    private final @NonNull BigDoorsToolUtilSpigot bigDoorsToolUtil;

    @Getter
    private DatabaseManager databaseManager;

    @Getter
    private final @NonNull DoorOpener doorOpener;

    @Getter
    private final @NonNull DoorRegistry doorRegistry = new DoorRegistry();

    @Getter
    private final @NonNull AutoCloseScheduler autoCloseScheduler = new AutoCloseScheduler();

    @Getter
    private final @NonNull DoorActivityManager doorActivityManager = new DoorActivityManager(this);

    @Getter
    private final @NonNull DoorSpecificationManager doorSpecificationManager = new DoorSpecificationManager();

    @Getter
    private final @NonNull DoorTypeManager doorTypeManager = new DoorTypeManager();

    @Getter
    private final @NonNull ToolUserManager toolUserManager = new ToolUserManager(this);

    @Getter
    private final @NonNull DelayedCommandInputManager delayedCommandInputManager = new DelayedCommandInputManager();

    public BigDoorsSpigot()
    {
        INSTANCE = this;
        BigDoors.get().setBigDoorsPlatform(this);
        BigDoors.get().registerRestartable(this);

        MAINTHREADID = Thread.currentThread().getId();
        pExecutor = new PExecutorSpigot(this);
        bigDoorsToolUtil = new BigDoorsToolUtilSpigot();

        doorOpener = new DoorOpener();

        try
        {
            glowingBlockSpawner = new GlowingBlockSpawner(this);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onEnable()
    {
        Bukkit.getLogger().setLevel(Level.FINER);

        try
        {
            // Register this here so it can check for updates even when loaded on an incorrect version.
            updateManager = new UpdateManager(this, 58669);

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

            RedstoneListener.init(this);
            LoginResourcePackListener.init(this, configLoader.resourcePack());

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

            headManager = HeadManager.init(this, getConfigLoader());

            Bukkit.getPluginManager().registerEvents(new EventListeners(this), this);
            Bukkit.getPluginManager().registerEvents(new GUIListener(this), this);
            Bukkit.getPluginManager().registerEvents(new ChunkListener(this), this);

            protectionCompatManager = ProtectionCompatManagerSpigot.init(this);
            Bukkit.getPluginManager().registerEvents(protectionCompatManager, this);

            powerBlockManager = new PowerBlockManager(this, configLoader, databaseManager, getPLogger());
            Bukkit.getPluginManager().registerEvents(WorldListener.init(powerBlockManager), this);

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
        final @NonNull File extensionsDir = new File(BigDoors.get().getPlatform().getDataDirectory() +
                                                         Constants.BIGDOORS_EXTENSIONS_FOLDER);
        if (!extensionsDir.exists())
            if (!extensionsDir.mkdirs())
            {
                BigDoors.get().getPLogger()
                        .logThrowable(new IOException("Failed to create folder: " + extensionsDir));
                return;
            }

        Bukkit.getLogger().setLevel(Level.ALL);
        DoorTypeLoader.get().loadDoorTypesFromDirectory();
    }

    @Override
    public @NonNull IPlatformManagerSpigot getPlatformManagerSpigot()
    {
        return PlatformManagerSpigot.get();
    }

    public static @NonNull BigDoorsSpigot get()
    {
        return INSTANCE;
    }

    private void init()
    {
        if (!validVersion)
            return;

        configLoader.restart();
        messages = new Messages(this, getDataFolder(), getConfigLoader().languageFile(), getPLogger());
        playerGUIs = new HashMap<>();

        updateManager.setEnabled(getConfigLoader().checkForUpdates(), getConfigLoader().autoDLUpdate());
    }

    @Override
    public @NonNull File getDataDirectory()
    {
        return getDataFolder();
    }

    @Override
    public @NonNull IPBlockDataFactory getPBlockDataFactory()
    {
        return PlatformManagerSpigot.get().getSpigotPlatform().getPBlockDataFactory();
    }

    @Override
    public @NonNull IFallingBlockFactory getFallingBlockFactory()
    {
        return PlatformManagerSpigot.get().getSpigotPlatform().getFallingBlockFactory();
    }

    @Override
    public @NonNull IMessageable getMessageableServer()
    {
        return MessageableServerSpigot.get();
    }

    @Override
    public @NonNull IBlockAnalyzer getBlockAnalyzer()
    {
        return PlatformManagerSpigot.get().getSpigotPlatform().getBlockAnalyzer();
    }

    @Override
    public boolean isMainThread(final long compareThread)
    {
        return compareThread == MAINTHREADID;
    }

    @Override
    public @NonNull IPExecutor getPExecutor()
    {
        return pExecutor;
    }

    public @NonNull Optional<String> canBreakBlock(final @NonNull IPPlayer player, final @NonNull IPLocationConst loc)
    {
        return protectionCompatManager.canBreakBlock(player, loc);
    }

    public @NonNull Optional<String> canBreakBlocksBetweenLocs(final @NonNull IPPlayer player,
                                                               final @NonNull Vector3DiConst pos1,
                                                               final @NonNull Vector3DiConst pos2,
                                                               final @NonNull IPWorld world)
    {
        return protectionCompatManager.canBreakBlocksBetweenLocs(player, pos1, pos2, world);
    }

    @Override
    public void registerRestartable(final @NonNull IRestartable restartable)
    {
        restartables.add(restartable);
    }

    @Override
    public boolean isRestartableRegistered(final @NonNull IRestartable restartable)
    {
        return restartables.contains(restartable);
    }

    @Override
    public void deregisterRestartable(final @NonNull IRestartable restartable)
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
        playerGUIs.forEach((key, value) -> value.close());
        playerGUIs.clear();

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
    public @NonNull IEconomyManager getEconomyManager()
    {
        return vaultManager;
    }

    @Override
    public @NonNull IPermissionsManager getPermissionsManager()
    {
        return vaultManager;
    }

    public @NonNull IFallingBlockFactory getFABF()
    {
        return PlatformManagerSpigot.get().getSpigotPlatform().getFallingBlockFactory();
    }

    @Override
    public @NonNull Optional<IGlowingBlockSpawner> getGlowingBlockSpawner()
    {
        return Optional.ofNullable(glowingBlockSpawner);
    }

    public @NonNull BigDoorsSpigot getPlugin()
    {
        return this;
    }

    public @NonNull Optional<GUI> getGUIUser(final @NonNull Player player)
    {
        GUI gui = null;
        if (playerGUIs.containsKey(player.getUniqueId()))
            gui = playerGUIs.get(player.getUniqueId());
        return Optional.ofNullable(gui);
    }

    public void addGUIUser(final @NonNull GUI gui)
    {
        playerGUIs.put(gui.getGuiHolder().getUUID(), gui);
    }

    public void removeGUIUser(final @NonNull GUI gui)
    {
        playerGUIs.remove(gui.getGuiHolder().getUUID());
    }

    public void onPlayerLogout(final @NonNull Player player)
    {
        getDelayedCommandInputManager().cancelAll(SpigotAdapter.wrapPlayer(player));
        playerGUIs.remove(player.getUniqueId());
        toolUserManager.abortToolUser(player.getUniqueId());
    }

    @Override
    public @NonNull String getVersion()
    {
        return BigDoorsSpigot.get().getDescription().getVersion();
    }

    @Override
    public @NonNull DebugReporter getDebugReporter()
    {
        return new DebugReporterSpigot(this);
    }

    // Get the logger.
    @Override
    public @NonNull IPLogger getPLogger()
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
    public @NonNull String getLoginMessage()
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
    public void callDoorEvent(final @NonNull IBigDoorsEvent doorEvent)
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
