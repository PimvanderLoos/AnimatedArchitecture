package nl.pim16aap2.bigdoors.spigot.core;

import lombok.Getter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.api.GlowingBlockSpawner;
import nl.pim16aap2.bigdoors.core.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.core.api.IBigDoorsToolUtil;
import nl.pim16aap2.bigdoors.core.api.IBlockAnalyzer;
import nl.pim16aap2.bigdoors.core.api.IChunkLoader;
import nl.pim16aap2.bigdoors.core.api.IConfigLoader;
import nl.pim16aap2.bigdoors.core.api.IEconomyManager;
import nl.pim16aap2.bigdoors.core.api.IMessageable;
import nl.pim16aap2.bigdoors.core.api.IMessagingInterface;
import nl.pim16aap2.bigdoors.core.api.IPExecutor;
import nl.pim16aap2.bigdoors.core.api.IPermissionsManager;
import nl.pim16aap2.bigdoors.core.api.IProtectionCompatManager;
import nl.pim16aap2.bigdoors.core.api.IRedstoneManager;
import nl.pim16aap2.bigdoors.core.api.factories.IAnimatedBlockFactory;
import nl.pim16aap2.bigdoors.core.api.factories.IBigDoorsEventFactory;
import nl.pim16aap2.bigdoors.core.api.factories.IGuiFactory;
import nl.pim16aap2.bigdoors.core.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.core.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.core.api.factories.IPWorldFactory;
import nl.pim16aap2.bigdoors.core.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.core.audio.IAudioPlayer;
import nl.pim16aap2.bigdoors.core.commands.CommandFactory;
import nl.pim16aap2.bigdoors.core.commands.IPServer;
import nl.pim16aap2.bigdoors.core.extensions.StructureTypeLoader;
import nl.pim16aap2.bigdoors.core.localization.ILocalizer;
import nl.pim16aap2.bigdoors.core.localization.LocalizationManager;
import nl.pim16aap2.bigdoors.core.managers.AnimatedBlockHookManager;
import nl.pim16aap2.bigdoors.core.managers.AnimationHookManager;
import nl.pim16aap2.bigdoors.core.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.core.managers.DelayedCommandInputManager;
import nl.pim16aap2.bigdoors.core.managers.LimitsManager;
import nl.pim16aap2.bigdoors.core.managers.PowerBlockManager;
import nl.pim16aap2.bigdoors.core.managers.StructureSpecificationManager;
import nl.pim16aap2.bigdoors.core.managers.StructureTypeManager;
import nl.pim16aap2.bigdoors.core.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.core.moveblocks.StructureActivityManager;
import nl.pim16aap2.bigdoors.spigot.core.comands.CommandManager;
import nl.pim16aap2.bigdoors.spigot.core.exceptions.InitializationException;
import nl.pim16aap2.bigdoors.spigot.core.listeners.ChunkListener;
import nl.pim16aap2.bigdoors.spigot.core.listeners.EventListeners;
import nl.pim16aap2.bigdoors.spigot.core.listeners.LoginMessageListener;
import nl.pim16aap2.bigdoors.spigot.core.listeners.LoginResourcePackListener;
import nl.pim16aap2.bigdoors.spigot.core.listeners.RedstoneListener;
import nl.pim16aap2.bigdoors.spigot.core.listeners.WorldListener;
import nl.pim16aap2.bigdoors.spigot.core.managers.HeadManager;
import nl.pim16aap2.bigdoors.spigot.core.managers.SubPlatformManager;
import nl.pim16aap2.bigdoors.spigot.util.api.IBigDoorsSpigotSubPlatform;
import nl.pim16aap2.bigdoors.core.storage.IStorage;
import nl.pim16aap2.bigdoors.core.structures.StructureRegistry;
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
