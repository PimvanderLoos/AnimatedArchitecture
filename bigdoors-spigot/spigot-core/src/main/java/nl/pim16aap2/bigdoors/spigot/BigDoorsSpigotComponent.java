package nl.pim16aap2.bigdoors.spigot;

import dagger.BindsInstance;
import dagger.Component;
import nl.pim16aap2.bigdoors.api.GlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.IBlockAnalyzer;
import nl.pim16aap2.bigdoors.api.IChunkLoader;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.api.IMessagingInterface;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPowerBlockRedstoneManager;
import nl.pim16aap2.bigdoors.api.debugging.DebugReporter;
import nl.pim16aap2.bigdoors.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.api.factories.IAnimatedBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IBigDoorsEventFactory;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.api.factories.IPWorldFactory;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.audio.AudioAnimationHook;
import nl.pim16aap2.bigdoors.audio.IAudioPlayer;
import nl.pim16aap2.bigdoors.commands.CommandFactory;
import nl.pim16aap2.bigdoors.commands.IPServer;
import nl.pim16aap2.bigdoors.events.IDoorEventCaller;
import nl.pim16aap2.bigdoors.extensions.DoorTypeLoader;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.localization.LocalizationManager;
import nl.pim16aap2.bigdoors.localization.LocalizationModule;
import nl.pim16aap2.bigdoors.managers.AnimatedBlockHookManager;
import nl.pim16aap2.bigdoors.managers.AnimationHookManager;
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
import nl.pim16aap2.bigdoors.spigot.comands.CommandManager;
import nl.pim16aap2.bigdoors.spigot.compatiblity.ProtectionCompatManagerModule;
import nl.pim16aap2.bigdoors.spigot.compatiblity.ProtectionCompatManagerSpigot;
import nl.pim16aap2.bigdoors.spigot.config.ConfigLoaderSpigot;
import nl.pim16aap2.bigdoors.spigot.config.ConfigLoaderSpigotModule;
import nl.pim16aap2.bigdoors.spigot.factories.bigdoorseventfactory.BigDoorsEventsSpigotModule;
import nl.pim16aap2.bigdoors.spigot.factories.plocationfactory.PLocationFactorySpigotModule;
import nl.pim16aap2.bigdoors.spigot.factories.pplayerfactory.PPlayerFactorySpigotModule;
import nl.pim16aap2.bigdoors.spigot.factories.pworldfactory.PWorldFactorySpigotModule;
import nl.pim16aap2.bigdoors.spigot.implementations.BigDoorsToolUtilSpigot;
import nl.pim16aap2.bigdoors.spigot.implementations.BigDoorsToolUtilSpigotModule;
import nl.pim16aap2.bigdoors.spigot.implementations.ChunkLoaderSpigotModule;
import nl.pim16aap2.bigdoors.spigot.implementations.TextFactorySpigot;
import nl.pim16aap2.bigdoors.spigot.implementations.TextFactorySpigotModule;
import nl.pim16aap2.bigdoors.spigot.listeners.ChunkListener;
import nl.pim16aap2.bigdoors.spigot.listeners.EventListeners;
import nl.pim16aap2.bigdoors.spigot.listeners.LoginMessageListener;
import nl.pim16aap2.bigdoors.spigot.listeners.LoginResourcePackListener;
import nl.pim16aap2.bigdoors.spigot.listeners.RedstoneListener;
import nl.pim16aap2.bigdoors.spigot.listeners.WorldListener;
import nl.pim16aap2.bigdoors.spigot.managers.HeadManager;
import nl.pim16aap2.bigdoors.spigot.managers.PlatformManagerSpigotModule;
import nl.pim16aap2.bigdoors.spigot.managers.PowerBlockRedstoneManagerSpigotModule;
import nl.pim16aap2.bigdoors.spigot.managers.SubPlatformManager;
import nl.pim16aap2.bigdoors.spigot.managers.UpdateManager;
import nl.pim16aap2.bigdoors.spigot.managers.VaultManager;
import nl.pim16aap2.bigdoors.spigot.managers.VaultManagerModule;
import nl.pim16aap2.bigdoors.spigot.util.DebugReporterSpigotModule;
import nl.pim16aap2.bigdoors.spigot.util.api.IBigDoorsSpigotSubPlatform;
import nl.pim16aap2.bigdoors.spigot.util.implementations.audio.AudioPlayerSpigotModule;
import nl.pim16aap2.bigdoors.spigot.util.implementations.glowingblocks.GlowingBlockSpawnerModule;
import nl.pim16aap2.bigdoors.spigot.util.implementations.messageable.MessagingInterfaceSpigotModule;
import nl.pim16aap2.bigdoors.spigot.util.implementations.pexecutor.PExecutorModule;
import nl.pim16aap2.bigdoors.spigot.util.implementations.pserver.PServerModule;
import nl.pim16aap2.bigdoors.storage.sqlite.SQLiteStorageModule;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Named;
import javax.inject.Singleton;

