package nl.pim16aap2.bigdoors.api;

import nl.pim16aap2.bigdoors.api.factories.IBigDoorsEventFactory;
import nl.pim16aap2.bigdoors.api.factories.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IPBlockDataFactory;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.api.factories.IPWorldFactory;
import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.commands.DelayedCommandInputRequest;
import nl.pim16aap2.bigdoors.commands.IPServer;
import nl.pim16aap2.bigdoors.doors.DoorOpener;
import nl.pim16aap2.bigdoors.events.IBigDoorsEvent;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
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

import java.io.File;

public interface IBigDoorsPlatform extends IRestartable
{
    /**
     * Gets the directory where all data will stored.
     *
     * @return The directory where all data will stored.
     */
    File getDataDirectory();

    /**
     * Gets the instance of the {@link IPLocationFactory} for this platform.
     *
     * @return The instance of the {@link IPLocationFactory} for this platform.
     */
    IPLocationFactory getPLocationFactory();

    /**
     * Gets the instance of the {@link IBigDoorsToolUtil} for this platform.
     *
     * @return The instance of the {@link IBigDoorsToolUtil} for this platform.
     */
    IBigDoorsToolUtil getBigDoorsToolUtil();

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
     * Gets the instance of the {@link IPWorldFactory} for this platform.
     *
     * @return The instance of the {@link IPWorldFactory} for this platform.
     */
    IPWorldFactory getPWorldFactory();

    /**
     * Gets the instance of the {@link IPBlockDataFactory} for this platform.
     *
     * @return The instance of the {@link IPBlockDataFactory} for this platform.
     */
    IPBlockDataFactory getPBlockDataFactory();

    /**
     * Gets the instance of the {@link IFallingBlockFactory} for this platform.
     *
     * @return The instance of the {@link IFallingBlockFactory} for this platform.
     */
    IFallingBlockFactory getFallingBlockFactory();

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
    IConfigLoader getConfigLoader();

    /**
     * Gets the instance of the {@link ISoundEngine} for this platform.
     *
     * @return The instance of the {@link ISoundEngine} for this platform.
     */
    ISoundEngine getSoundEngine();

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
     * Gets the instance of the {@link IBlockAnalyzer} for this platform.
     *
     * @return The instance of the {@link IBlockAnalyzer} for this platform.
     */
    IBlockAnalyzer getBlockAnalyzer();

    /**
     * Gets the instance of the {@link IPowerBlockRedstoneManager} for this platform.
     *
     * @return The instance of the {@link IPowerBlockRedstoneManager} for this platform.
     */
    IPowerBlockRedstoneManager getPowerBlockRedstoneManager();

    /**
     * Gets the instance of the {@link IChunkManager} for this platform.
     *
     * @return The instance of the {@link IChunkManager} for this platform.
     */
    IChunkManager getChunkManager();

    /**
     * Gets the instance of the {@link IBigDoorsEventFactory} for this platform.
     *
     * @return The instance of the {@link IBigDoorsEventFactory} for this platform.
     */
    IBigDoorsEventFactory getBigDoorsEventFactory();

    /**
     * Calls a {@link IBigDoorsEvent}.
     *
     * @param doorActionEvent
     *     The {@link IBigDoorsEvent} to call.
     */
    void callDoorEvent(IBigDoorsEvent doorActionEvent);

    /**
     * Checks if a thread is the main thread.
     *
     * @param threadID
     *     The ID of the thread to compare.
     * @return True if the thread is the main thread.
     */
    boolean isMainThread(long threadID);

    /**
     * Constructs a new {@link IPExecutor}.
     *
     * @return A new {@link IPExecutor}.
     */
    IPExecutor getPExecutor();

    /**
     * Gets the {@link IGlowingBlockSpawner} for the current platform.
     *
     * @return The {@link IGlowingBlockSpawner} for the current platform.
     */
    IGlowingBlockSpawner getGlowingBlockSpawner();

    /**
     * Gets the {@link PowerBlockManager} instance.
     *
     * @return The {@link PowerBlockManager} instance.
     */
    PowerBlockManager getPowerBlockManager();

    /**
     * Gets the {@link IPLogger} for this platform.
     *
     * @return The {@link IPLogger} for this platform.
     */
    IPLogger getPLogger();

    /**
     * Gets the {@link DatabaseManager}.
     *
     * @return The {@link DatabaseManager}.
     */
    DatabaseManager getDatabaseManager();

    /**
     * Gets the {@link DoorRegistry}.
     *
     * @return The {@link DoorRegistry}.
     */
    DoorRegistry getDoorRegistry();

    /**
     * Gets the {@link AutoCloseScheduler} instance.
     *
     * @return The {@link AutoCloseScheduler} instance.
     */
    AutoCloseScheduler getAutoCloseScheduler();

    /**
     * Gets the {@link DoorActivityManager} instance.
     *
     * @return The {@link DoorActivityManager} instance.
     */
    DoorActivityManager getDoorActivityManager();

    /**
     * Gets the {@link DoorSpecificationManager} instance.
     *
     * @return The {@link DoorSpecificationManager} instance.
     */
    DoorSpecificationManager getDoorSpecificationManager();

    /**
     * Gets the {@link DoorTypeManager} instance.
     *
     * @return The {@link DoorTypeManager} instance.
     */
    DoorTypeManager getDoorTypeManager();

    /**
     * Gets the {@link ToolUserManager} instance.
     *
     * @return The {@link ToolUserManager} instance.
     */
    ToolUserManager getToolUserManager();

    /**
     * Gets the {@link DoorOpener}.
     *
     * @return The {@link DoorOpener}.
     */
    DoorOpener getDoorOpener();

    /**
     * Gets the {@link DebugReporter}.
     *
     * @return The {@link DebugReporter}.
     */
    DebugReporter getDebugReporter();

    /**
     * Gets the build id of BigDoors that is currently running.
     *
     * @return The build id of BigDoors that is currently running.
     */
    String getVersion();

    /**
     * Gets the {@link DelayedCommandInputManager} to manage {@link DelayedCommandInputRequest}s.
     *
     * @return The {@link DelayedCommandInputManager} registered by the platform.
     */
    DelayedCommandInputManager getDelayedCommandInputManager();

    /**
     * Gets the {@link IPServer} instance.
     *
     * @return The {@link IPServer} instance.
     */
    IPServer getPServer();

    /**
     * Gets the {@link LimitsManager}.
     *
     * @return The {@link LimitsManager}.
     */
    LimitsManager getLimitsManager();

    ILocalizer getLocalizer();
}
