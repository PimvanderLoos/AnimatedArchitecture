package nl.pim16aap2.animatedarchitecture.spigot.core;

import lombok.Getter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.StructureActivityManager;
import nl.pim16aap2.animatedarchitecture.core.api.HighlightedBlockSpawner;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatform;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitectureToolUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IChunkLoader;
import nl.pim16aap2.animatedarchitecture.core.api.IConfig;
import nl.pim16aap2.animatedarchitecture.core.api.IEconomyManager;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IMessageable;
import nl.pim16aap2.animatedarchitecture.core.api.IMessagingInterface;
import nl.pim16aap2.animatedarchitecture.core.api.IPermissionsManager;
import nl.pim16aap2.animatedarchitecture.core.api.IProtectionHookManager;
import nl.pim16aap2.animatedarchitecture.core.api.IRedstoneManager;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockFactory;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IAnimatedArchitectureEventFactory;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ILocationFactory;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IPlayerFactory;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IWorldFactory;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.audio.IAudioPlayer;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandFactory;
import nl.pim16aap2.animatedarchitecture.core.commands.IServer;
import nl.pim16aap2.animatedarchitecture.core.extensions.StructureTypeLoader;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.localization.LocalizationManager;
import nl.pim16aap2.animatedarchitecture.core.managers.AnimatedBlockHookManager;
import nl.pim16aap2.animatedarchitecture.core.managers.AnimationHookManager;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.managers.DelayedCommandInputManager;
import nl.pim16aap2.animatedarchitecture.core.managers.LimitsManager;
import nl.pim16aap2.animatedarchitecture.core.managers.PowerBlockManager;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureSpecificationManager;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;
import nl.pim16aap2.animatedarchitecture.core.storage.IStorage;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAnimationRequestBuilder;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureRegistry;
import nl.pim16aap2.animatedarchitecture.core.util.structureretriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.util.versioning.BuildDataReader;
import nl.pim16aap2.animatedarchitecture.core.util.versioning.ProjectVersion;
import nl.pim16aap2.animatedarchitecture.spigot.core.comands.CommandManager;
import nl.pim16aap2.animatedarchitecture.spigot.core.exceptions.InitializationException;
import nl.pim16aap2.animatedarchitecture.spigot.core.gui.GuiFactory;
import nl.pim16aap2.animatedarchitecture.spigot.core.listeners.ChunkListener;
import nl.pim16aap2.animatedarchitecture.spigot.core.listeners.EventListeners;
import nl.pim16aap2.animatedarchitecture.spigot.core.listeners.LoginMessageListener;
import nl.pim16aap2.animatedarchitecture.spigot.core.listeners.LoginResourcePackListener;
import nl.pim16aap2.animatedarchitecture.spigot.core.listeners.RedstoneListener;
import nl.pim16aap2.animatedarchitecture.spigot.core.listeners.WorldListener;
import nl.pim16aap2.animatedarchitecture.spigot.core.managers.HeadManager;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IBlockAnalyzerSpigot;
import org.jetbrains.annotations.Nullable;

import javax.inject.Singleton;
import java.util.function.Function;

@Flogger
@Singleton
public final class AnimatedArchitectureSpigotPlatform implements IAnimatedArchitecturePlatform
{
    private final AnimatedArchitectureSpigotComponent animatedArchitectureSpigotComponent;

    private final RestartableHolder restartableHolder;

    @Getter
    private final IAnimatedArchitectureToolUtil animatedArchitectureToolUtil;

    @Getter
    private final IWorldFactory worldFactory;

    @Getter
    private final ILocationFactory locationFactory;

    @Getter
    private final IAnimatedBlockFactory animatedBlockFactory;

    @Getter
    private final GuiFactory guiFactory;

    private final StructureAnimationRequestBuilder structureAnimationRequestBuilder;

    @Getter
    private final IPlayerFactory playerFactory;

    @Getter
    private final StructureRetrieverFactory structureRetrieverFactory;

    @Getter
    private final IConfig animatedArchitectureConfig;

    @Getter
    private final IAudioPlayer audioPlayer;

    @Getter
    private final IBlockAnalyzerSpigot blockAnalyzer;

