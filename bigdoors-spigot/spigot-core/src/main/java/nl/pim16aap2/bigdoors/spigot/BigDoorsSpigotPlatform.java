package nl.pim16aap2.bigdoors.spigot;

import lombok.Getter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.GlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IBigDoorsToolUtil;
import nl.pim16aap2.bigdoors.api.IBlockAnalyzer;
import nl.pim16aap2.bigdoors.api.IChunkLoader;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IEconomyManager;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.api.IMessagingInterface;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPermissionsManager;
import nl.pim16aap2.bigdoors.api.IProtectionCompatManager;
import nl.pim16aap2.bigdoors.api.IRedstoneManager;
import nl.pim16aap2.bigdoors.api.factories.IAnimatedBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IBigDoorsEventFactory;
import nl.pim16aap2.bigdoors.api.factories.IGuiFactory;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.api.factories.IPWorldFactory;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.audio.IAudioPlayer;
import nl.pim16aap2.bigdoors.commands.CommandFactory;
import nl.pim16aap2.bigdoors.commands.IPServer;
import nl.pim16aap2.bigdoors.extensions.StructureTypeLoader;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.localization.LocalizationManager;
import nl.pim16aap2.bigdoors.managers.AnimatedBlockHookManager;
import nl.pim16aap2.bigdoors.managers.AnimationHookManager;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DelayedCommandInputManager;
import nl.pim16aap2.bigdoors.managers.LimitsManager;
import nl.pim16aap2.bigdoors.managers.PowerBlockManager;
import nl.pim16aap2.bigdoors.managers.StructureSpecificationManager;
import nl.pim16aap2.bigdoors.managers.StructureTypeManager;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.moveblocks.StructureActivityManager;
import nl.pim16aap2.bigdoors.spigot.comands.CommandManager;
import nl.pim16aap2.bigdoors.spigot.exceptions.InitializationException;
import nl.pim16aap2.bigdoors.spigot.listeners.ChunkListener;
import nl.pim16aap2.bigdoors.spigot.listeners.EventListeners;
import nl.pim16aap2.bigdoors.spigot.listeners.LoginMessageListener;
import nl.pim16aap2.bigdoors.spigot.listeners.LoginResourcePackListener;
import nl.pim16aap2.bigdoors.spigot.listeners.RedstoneListener;
import nl.pim16aap2.bigdoors.spigot.listeners.WorldListener;
import nl.pim16aap2.bigdoors.spigot.managers.HeadManager;
import nl.pim16aap2.bigdoors.spigot.managers.SubPlatformManager;
import nl.pim16aap2.bigdoors.spigot.util.api.IBigDoorsSpigotSubPlatform;
import nl.pim16aap2.bigdoors.storage.IStorage;
import nl.pim16aap2.bigdoors.structures.StructureRegistry;
import org.jetbrains.annotations.Nullable;

import javax.inject.Singleton;
import java.util.function.Function;

@Flogger
@Singleton //
final class BigDoorsSpigotPlatform implements IBigDoorsPlatform
{
    private final BigDoorsSpigotComponent bigDoorsSpigotComponent;

    private final BigDoorsPlugin plugin;

    private final RestartableHolder restartableHolder;

    @Getter
    private final IBigDoorsToolUtil bigDoorsToolUtil;

    @Getter
    private final IPWorldFactory pWorldFactory;

    @Getter
    private final IPLocationFactory pLocationFactory;

    @Getter
    private final IAnimatedBlockFactory animatedBlockFactory;

    @Getter
    private final IGuiFactory guiFactory;

    @Getter
    private final IPPlayerFactory pPlayerFactory;

    @Getter
    private final IConfigLoader bigDoorsConfig;

    @Getter
    private final IAudioPlayer audioPlayer;

    @Getter
    private final IBlockAnalyzer blockAnalyzer;

    @Getter
    private final IPExecutor pExecutor;

    @Getter
    private final GlowingBlockSpawner glowingBlockSpawner;

    @Getter
    private final ILocalizer localizer;

