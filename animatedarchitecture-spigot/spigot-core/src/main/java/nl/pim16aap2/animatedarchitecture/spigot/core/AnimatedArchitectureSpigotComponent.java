package nl.pim16aap2.animatedarchitecture.spigot.core;

import dagger.BindsInstance;
import dagger.Component;
import nl.pim16aap2.animatedarchitecture.core.api.HighlightedBlockSpawner;
import nl.pim16aap2.animatedarchitecture.core.api.IChunkLoader;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IMessageable;
import nl.pim16aap2.animatedarchitecture.core.api.IMessagingInterface;
import nl.pim16aap2.animatedarchitecture.core.api.IRedstoneManager;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockFactory;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebugReporter;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IAnimatedArchitectureEventFactory;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ILocationFactory;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IPlayerFactory;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IWorldFactory;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.audio.AudioAnimationHook;
import nl.pim16aap2.animatedarchitecture.core.audio.IAudioPlayer;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandFactory;
import nl.pim16aap2.animatedarchitecture.core.commands.IServer;
import nl.pim16aap2.animatedarchitecture.core.events.IAnimatedArchitectureEventCaller;
import nl.pim16aap2.animatedarchitecture.core.extensions.StructureTypeLoader;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.localization.LocalizationManager;
import nl.pim16aap2.animatedarchitecture.core.localization.LocalizationModule;
import nl.pim16aap2.animatedarchitecture.core.managers.AnimatedBlockHookManager;
import nl.pim16aap2.animatedarchitecture.core.managers.AnimationHookManager;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.managers.DelayedCommandInputManager;
import nl.pim16aap2.animatedarchitecture.core.managers.LimitsManager;
import nl.pim16aap2.animatedarchitecture.core.managers.PowerBlockManager;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureSpecificationManager;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.StructureActivityManager;
import nl.pim16aap2.animatedarchitecture.core.storage.sqlite.SQLiteStorageModule;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAnimationRequestBuilder;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureRegistry;
import nl.pim16aap2.animatedarchitecture.core.util.structureretriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.util.updater.UpdateChecker;
import nl.pim16aap2.animatedarchitecture.core.util.versioning.BuildDataReader;
import nl.pim16aap2.animatedarchitecture.core.util.versioning.ProjectVersion;
import nl.pim16aap2.animatedarchitecture.spigot.core.animation.AnimationBlockDisplayModule;
import nl.pim16aap2.animatedarchitecture.spigot.core.comands.CommandManager;
import nl.pim16aap2.animatedarchitecture.spigot.core.compatiblity.ProtectionCompatManagerModule;
import nl.pim16aap2.animatedarchitecture.spigot.core.compatiblity.ProtectionCompatManagerSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.core.config.ConfigSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.core.config.ConfigSpigotModule;
import nl.pim16aap2.animatedarchitecture.spigot.core.gui.GuiFactory;
import nl.pim16aap2.animatedarchitecture.spigot.core.gui.GuiFactorySpigotModule;
import nl.pim16aap2.animatedarchitecture.spigot.core.implementations.AnimatedArchitectureEventsSpigotModule;
import nl.pim16aap2.animatedarchitecture.spigot.core.implementations.AnimatedArchitectureToolUtilSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.core.implementations.AnimatedArchitectureToolUtilSpigotModule;
import nl.pim16aap2.animatedarchitecture.spigot.core.implementations.AudioPlayerSpigotModule;
import nl.pim16aap2.animatedarchitecture.spigot.core.implementations.ChunkLoaderSpigotModule;
import nl.pim16aap2.animatedarchitecture.spigot.core.implementations.DebugReporterSpigotModule;
import nl.pim16aap2.animatedarchitecture.spigot.core.implementations.HighlightedBlockSpawnerModule;
import nl.pim16aap2.animatedarchitecture.spigot.core.implementations.LocationFactorySpigotModule;
import nl.pim16aap2.animatedarchitecture.spigot.core.implementations.PlayerFactorySpigotModule;
import nl.pim16aap2.animatedarchitecture.spigot.core.implementations.TextFactorySpigot;
import nl.pim16aap2.animatedarchitecture.spigot.core.implementations.TextFactorySpigotModule;
import nl.pim16aap2.animatedarchitecture.spigot.core.implementations.WorldFactorySpigotModule;
import nl.pim16aap2.animatedarchitecture.spigot.core.listeners.ChunkListener;
import nl.pim16aap2.animatedarchitecture.spigot.core.listeners.EventListeners;
import nl.pim16aap2.animatedarchitecture.spigot.core.listeners.LoginMessageListener;
import nl.pim16aap2.animatedarchitecture.spigot.core.listeners.LoginResourcePackListener;
import nl.pim16aap2.animatedarchitecture.spigot.core.listeners.RedstoneListener;
import nl.pim16aap2.animatedarchitecture.spigot.core.listeners.WorldListener;
import nl.pim16aap2.animatedarchitecture.spigot.core.managers.HeadManager;
import nl.pim16aap2.animatedarchitecture.spigot.core.managers.PowerBlockRedstoneManagerSpigotModule;
import nl.pim16aap2.animatedarchitecture.spigot.core.managers.VaultManager;
import nl.pim16aap2.animatedarchitecture.spigot.core.managers.VaultManagerModule;
import nl.pim16aap2.animatedarchitecture.spigot.core.util.BlockAnalyzerModule;
import nl.pim16aap2.animatedarchitecture.spigot.core.util.BlockAnalyzerProvider;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.ExecutorModule;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.MessagingInterfaceSpigotModule;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.SpigotServerModule;
import nl.pim16aap2.animatedarchitecture.spigot.util.text.TextComponentFactorySpigotModule;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Named;
import javax.inject.Singleton;