    @Getter
    private final IExecutor executor;

    @Getter
    private final HighlightedBlockSpawner highlightedBlockSpawner;

    @Getter
    private final ILocalizer localizer;

    @Getter
    private final IMessagingInterface messagingInterface;

    @Getter
    private final IMessageable messageableServer;

    @Getter
    private final IServer server;

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
    private final IProtectionHookManager protectionCompatManager;

    @Getter
    private final LimitsManager limitsManager;

    @Getter
    private final CommandFactory commandFactory;

    @Getter
    private final IAnimatedArchitectureEventFactory animatedArchitectureEventFactory;

    @Getter
    private final HeadManager headManager;

    @Getter
    private final IRedstoneManager powerBlockRedstoneManager;

    @Getter
    private final StructureTypeLoader doorTypeLoader;

    @Getter
    private final LocalizationManager localizationManager;

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

    @Getter
    private final BuildDataReader.BuildData buildData;

    @Getter
    private final ProjectVersion projectVersion;

    AnimatedArchitectureSpigotPlatform(AnimatedArchitectureSpigotComponent animatedArchitectureSpigotComponent)
        throws InitializationException
    {
        this.animatedArchitectureSpigotComponent = animatedArchitectureSpigotComponent;

        databaseManager = animatedArchitectureSpigotComponent.getDatabaseManager();
        if (databaseManager.getDatabaseState() != IStorage.DatabaseState.OK)
            throw new InitializationException("Failed to initialize AnimatedArchitecture database! Database state: " +
                                                  databaseManager.getDatabaseState().name());

        protectionCompatManager = safeGetter(AnimatedArchitectureSpigotComponent::getProtectionHookManager);
        economyManager = safeGetter(AnimatedArchitectureSpigotComponent::getVaultManager);
        permissionsManager = safeGetter(AnimatedArchitectureSpigotComponent::getVaultManager);
        limitsManager = safeGetter(AnimatedArchitectureSpigotComponent::getLimitsManager);
        headManager = safeGetter(AnimatedArchitectureSpigotComponent::getHeadManager);
        powerBlockManager = safeGetter(AnimatedArchitectureSpigotComponent::getPowerBlockManager);
        doorRegistry = safeGetter(AnimatedArchitectureSpigotComponent::getDoorRegistry);
        localizationManager = safeGetter(AnimatedArchitectureSpigotComponent::getLocalizationManager);
        chunkLoader = safeGetter(AnimatedArchitectureSpigotComponent::getChunkLoader);
        powerBlockRedstoneManager = safeGetter(AnimatedArchitectureSpigotComponent::getIPowerBlockRedstoneManager);
        doorActivityManager = safeGetter(AnimatedArchitectureSpigotComponent::getDoorActivityManager);
        doorSpecificationManager = safeGetter(AnimatedArchitectureSpigotComponent::getDoorSpecificationManager);
        doorTypeManager = safeGetter(AnimatedArchitectureSpigotComponent::getDoorTypeManager);
        toolUserManager = safeGetter(AnimatedArchitectureSpigotComponent::getToolUserManager);
        delayedCommandInputManager = safeGetter(AnimatedArchitectureSpigotComponent::getDelayedCommandInputManager);
        animatedBlockHookManager = safeGetter(AnimatedArchitectureSpigotComponent::getAnimatedBlockHookManager);
        animationHookManager = safeGetter(AnimatedArchitectureSpigotComponent::getAnimationHookManager);

        locationFactory = safeGetter(AnimatedArchitectureSpigotComponent::getLocationFactory);
        worldFactory = safeGetter(AnimatedArchitectureSpigotComponent::getWorldFactory);
        playerFactory = safeGetter(AnimatedArchitectureSpigotComponent::getPlayerFactory);
        structureRetrieverFactory = safeGetter(AnimatedArchitectureSpigotComponent::getStructureRetrieverFactory);
        commandFactory = safeGetter(AnimatedArchitectureSpigotComponent::getCommandFactory);
        animatedBlockFactory = safeGetter(AnimatedArchitectureSpigotComponent::getAnimatedBlockFactory);
        animatedArchitectureEventFactory = safeGetter(
            AnimatedArchitectureSpigotComponent::getIAnimatedArchitectureEventFactory);
        guiFactory = safeGetter(AnimatedArchitectureSpigotComponent::getGUIFactory);
        structureAnimationRequestBuilder = safeGetter(
            AnimatedArchitectureSpigotComponent::structureAnimationRequestBuilder);

        redstoneListener = safeGetter(AnimatedArchitectureSpigotComponent::getRedstoneListener);
        loginResourcePackListener = safeGetter(AnimatedArchitectureSpigotComponent::getLoginResourcePackListener);
        chunkListener = safeGetter(AnimatedArchitectureSpigotComponent::getChunkListener);
        eventListeners = safeGetter(AnimatedArchitectureSpigotComponent::getEventListeners);
        loginMessageListener = safeGetter(AnimatedArchitectureSpigotComponent::getLoginMessageListener);

        animatedArchitectureConfig = safeGetter(AnimatedArchitectureSpigotComponent::getConfig);
        executor = safeGetter(AnimatedArchitectureSpigotComponent::getExecutor);
        worldListener = safeGetter(AnimatedArchitectureSpigotComponent::getWorldListener);
        highlightedBlockSpawner = safeGetter(AnimatedArchitectureSpigotComponent::getHighlightedBlockSpawner);
        server = safeGetter(AnimatedArchitectureSpigotComponent::getServer);
        audioPlayer = safeGetter(AnimatedArchitectureSpigotComponent::getIAudioPlayer);
        messagingInterface = safeGetter(AnimatedArchitectureSpigotComponent::getIMessagingInterface);
        messageableServer = safeGetter(AnimatedArchitectureSpigotComponent::getMessageable);
        animatedArchitectureToolUtil = safeGetter(
            AnimatedArchitectureSpigotComponent::getAnimatedArchitectureToolUtilSpigot);
        localizer = safeGetter(AnimatedArchitectureSpigotComponent::getILocalizer);
        blockAnalyzer = safeGetter(AnimatedArchitectureSpigotComponent::getBlockAnalyzerProvider).getBlockAnalyzer();
        doorTypeLoader = safeGetter(AnimatedArchitectureSpigotComponent::getDoorTypeLoader);
        restartableHolder = safeGetter(AnimatedArchitectureSpigotComponent::getRestartableHolder);
        projectVersion = safeGetter(AnimatedArchitectureSpigotComponent::getProjectVersion);
        commandListener = safeGetter(AnimatedArchitectureSpigotComponent::getCommandListener);
        buildData = safeGetter(AnimatedArchitectureSpigotComponent::getBuildDataReader).getBuildData();

        initPlatform();
    }