@SuppressWarnings("unused")
@Singleton
@Component(modules = {
    BigDoorsPluginModule.class,
    PlatformManagerSpigotModule.class,
    ProtectionCompatManagerModule.class,
    ConfigLoaderSpigotModule.class,
    LocalizationModule.class,
    PExecutorModule.class,
    GlowingBlockSpawnerModule.class,
    PServerModule.class,
    PWorldFactorySpigotModule.class,
    PLocationFactorySpigotModule.class,
    BigDoorsEventsSpigotModule.class,
    PPlayerFactorySpigotModule.class,
    MessagingInterfaceSpigotModule.class,
    AudioPlayerSpigotModule.class,
    PowerBlockRedstoneManagerSpigotModule.class,
    BigDoorsSpigotSubPlatformModule.class,
    SQLiteStorageModule.class,
    DebugReporterSpigotModule.class,
    VaultManagerModule.class,
    BigDoorsToolUtilSpigotModule.class,
    TextFactorySpigotModule.class,
    ChunkLoaderSpigotModule.class,
})
interface BigDoorsSpigotComponent
{
    @Component.Builder
    interface Builder
    {
        @BindsInstance
        Builder setPlugin(BigDoorsPlugin javaPlugin);

        @BindsInstance
        Builder setRestartableHolder(RestartableHolder restartableHolder);

        BigDoorsSpigotComponent build();
    }

    JavaPlugin getBigDoorsJavaPlugin();

    CommandManager getCommandListener();

    RestartableHolder getRestartableHolder();

    SubPlatformManager getSubPlatformManager();

    IDoorEventCaller getDoorEventCaller();

    @Named("mainThreadId")
    long getMainThreadId();

    DebugReporter getDebugReporter();

    DebuggableRegistry getDebuggableRegistry();

    IBigDoorsSpigotSubPlatform getSpigotSubPlatform();

    ProtectionCompatManagerSpigot getProtectionCompatManager();

    ConfigLoaderSpigot getConfig();

    RedstoneListener getRedstoneListener();

    LoginResourcePackListener getLoginResourcePackListener();

    IPExecutor getPExecutor();

    PowerBlockManager getPowerBlockManager();

    WorldListener getWorldListener();

    ChunkListener getChunkListener();

    EventListeners getEventListeners();

    LoginMessageListener getLoginMessageListener();

    VaultManager getVaultManager();

    GlowingBlockSpawner getIGlowingBlockSpawner();

    LimitsManager getLimitsManager();

    HeadManager getHeadManager();

    UpdateManager getUpdateManager();

    IPServer getIPServer();

    IPLocationFactory getIPLocationFactory();

    IPWorldFactory getIPWorldFactory();

    IPPlayerFactory getIPPlayerFactory();

    IAudioPlayer getIAudioPlayer();

    AudioAnimationHook.Factory getAudioAnimationHookFactory();

    IMessagingInterface getIMessagingInterface();

    IChunkLoader getChunkLoader();

    @Named("MessageableServer")
    IMessageable getMessageable();

    IBigDoorsEventFactory getIBigDoorsEventFactory();

    IPowerBlockRedstoneManager getIPowerBlockRedstoneManager();

    BigDoorsToolUtilSpigot getBigDoorsToolUtilSpigot();

    TextFactorySpigot getTextFactorySpigot();

    DatabaseManager getDatabaseManager();

    DoorRegistry getDoorRegistry();

    AutoCloseScheduler getAutoCloseScheduler();

    DoorActivityManager getDoorActivityManager();

    DoorSpecificationManager getDoorSpecificationManager();

    DoorTypeManager getDoorTypeManager();

    ToolUserManager getToolUserManager();

    DelayedCommandInputManager getDelayedCommandInputManager();

    ILocalizer getILocalizer();

    LocalizationManager getLocalizationManager();

    IAnimatedBlockFactory getAnimatedBlockFactory();

    IBlockAnalyzer getBlockAnalyzer();

    DoorTypeLoader getDoorTypeLoader();

    CommandFactory getCommandFactory();

    AnimatedBlockHookManager getAnimatedBlockHookManager();

    AnimationHookManager getAnimationHookManager();
}