    @Getter
    private final IMessagingInterface messagingInterface;

    @Getter
    private final IMessageable messageableServer;

    @Getter
    private final IPServer pServer;

    @Getter
    private final StructureRegistry doorRegistry;

    @Getter
    private final IChunkLoader chunkLoader;

    @Getter
    private final DatabaseManager databaseManager;

    @Getter
    private final StructureActivityManager doorActivityManager;

    @Getter
    private final StructureSpecificationManager doorSpecificationManager;

    @Getter
    private final StructureTypeManager doorTypeManager;

    @Getter
    private final ToolUserManager toolUserManager;

    @Getter
    private final DelayedCommandInputManager delayedCommandInputManager;

    @Getter
    private final AnimatedBlockHookManager animatedBlockHookManager;

    @Getter
    private final AnimationHookManager animationHookManager;

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
    private final IRedstoneManager powerBlockRedstoneManager;

    @Getter
    private final StructureTypeLoader doorTypeLoader;

    @Getter
    private final LocalizationManager localizationManager;

    @Getter
    private final IBigDoorsSpigotSubPlatform spigotSubPlatform;

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

    @Getter
    private final CommandManager commandListener;

    BigDoorsSpigotPlatform(BigDoorsSpigotComponent bigDoorsSpigotComponent, BigDoorsPlugin plugin)
        throws InitializationException
    {
        this.bigDoorsSpigotComponent = bigDoorsSpigotComponent;
        this.plugin = plugin;

        final SubPlatformManager subPlatformManagerSpigot = bigDoorsSpigotComponent.getSubPlatformManager();

        if (!subPlatformManagerSpigot.isValidPlatform())
            throw new InitializationException("Failed to initialize BigDoors SubPlatform version " +
                                                  subPlatformManagerSpigot.getSubPlatformVersion() +
                                                  " for server version: " +
                                                  subPlatformManagerSpigot.getServerVersion());

        databaseManager = bigDoorsSpigotComponent.getDatabaseManager();
        if (databaseManager.getDatabaseState() != IStorage.DatabaseState.OK)
            throw new InitializationException("Failed to initialize BigDoors database! Database state: " +
                                                  databaseManager.getDatabaseState().name());

        spigotSubPlatform = safeGetter(BigDoorsSpigotComponent::getSpigotSubPlatform);
        protectionCompatManager = safeGetter(BigDoorsSpigotComponent::getProtectionCompatManager);
        economyManager = safeGetter(BigDoorsSpigotComponent::getVaultManager);
        permissionsManager = safeGetter(BigDoorsSpigotComponent::getVaultManager);
        limitsManager = safeGetter(BigDoorsSpigotComponent::getLimitsManager);
        headManager = safeGetter(BigDoorsSpigotComponent::getHeadManager);
        powerBlockManager = safeGetter(BigDoorsSpigotComponent::getPowerBlockManager);
        doorRegistry = safeGetter(BigDoorsSpigotComponent::getDoorRegistry);
        localizationManager = safeGetter(BigDoorsSpigotComponent::getLocalizationManager);
        chunkLoader = safeGetter(BigDoorsSpigotComponent::getChunkLoader);
        powerBlockRedstoneManager = safeGetter(BigDoorsSpigotComponent::getIPowerBlockRedstoneManager);
        doorActivityManager = safeGetter(BigDoorsSpigotComponent::getDoorActivityManager);
        doorSpecificationManager = safeGetter(BigDoorsSpigotComponent::getDoorSpecificationManager);
        doorTypeManager = safeGetter(BigDoorsSpigotComponent::getDoorTypeManager);
        toolUserManager = safeGetter(BigDoorsSpigotComponent::getToolUserManager);
        delayedCommandInputManager = safeGetter(BigDoorsSpigotComponent::getDelayedCommandInputManager);
        animatedBlockHookManager = safeGetter(BigDoorsSpigotComponent::getAnimatedBlockHookManager);
        animationHookManager = safeGetter(BigDoorsSpigotComponent::getAnimationHookManager);

        pLocationFactory = safeGetter(BigDoorsSpigotComponent::getIPLocationFactory);
        pWorldFactory = safeGetter(BigDoorsSpigotComponent::getIPWorldFactory);
        pPlayerFactory = safeGetter(BigDoorsSpigotComponent::getIPPlayerFactory);
        commandFactory = safeGetter(BigDoorsSpigotComponent::getCommandFactory);
        animatedBlockFactory = safeGetter(BigDoorsSpigotComponent::getAnimatedBlockFactory);
        bigDoorsEventFactory = safeGetter(BigDoorsSpigotComponent::getIBigDoorsEventFactory);
        guiFactory = safeGetter(BigDoorsSpigotComponent::getGUIFactory);

        redstoneListener = safeGetter(BigDoorsSpigotComponent::getRedstoneListener);
        loginResourcePackListener = safeGetter(BigDoorsSpigotComponent::getLoginResourcePackListener);
        chunkListener = safeGetter(BigDoorsSpigotComponent::getChunkListener);
        eventListeners = safeGetter(BigDoorsSpigotComponent::getEventListeners);
        loginMessageListener = safeGetter(BigDoorsSpigotComponent::getLoginMessageListener);

        bigDoorsConfig = safeGetter(BigDoorsSpigotComponent::getConfig);
        pExecutor = safeGetter(BigDoorsSpigotComponent::getPExecutor);
        worldListener = safeGetter(BigDoorsSpigotComponent::getWorldListener);
        glowingBlockSpawner = safeGetter(BigDoorsSpigotComponent::getIGlowingBlockSpawner);
        pServer = safeGetter(BigDoorsSpigotComponent::getIPServer);
        audioPlayer = safeGetter(BigDoorsSpigotComponent::getIAudioPlayer);
        messagingInterface = safeGetter(BigDoorsSpigotComponent::getIMessagingInterface);
        messageableServer = safeGetter(BigDoorsSpigotComponent::getMessageable);
        bigDoorsToolUtil = safeGetter(BigDoorsSpigotComponent::getBigDoorsToolUtilSpigot);
        localizer = safeGetter(BigDoorsSpigotComponent::getILocalizer);
        blockAnalyzer = safeGetter(BigDoorsSpigotComponent::getBlockAnalyzer);
        doorTypeLoader = safeGetter(BigDoorsSpigotComponent::getDoorTypeLoader);
        restartableHolder = safeGetter(BigDoorsSpigotComponent::getRestartableHolder);
        commandListener = safeGetter(BigDoorsSpigotComponent::getCommandListener);

        initPlatform();
    }