@SuppressWarnings("unused")
@Singleton
@Component(modules = {
    AnimatedArchitecturePluginModule.class,
    ProtectionCompatManagerModule.class,
    ConfigSpigotModule.class,
    LocalizationModule.class,
    ExecutorModule.class,
    HighlightedBlockSpawnerModule.class,
    SpigotServerModule.class,
    WorldFactorySpigotModule.class,
    LocationFactorySpigotModule.class,
    AnimatedArchitectureEventsSpigotModule.class,
    PlayerFactorySpigotModule.class,
    MessagingInterfaceSpigotModule.class,
    AudioPlayerSpigotModule.class,
    PowerBlockRedstoneManagerSpigotModule.class,
    AnimationBlockDisplayModule.class,
    HighlightedBlockSpawnerModule.class,
    SQLiteStorageModule.class,
    DebugReporterSpigotModule.class,
    VaultManagerModule.class,
    AnimatedArchitectureToolUtilSpigotModule.class,
    TextFactorySpigotModule.class,
    ChunkLoaderSpigotModule.class,
    GuiFactorySpigotModule.class,
    TextComponentFactorySpigotModule.class,
    BlockAnalyzerModule.class,
})
interface AnimatedArchitectureSpigotComponent
{
    @Component.Builder
    interface Builder
    {
        @BindsInstance
        Builder setPlugin(AnimatedArchitecturePlugin javaPlugin);

        @BindsInstance
        Builder setProjectVersion(ProjectVersion projectVersion);

        @BindsInstance
        Builder setUpdateChecker(UpdateChecker updateChecker);

        @BindsInstance
        Builder setRestartableHolder(RestartableHolder restartableHolder);

        AnimatedArchitectureSpigotComponent build();
    }

    JavaPlugin getAnimatedArchitectureJavaPlugin();

    CommandManager getCommandListener();

    RestartableHolder getRestartableHolder();

    ProjectVersion getProjectVersion();

    IAnimatedArchitectureEventCaller getDoorEventCaller();

    @Named("mainThreadId")
    long getMainThreadId();

    DebugReporter getDebugReporter();

    DebuggableRegistry getDebuggableRegistry();

    ProtectionCompatManagerSpigot getProtectionCompatManager();

    GuiFactory getGUIFactory();

    StructureAnimationRequestBuilder structureAnimationRequestBuilder();

    ConfigSpigot getConfig();

    RedstoneListener getRedstoneListener();

    LoginResourcePackListener getLoginResourcePackListener();

    IExecutor getExecutor();

    PowerBlockManager getPowerBlockManager();

    WorldListener getWorldListener();

    ChunkListener getChunkListener();

    EventListeners getEventListeners();

    LoginMessageListener getLoginMessageListener();

    VaultManager getVaultManager();

    HighlightedBlockSpawner getHighlightedBlockSpawner();

    LimitsManager getLimitsManager();

    HeadManager getHeadManager();

    IServer getServer();

    ILocationFactory getLocationFactory();

    IWorldFactory getWorldFactory();

    IPlayerFactory getPlayerFactory();

    StructureRetrieverFactory getStructureRetrieverFactory();

    IAudioPlayer getIAudioPlayer();

    AudioAnimationHook.Factory getAudioAnimationHookFactory();

    IMessagingInterface getIMessagingInterface();

    IChunkLoader getChunkLoader();

    @Named("MessageableServer")
    IMessageable getMessageable();

    IAnimatedArchitectureEventFactory getIAnimatedArchitectureEventFactory();

    IRedstoneManager getIPowerBlockRedstoneManager();

    AnimatedArchitectureToolUtilSpigot getAnimatedArchitectureToolUtilSpigot();

    TextFactorySpigot getTextFactorySpigot();

    DatabaseManager getDatabaseManager();

    StructureRegistry getDoorRegistry();

    BuildDataReader getBuildDataReader();

    StructureActivityManager getDoorActivityManager();

    StructureSpecificationManager getDoorSpecificationManager();

    StructureTypeManager getDoorTypeManager();

    ToolUserManager getToolUserManager();

    DelayedCommandInputManager getDelayedCommandInputManager();

    ILocalizer getILocalizer();

    LocalizationManager getLocalizationManager();

    IAnimatedBlockFactory getAnimatedBlockFactory();

    StructureTypeLoader getDoorTypeLoader();

    BlockAnalyzerProvider getBlockAnalyzerProvider();

    CommandFactory getCommandFactory();

    AnimatedBlockHookManager getAnimatedBlockHookManager();

    AnimationHookManager getAnimationHookManager();
}