    /**
     * Initializes stuff that doesn't need to happen in the constructor. E.g. registering hooks.
     *
     * @throws InitializationException
     *     When an error occurred while initializing the platform.
     */
    private void initPlatform()
        throws InitializationException
    {
        safeGetter(AnimatedArchitectureSpigotComponent::getDebuggableRegistry).registerDebuggable(restartableHolder);
        getAnimationHookManager().registerFactory(
            safeGetter(AnimatedArchitectureSpigotComponent::getAudioAnimationHookFactory));
    }

    @SuppressWarnings("NullAway") // NullAway doesn't like nullable in functional interfaces
    private <T> T safeGetter(Function<AnimatedArchitectureSpigotComponent, @Nullable T> fun)
        throws InitializationException
    {
        final @Nullable T ret;
        try
        {
            ret = fun.apply(animatedArchitectureSpigotComponent);
        }
        catch (Exception e)
        {
            throw e.getMessage() == null ?
                  new InitializationException(e) : new InitializationException(e.getMessage(), e);
        }
        if (ret == null)
            throw new InitializationException(
                "Failed to instantiate the AnimatedArchitecture platform for Spigot: Missing dependency!");
        return ret;
    }

    @Override
    public StructureAnimationRequestBuilder.IBuilderStructure getStructureAnimationRequestBuilder()
    {
        return structureAnimationRequestBuilder.builder();
    }

    @Override
    public void restartPlugin()
    {
        executor.runOnMainThread(restartableHolder::restart);
    }

    @Override
    public void shutDownPlugin()
    {
        restartableHolder.shutDown();
    }
}
