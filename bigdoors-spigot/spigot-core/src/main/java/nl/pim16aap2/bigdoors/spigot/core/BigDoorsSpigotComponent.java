package nl.pim16aap2.bigdoors.spigot.core;

import dagger.BindsInstance;
import dagger.Component;
import nl.pim16aap2.bigdoors.core.api.GlowingBlockSpawner;
import nl.pim16aap2.bigdoors.core.api.IBlockAnalyzer;
import nl.pim16aap2.bigdoors.core.api.IChunkLoader;
import nl.pim16aap2.bigdoors.core.api.IMessageable;
import nl.pim16aap2.bigdoors.core.api.IMessagingInterface;
import nl.pim16aap2.bigdoors.core.api.IPExecutor;
import nl.pim16aap2.bigdoors.core.api.IRedstoneManager;
import nl.pim16aap2.bigdoors.core.api.debugging.DebugReporter;
import nl.pim16aap2.bigdoors.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.core.api.factories.IAnimatedBlockFactory;
import nl.pim16aap2.bigdoors.core.api.factories.IBigDoorsEventFactory;
import nl.pim16aap2.bigdoors.core.api.factories.IGuiFactory;
import nl.pim16aap2.bigdoors.core.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.core.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.core.api.factories.IPWorldFactory;
import nl.pim16aap2.bigdoors.core.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.core.audio.AudioAnimationHook;
import nl.pim16aap2.bigdoors.core.audio.IAudioPlayer;
import nl.pim16aap2.bigdoors.core.commands.CommandFactory;
import nl.pim16aap2.bigdoors.core.commands.IPServer;
import nl.pim16aap2.bigdoors.core.events.IBigDoorsEventCaller;
import nl.pim16aap2.bigdoors.core.extensions.StructureTypeLoader;
import nl.pim16aap2.bigdoors.core.localization.ILocalizer;
import nl.pim16aap2.bigdoors.core.localization.LocalizationManager;
import nl.pim16aap2.bigdoors.core.localization.LocalizationModule;
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
import nl.pim16aap2.bigdoors.spigot.util.api.IBigDoorsSpigotSubPlatform;
import nl.pim16aap2.bigdoors.spigot.util.implementations.audio.AudioPlayerSpigotModule;
import nl.pim16aap2.bigdoors.spigot.util.implementations.glowingblocks.GlowingBlockSpawnerModule;
import nl.pim16aap2.bigdoors.spigot.util.implementations.messageable.MessagingInterfaceSpigotModule;
import nl.pim16aap2.bigdoors.spigot.util.implementations.pexecutor.PExecutorModule;
import nl.pim16aap2.bigdoors.spigot.util.implementations.pserver.PServerModule;
import nl.pim16aap2.bigdoors.spigot.core.comands.CommandManager;
import nl.pim16aap2.bigdoors.spigot.core.compatiblity.ProtectionCompatManagerModule;
import nl.pim16aap2.bigdoors.spigot.core.compatiblity.ProtectionCompatManagerSpigot;
import nl.pim16aap2.bigdoors.spigot.core.config.ConfigLoaderSpigot;
import nl.pim16aap2.bigdoors.spigot.core.config.ConfigLoaderSpigotModule;
import nl.pim16aap2.bigdoors.spigot.core.factories.bigdoorseventfactory.BigDoorsEventsSpigotModule;
import nl.pim16aap2.bigdoors.spigot.core.factories.plocationfactory.PLocationFactorySpigotModule;
import nl.pim16aap2.bigdoors.spigot.core.factories.pplayerfactory.PPlayerFactorySpigotModule;
import nl.pim16aap2.bigdoors.spigot.core.factories.pworldfactory.PWorldFactorySpigotModule;
import nl.pim16aap2.bigdoors.spigot.core.gui.GuiFactorySpigotModule;
import nl.pim16aap2.bigdoors.spigot.core.implementations.BigDoorsToolUtilSpigot;
import nl.pim16aap2.bigdoors.spigot.core.implementations.BigDoorsToolUtilSpigotModule;
import nl.pim16aap2.bigdoors.spigot.core.implementations.ChunkLoaderSpigotModule;
import nl.pim16aap2.bigdoors.spigot.core.implementations.DebugReporterSpigotModule;
import nl.pim16aap2.bigdoors.spigot.core.implementations.TextFactorySpigot;
import nl.pim16aap2.bigdoors.spigot.core.implementations.TextFactorySpigotModule;
import nl.pim16aap2.bigdoors.spigot.core.listeners.ChunkListener;
import nl.pim16aap2.bigdoors.spigot.core.listeners.EventListeners;
import nl.pim16aap2.bigdoors.spigot.core.listeners.LoginMessageListener;
import nl.pim16aap2.bigdoors.spigot.core.listeners.LoginResourcePackListener;
import nl.pim16aap2.bigdoors.spigot.core.listeners.RedstoneListener;
import nl.pim16aap2.bigdoors.spigot.core.listeners.WorldListener;
import nl.pim16aap2.bigdoors.spigot.core.managers.HeadManager;
import nl.pim16aap2.bigdoors.spigot.core.managers.PlatformManagerSpigotModule;
import nl.pim16aap2.bigdoors.spigot.core.managers.PowerBlockRedstoneManagerSpigotModule;
import nl.pim16aap2.bigdoors.spigot.core.managers.SubPlatformManager;
import nl.pim16aap2.bigdoors.spigot.core.managers.VaultManager;
import nl.pim16aap2.bigdoors.spigot.core.managers.VaultManagerModule;
import nl.pim16aap2.bigdoors.core.storage.sqlite.SQLiteStorageModule;
import nl.pim16aap2.bigdoors.core.structures.StructureRegistry;
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
    GuiFactorySpigotModule.class,
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

    IBigDoorsEventCaller getDoorEventCaller();

    @Named("mainThreadId")
    long getMainThreadId();

    DebugReporter getDebugReporter();

    DebuggableRegistry getDebuggableRegistry();

    IBigDoorsSpigotSubPlatform getSpigotSubPlatform();

    ProtectionCompatManagerSpigot getProtectionCompatManager();

    IGuiFactory getGUIFactory();

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

    IRedstoneManager getIPowerBlockRedstoneManager();

    BigDoorsToolUtilSpigot getBigDoorsToolUtilSpigot();

    TextFactorySpigot getTextFactorySpigot();

    DatabaseManager getDatabaseManager();

    StructureRegistry getDoorRegistry();

    StructureActivityManager getDoorActivityManager();

    StructureSpecificationManager getDoorSpecificationManager();

    StructureTypeManager getDoorTypeManager();

    ToolUserManager getToolUserManager();

    DelayedCommandInputManager getDelayedCommandInputManager();

    ILocalizer getILocalizer();

    LocalizationManager getLocalizationManager();

    IAnimatedBlockFactory getAnimatedBlockFactory();

    IBlockAnalyzer getBlockAnalyzer();

    StructureTypeLoader getDoorTypeLoader();

    CommandFactory getCommandFactory();

    AnimatedBlockHookManager getAnimatedBlockHookManager();

    AnimationHookManager getAnimationHookManager();
}