    /**
     * Initializes stuff that doesn't need to happen in the constructor. E.g. registering hooks.
     *
     * @throws InitializationException
     */
    private void initPlatform()
        throws InitializationException
    {
        safeGetter(BigDoorsSpigotComponent::getDebuggableRegistry).registerDebuggable(restartableHolder);
        getAnimationHookManager().registerFactory(safeGetter(BigDoorsSpigotComponent::getAudioAnimationHookFactory));
    }

    @SuppressWarnings("NullAway") // NullAway doesn't like nullable in functional interfaces
    private <T> T safeGetter(Function<BigDoorsSpigotComponent, @Nullable T> fun)
        throws InitializationException
    {
        final @Nullable T ret;
        try
        {
            ret = fun.apply(bigDoorsSpigotComponent);
        }
        catch (Exception e)
        {
            throw e.getMessage() == null ?
                  new InitializationException(e) : new InitializationException(e.getMessage(), e);
        }
        if (ret == null)
            throw new InitializationException(
                "Failed to instantiate the BigDoors platform for Spigot: Missing dependency!");
        return ret;
    }

    @Override
    public void restartPlugin()
    {
        pExecutor.runOnMainThread(restartableHolder::restart);
    }

    @Override
    public void shutDownPlugin()
    {
        restartableHolder.shutDown();
    }

    @Override
    public String getVersion()
    {
        return plugin.getDescription().getVersion();
    }
}
