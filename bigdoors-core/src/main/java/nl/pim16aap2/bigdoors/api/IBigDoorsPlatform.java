package nl.pim16aap2.bigdoors.api;

import nl.pim16aap2.bigdoors.api.factories.IAnimatedBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.api.factories.IPWorldFactory;
import nl.pim16aap2.bigdoors.audio.IAudioPlayer;
import nl.pim16aap2.bigdoors.commands.DelayedCommandInputRequest;
import nl.pim16aap2.bigdoors.commands.IPServer;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DelayedCommandInputManager;
import nl.pim16aap2.bigdoors.managers.LimitsManager;
import nl.pim16aap2.bigdoors.managers.MovableRegistry;
import nl.pim16aap2.bigdoors.managers.MovableSpecificationManager;
import nl.pim16aap2.bigdoors.managers.MovableTypeManager;
import nl.pim16aap2.bigdoors.managers.PowerBlockManager;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.moveblocks.AutoCloseScheduler;
import nl.pim16aap2.bigdoors.moveblocks.MovableActivityManager;

/**
 * Represents a set of getter methods to get access to the internals of BigDoors.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
public interface IBigDoorsPlatform
{
    /**
     * Restarts the plugin.
     */
    void restartPlugin();

    /**
     * Shuts the plugin down.
     */
    void shutDownPlugin();

    /**
     * Gets the build id of BigDoors that is currently running.
     *
     * @return The build id of BigDoors that is currently running.
     */
    String getVersion();

    /**
     * Gets the instance of the {@link IBigDoorsToolUtil} for this platform.
     *
     * @return The instance of the {@link IBigDoorsToolUtil} for this platform.
     */
    IBigDoorsToolUtil getBigDoorsToolUtil();

    /**
     * Gets the instance of the {@link IPWorldFactory} for this platform.
     *
     * @return The instance of the {@link IPWorldFactory} for this platform.
     */
    IPWorldFactory getPWorldFactory();

    /**
     * Gets the instance of the {@link IPLocationFactory} for this platform.
     *
     * @return The instance of the {@link IPLocationFactory} for this platform.
     */
    IPLocationFactory getPLocationFactory();

    /**
     * Gets the instance of the {@link IAnimatedBlockFactory} for this platform.
     *
     * @return The instance of the {@link IAnimatedBlockFactory} for this platform.
     */
    IAnimatedBlockFactory getAnimatedBlockFactory();

    /**
     * Gets the instance of the {@link IPPlayerFactory} for this platform.
     *
     * @return The instance of the {@link IPPlayerFactory} for this platform.
     */
    IPPlayerFactory getPPlayerFactory();

    /**
     * Gets the instance of the {@link IConfigLoader} for this platform.
     *
     * @return The instance of the {@link IConfigLoader} for this platform.
     */
    IConfigLoader getBigDoorsConfig();

    /**
     * Gets the instance of the {@link IAudioPlayer} for this platform.
     *
     * @return The instance of the {@link IAudioPlayer} for this platform.
     */
    IAudioPlayer getAudioPlayer();

    /**
     * Gets the instance of the {@link IBlockAnalyzer} for this platform.
     *
     * @return The instance of the {@link IBlockAnalyzer} for this platform.
     */
    IBlockAnalyzer getBlockAnalyzer();

    /**
     * Constructs a new {@link IPExecutor}.
     *
     * @return A new {@link IPExecutor}.
     */
    IPExecutor getPExecutor();

    /**
     * Gets the {@link GlowingBlockSpawner} for the current platform.
     *
     * @return The {@link GlowingBlockSpawner} for the current platform.
     */
    GlowingBlockSpawner getGlowingBlockSpawner();

    /**
     * Gets the {@link ILocalizer} used to localize strings.
     *
     * @return The {@link ILocalizer} registered for this platform.
     */
    ILocalizer getLocalizer();

    /**
     * Gets the instance of the {@link IMessagingInterface} for this platform.
     *
     * @return The instance of the {@link IMessagingInterface} for this platform.
     */
    IMessagingInterface getMessagingInterface();

    /**
     * Gets the implementation of a {@link IMessageable} for the server.
     *
     * @return The implementation of a {@link IMessageable} for the server.
     */
    IMessageable getMessageableServer();

    /**
     * Gets the {@link IPServer} instance.
     *
     * @return The {@link IPServer} instance.
     */
    IPServer getPServer();

    /**
     * Gets the {@link MovableRegistry}.
     *
     * @return The {@link MovableRegistry}.
     */
    MovableRegistry getDoorRegistry();

    /**
     * Gets the {@link AutoCloseScheduler} instance.
     *
     * @return The {@link AutoCloseScheduler} instance.
     */
    AutoCloseScheduler getAutoCloseScheduler();

    /**
     * @return The instance of the {@link IConfigLoader} for this platform.
     */
    IChunkLoader getChunkLoader();

    /**
     * Gets the {@link DatabaseManager}.
     *
     * @return The {@link DatabaseManager}.
     */
    DatabaseManager getDatabaseManager();

    /**
     * Gets the {@link MovableActivityManager} instance.
     *
     * @return The {@link MovableActivityManager} instance.
     */
    MovableActivityManager getDoorActivityManager();

    /**
     * Gets the {@link MovableSpecificationManager} instance.
     *
     * @return The {@link MovableSpecificationManager} instance.
     */
    MovableSpecificationManager getDoorSpecificationManager();

    /**
     * Gets the {@link MovableTypeManager} instance.
     *
     * @return The {@link MovableTypeManager} instance.
     */
    MovableTypeManager getDoorTypeManager();

    /**
     * Gets the {@link ToolUserManager} instance.
     *
     * @return The {@link ToolUserManager} instance.
     */
    ToolUserManager getToolUserManager();

    /**
     * Gets the {@link DelayedCommandInputManager} to manage {@link DelayedCommandInputRequest}s.
     *
     * @return The {@link DelayedCommandInputManager} registered by the platform.
     */
    DelayedCommandInputManager getDelayedCommandInputManager();

    /**
     * Gets the {@link PowerBlockManager} instance.
     *
     * @return The {@link PowerBlockManager} instance.
     */
    PowerBlockManager getPowerBlockManager();

    /**
     * Gets the instance of the {@link IEconomyManager} for this platform.
     *
     * @return The instance of the {@link IEconomyManager} for this platform.
     */
    IEconomyManager getEconomyManager();

    /**
     * Gets the instance of the {@link IPermissionsManager} for this platform.
     *
     * @return The instance of the {@link IPermissionsManager} for this platform.
     */
    IPermissionsManager getPermissionsManager();

    /**
     * Gets the instance of the {@link IProtectionCompatManager} for this platform.
     *
     * @return The instance of the {@link IProtectionCompatManager} for this platform.
     */
    IProtectionCompatManager getProtectionCompatManager();

    /**
     * Gets the {@link LimitsManager}.
     *
     * @return The {@link LimitsManager}.
     */
    LimitsManager getLimitsManager();
}
