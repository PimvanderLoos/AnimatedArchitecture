package nl.pim16aap2.bigdoors.spigot;

import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IBigDoorsToolUtil;
import nl.pim16aap2.bigdoors.api.IBlockAnalyzer;
import nl.pim16aap2.bigdoors.api.IChunkManager;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IEconomyManager;
import nl.pim16aap2.bigdoors.api.IGlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.api.IMessagingInterface;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPermissionsManager;
import nl.pim16aap2.bigdoors.api.IPowerBlockRedstoneManager;
import nl.pim16aap2.bigdoors.api.IProtectionCompatManager;
import nl.pim16aap2.bigdoors.api.ISoundEngine;
import nl.pim16aap2.bigdoors.api.factories.IBigDoorsEventFactory;
import nl.pim16aap2.bigdoors.api.factories.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IPBlockDataFactory;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.api.factories.IPWorldFactory;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
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
import nl.pim16aap2.bigdoors.spigot.events.BigDoorsSpigotEvent;
import nl.pim16aap2.bigdoors.spigot.listeners.ChunkListener;
import nl.pim16aap2.bigdoors.spigot.listeners.EventListeners;
import nl.pim16aap2.bigdoors.spigot.listeners.LoginMessageListener;
import nl.pim16aap2.bigdoors.spigot.listeners.LoginResourcePackListener;
import nl.pim16aap2.bigdoors.spigot.listeners.RedstoneListener;
import nl.pim16aap2.bigdoors.spigot.listeners.WorldListener;
import nl.pim16aap2.bigdoors.spigot.managers.HeadManager;
import nl.pim16aap2.bigdoors.spigot.managers.UpdateManager;
import nl.pim16aap2.bigdoors.spigot.util.api.IBigDoorsSpigotSubPlatform;
import org.bukkit.Bukkit;

import javax.inject.Singleton;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

@Singleton //
final class BigDoorsSpigotPlatform implements IBigDoorsPlatform
{
    private final long mainThreadId;

    private final BigDoorsPlugin plugin;

    private final RestartableHolder restartableHolder;

    @Getter
    private final IBigDoorsToolUtil bigDoorsToolUtil;

    @Getter
    private final IPWorldFactory pWorldFactory;

    @Getter
    private final IPLocationFactory pLocationFactory;

    @Getter
    private final IPBlockDataFactory pBlockDataFactory;

    @Getter
    private final IFallingBlockFactory fallingBlockFactory;

    @Getter
    private final IPPlayerFactory pPlayerFactory;

    @Getter
    private final IConfigLoader bigDoorsConfig;

    @Getter
    private final ISoundEngine soundEngine;

    @Getter
    private final IBlockAnalyzer blockAnalyzer;

    @Getter
    private final IPExecutor pExecutor;

    @Getter
    private final IGlowingBlockSpawner glowingBlockSpawner;

    @Getter
    private final IPLogger logger;

    @Getter
    private final ILocalizer localizer;

    @Getter
    private final IMessagingInterface messagingInterface;

    @Getter
    private final IMessageable messageableServer;

    @Getter
    private final IPServer pServer;

    @Getter
    private final DoorRegistry doorRegistry;

    @Getter
    private final AutoCloseScheduler autoCloseScheduler;

    @Getter
    private final IChunkManager chunkManager;

    @Getter
    private final DatabaseManager databaseManager;

    @Getter
    private final DoorActivityManager doorActivityManager;

    @Getter
    private final DoorSpecificationManager doorSpecificationManager;

    @Getter
    private final DoorTypeManager doorTypeManager;

    @Getter
    private final ToolUserManager toolUserManager;

    @Getter
    private final DelayedCommandInputManager delayedCommandInputManager;

    @Getter
    private final PowerBlockManager powerBlockManager;

    @Getter
    private final IEconomyManager economyManager;

    @Getter
    private final IPermissionsManager permissionsManager;

    @Getter
    private final IProtectionCompatManager protectionCompatManager;

    @Getter
    private final LimitsManager limitsManager;

    @Getter
    private final CommandFactory commandFactory;

    @Getter
    private final IBigDoorsEventFactory bigDoorsEventFactory;

    @Getter
    private final HeadManager headManager;

    @Getter
    private final UpdateManager updateManager;

    @Getter
    private final IPowerBlockRedstoneManager powerBlockRedstoneManager;

    @Getter
    private final DoorTypeLoader doorTypeLoader;

    @Getter
    private final LocalizationManager localizationManager;

    @Getter
    private final IBigDoorsSpigotSubPlatform spigotPlatform;


    @SuppressWarnings({"FieldCanBeLocal", "unused", "PMD.SingularField"})
    private final ChunkListener chunkListener;

    @SuppressWarnings({"FieldCanBeLocal", "unused", "PMD.SingularField"})
    private final EventListeners eventListeners;

    @SuppressWarnings({"FieldCanBeLocal", "unused", "PMD.SingularField"})
    private final LoginMessageListener loginMessageListener;

    @SuppressWarnings({"FieldCanBeLocal", "unused", "PMD.SingularField"})
    private final LoginResourcePackListener loginResourcePackListener;

    @SuppressWarnings({"FieldCanBeLocal", "unused", "PMD.SingularField"})
    private final RedstoneListener redstoneListener;

    @SuppressWarnings({"FieldCanBeLocal", "unused", "PMD.SingularField"})
    private final WorldListener worldListener;

    BigDoorsSpigotPlatform(BigDoorsSpigotComponent bigDoorsSpigotComponent, BigDoorsPlugin plugin, long mainThreadId)
        throws Exception
    {
        this.plugin = plugin;
        this.mainThreadId = mainThreadId;

        spigotPlatform = bigDoorsSpigotComponent.getSpigotPlatform();
        logger = bigDoorsSpigotComponent.getLogger();

        protectionCompatManager = bigDoorsSpigotComponent.getProtectionCompatManager();
        economyManager = bigDoorsSpigotComponent.getVaultManager();
        permissionsManager = bigDoorsSpigotComponent.getVaultManager();
        limitsManager = bigDoorsSpigotComponent.getLimitsManager();
        headManager = bigDoorsSpigotComponent.getHeadManager();
        updateManager = bigDoorsSpigotComponent.getUpdateManager();
        powerBlockManager = bigDoorsSpigotComponent.getPowerBlockManager();
        databaseManager = bigDoorsSpigotComponent.getDatabaseManager();
        doorRegistry = bigDoorsSpigotComponent.getDoorRegistry();
        localizationManager = bigDoorsSpigotComponent.getLocalizationManager();
        chunkManager = bigDoorsSpigotComponent.getIChunkManager();
        powerBlockRedstoneManager = bigDoorsSpigotComponent.getIPowerBlockRedstoneManager();
        doorActivityManager = bigDoorsSpigotComponent.getDoorActivityManager();
        doorSpecificationManager = bigDoorsSpigotComponent.getDoorSpecificationManager();
        doorTypeManager = bigDoorsSpigotComponent.getDoorTypeManager();
        toolUserManager = bigDoorsSpigotComponent.getToolUserManager();
        delayedCommandInputManager = bigDoorsSpigotComponent.getDelayedCommandInputManager();

        pLocationFactory = bigDoorsSpigotComponent.getIPLocationFactory();
        pWorldFactory = bigDoorsSpigotComponent.getIPWorldFactory();
        pPlayerFactory = bigDoorsSpigotComponent.getIPPlayerFactory();
        commandFactory = bigDoorsSpigotComponent.getCommandFactory();
        pBlockDataFactory = bigDoorsSpigotComponent.getBlockDataFactory();
        fallingBlockFactory = bigDoorsSpigotComponent.getFallingBlockFactory();
        bigDoorsEventFactory = bigDoorsSpigotComponent.getIBigDoorsEventFactory();

        redstoneListener = bigDoorsSpigotComponent.getRedstoneListener();
        loginResourcePackListener = bigDoorsSpigotComponent.getLoginResourcePackListener();
        chunkListener = bigDoorsSpigotComponent.getChunkListener();
        eventListeners = bigDoorsSpigotComponent.getEventListeners();
        loginMessageListener = bigDoorsSpigotComponent.getLoginMessageListener();

        bigDoorsConfig = bigDoorsSpigotComponent.getConfig();
        pExecutor = bigDoorsSpigotComponent.getPExecutor();
        worldListener = bigDoorsSpigotComponent.getWorldListener();
        glowingBlockSpawner = bigDoorsSpigotComponent.getIGlowingBlockSpawner();
        pServer = bigDoorsSpigotComponent.getIPServer();
        soundEngine = bigDoorsSpigotComponent.getISoundEngine();
        messagingInterface = bigDoorsSpigotComponent.getIMessagingInterface();
        messageableServer = bigDoorsSpigotComponent.getMessageable();
        bigDoorsToolUtil = bigDoorsSpigotComponent.getBigDoorsToolUtilSpigot();
        autoCloseScheduler = bigDoorsSpigotComponent.getAutoCloseScheduler();
        localizer = bigDoorsSpigotComponent.getILocalizer();
        blockAnalyzer = bigDoorsSpigotComponent.getBlockAnalyzer();
        doorTypeLoader = bigDoorsSpigotComponent.getDoorTypeLoader();
        restartableHolder = bigDoorsSpigotComponent.getRestartableHolder();

        initLocalization();
    }

    private void initLocalization()
    {
        final List<Class<?>> types = doorTypeManager.getEnabledDoorTypes().stream()
                                                    .map(DoorType::getDoorClass)
                                                    .collect(Collectors.toList());
        localizationManager.addResourcesFromClass(types);
        localizationManager.addResourcesFromClass(List.of(getClass()));
    }

    @Override
    public File getDataDirectory()
    {
        return plugin.getDataFolder();
    }

    @Override
    public void restartPlugin()
    {
        restartableHolder.restart();
    }

    @Override
    public void shutDownPlugin()
    {
        restartableHolder.shutdown();
    }

    @Override
    public void callDoorEvent(IBigDoorsEvent bigDoorsEvent)
    {
        if (!(bigDoorsEvent instanceof BigDoorsSpigotEvent))
        {
            getLogger().logThrowable(new IllegalArgumentException(
                "Event " + bigDoorsEvent.getEventName() +
                    ", is not a Spigot event, but it was called on the Spigot platform!"));
            return;
        }

        // Async events can only be called asynchronously and Sync events can only be called from the main thread.
        final boolean isMainThread = isMainThread(Thread.currentThread().getId());
        if (isMainThread && bigDoorsEvent.isAsynchronous())
            pExecutor.runAsync(() -> Bukkit.getPluginManager().callEvent((BigDoorsSpigotEvent) bigDoorsEvent));
        else if ((!isMainThread) && (!bigDoorsEvent.isAsynchronous()))
            pExecutor.runSync(() -> Bukkit.getPluginManager().callEvent((BigDoorsSpigotEvent) bigDoorsEvent));
        else
            Bukkit.getPluginManager().callEvent((BigDoorsSpigotEvent) bigDoorsEvent);
    }

    @Override
    public boolean isMainThread(long threadId)
    {
        return threadId == mainThreadId;
    }

    @Override
    public String getVersion()
    {
        return plugin.getDescription().getVersion();
    }

}
