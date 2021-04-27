package nl.pim16aap2.bigdoors.api;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.factories.IBigDoorsEventFactory;
import nl.pim16aap2.bigdoors.api.factories.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IPBlockDataFactory;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.api.factories.IPWorldFactory;
import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.api.restartable.IRestartableHolder;
import nl.pim16aap2.bigdoors.commands.DelayedCommandInputRequest;
import nl.pim16aap2.bigdoors.commands.IPServer;
import nl.pim16aap2.bigdoors.doors.DoorOpener;
import nl.pim16aap2.bigdoors.events.IBigDoorsEvent;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.managers.AutoCloseScheduler;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DelayedCommandInputManager;
import nl.pim16aap2.bigdoors.managers.DoorActivityManager;
import nl.pim16aap2.bigdoors.managers.DoorRegistry;
import nl.pim16aap2.bigdoors.managers.DoorSpecificationManager;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import nl.pim16aap2.bigdoors.managers.LimitsManager;
import nl.pim16aap2.bigdoors.managers.PowerBlockManager;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.util.messages.Messages;

import java.io.File;
import java.util.Optional;

public interface IBigDoorsPlatform extends IRestartableHolder, IRestartable
{
    /**
     * Gets the directory where all data will stored.
     *
     * @return The directory where all data will stored.
     */
    @NonNull File getDataDirectory();

    /**
     * Gets the instance of the {@link IPLocationFactory} for this platform.
     *
     * @return The instance of the {@link IPLocationFactory} for this platform.
     */
    @NonNull IPLocationFactory getPLocationFactory();

    /**
     * Gets the instance of the {@link IBigDoorsToolUtil} for this platform.
     *
     * @return The instance of the {@link IBigDoorsToolUtil} for this platform.
     */
    @NonNull IBigDoorsToolUtil getBigDoorsToolUtil();

    /**
     * Gets the instance of the {@link IEconomyManager} for this platform.
     *
     * @return The instance of the {@link IEconomyManager} for this platform.
     */
    @NonNull IEconomyManager getEconomyManager();

    /**
     * Gets the instance of the {@link IPermissionsManager} for this platform.
     *
     * @return The instance of the {@link IPermissionsManager} for this platform.
     */
    @NonNull IPermissionsManager getPermissionsManager();

    /**
     * Gets the instance of the {@link IProtectionCompatManager} for this platform.
     *
     * @return The instance of the {@link IProtectionCompatManager} for this platform.
     */
    @NonNull IProtectionCompatManager getProtectionCompatManager();

    /**
     * Gets the instance of the {@link IPWorldFactory} for this platform.
     *
     * @return The instance of the {@link IPWorldFactory} for this platform.
     */
    @NonNull IPWorldFactory getPWorldFactory();

    /**
     * Gets the instance of the {@link IPBlockDataFactory} for this platform.
     *
     * @return The instance of the {@link IPBlockDataFactory} for this platform.
     */
    @NonNull IPBlockDataFactory getPBlockDataFactory();

    /**
     * Gets the instance of the {@link IFallingBlockFactory} for this platform.
     *
     * @return The instance of the {@link IFallingBlockFactory} for this platform.
     */
    @NonNull IFallingBlockFactory getFallingBlockFactory();

    /**
     * Gets the instance of the {@link IPPlayerFactory} for this platform.
     *
     * @return The instance of the {@link IPPlayerFactory} for this platform.
     */
    @NonNull IPPlayerFactory getPPlayerFactory();

    /**
     * Gets the instance of the {@link IConfigLoader} for this platform.
     *
     * @return The instance of the {@link IConfigLoader} for this platform.
     */
    @NonNull IConfigLoader getConfigLoader();

    /**
     * Gets the instance of the {@link ISoundEngine} for this platform.
     *
     * @return The instance of the {@link ISoundEngine} for this platform.
     */
    @NonNull ISoundEngine getSoundEngine();

    /**
     * Gets the instance of the {@link IMessagingInterface} for this platform.
     *
     * @return The instance of the {@link IMessagingInterface} for this platform.
     */
    @NonNull IMessagingInterface getMessagingInterface();

    /**
     * Gets the instance of the {@link Messages} for this platform.
     *
     * @return The instance of the {@link Messages} for this platform.
     */
    @NonNull Messages getMessages();

    /**
     * Gets the implementation of a {@link IMessageable} for the server.
     *
     * @return The implementation of a {@link IMessageable} for the server.
     */
    @NonNull IMessageable getMessageableServer();

    /**
     * Gets the instance of the {@link IBlockAnalyzer} for this platform.
     *
     * @return The instance of the {@link IBlockAnalyzer} for this platform.
     */
    @NonNull IBlockAnalyzer getBlockAnalyzer();

    /**
     * Gets the instance of the {@link IPowerBlockRedstoneManager} for this platform.
     *
     * @return The instance of the {@link IPowerBlockRedstoneManager} for this platform.
     */
    @NonNull IPowerBlockRedstoneManager getPowerBlockRedstoneManager();

    /**
     * Gets the instance of the {@link IChunkManager} for this platform.
     *
     * @return The instance of the {@link IChunkManager} for this platform.
     */
    @NonNull IChunkManager getChunkManager();

    /**
     * Gets the instance of the {@link IBigDoorsEventFactory} for this platform.
     *
     * @return The instance of the {@link IBigDoorsEventFactory} for this platform.
     */
    @NonNull IBigDoorsEventFactory getBigDoorsEventFactory();

    /**
     * Calls a {@link IBigDoorsEvent}.
     *
     * @param doorActionEvent The {@link IBigDoorsEvent} to call.
     */
    void callDoorEvent(@NonNull IBigDoorsEvent doorActionEvent);

    /**
     * Checks if a thread is the main thread.
     *
     * @param threadID The ID of the thread to compare.
     * @return True if the thread is the main thread.
     */
    boolean isMainThread(long threadID);

    /**
     * Constructs a new {@link IPExecutor}.
     *
     * @return A new {@link IPExecutor}.
     */
    @NonNull IPExecutor getPExecutor();

    /**
     * Gets the {@link IGlowingBlockSpawner} for the current platform.
     *
     * @return The {@link IGlowingBlockSpawner} for the current platform.
     */
    @NonNull Optional<IGlowingBlockSpawner> getGlowingBlockSpawner();

    /**
     * Gets the {@link PowerBlockManager} instance.
     *
     * @return The {@link PowerBlockManager} instance.
     */
    @NonNull PowerBlockManager getPowerBlockManager();

    /**
     * Gets the {@link IPLogger} for this platform.
     *
     * @return The {@link IPLogger} for this platform.
     */
    @NonNull IPLogger getPLogger();

    /**
     * Gets the {@link DatabaseManager}.
     *
     * @return The {@link DatabaseManager}.
     */
    @NonNull DatabaseManager getDatabaseManager();

    /**
     * Gets the {@link DoorRegistry}.
     *
     * @return The {@link DoorRegistry}.
     */
    @NonNull DoorRegistry getDoorRegistry();

    /**
     * Gets the {@link AutoCloseScheduler} instance.
     *
     * @return The {@link AutoCloseScheduler} instance.
     */
    @NonNull AutoCloseScheduler getAutoCloseScheduler();

    /**
     * Gets the {@link DoorActivityManager} instance.
     *
     * @return The {@link DoorActivityManager} instance.
     */
    @NonNull DoorActivityManager getDoorActivityManager();

    /**
     * Gets the {@link DoorSpecificationManager} instance.
     *
     * @return The {@link DoorSpecificationManager} instance.
     */
    @NonNull DoorSpecificationManager getDoorSpecificationManager();

    /**
     * Gets the {@link DoorTypeManager} instance.
     *
     * @return The {@link DoorTypeManager} instance.
     */
    @NonNull DoorTypeManager getDoorTypeManager();

    /**
     * Gets the {@link ToolUserManager} instance.
     *
     * @return The {@link ToolUserManager} instance.
     */
    @NonNull ToolUserManager getToolUserManager();

    /**
     * Gets the {@link DoorOpener}.
     *
     * @return The {@link DoorOpener}.
     */
    @NonNull DoorOpener getDoorOpener();

    /**
     * Gets the {@link DebugReporter}.
     *
     * @return The {@link DebugReporter}.
     */
    @NonNull DebugReporter getDebugReporter();

    /**
     * Gets the build id of BigDoors that is currently running.
     *
     * @return The build id of BigDoors that is currently running.
     */
    @NonNull String getVersion();

    /**
     * Gets the {@link DelayedCommandInputManager} to manage {@link DelayedCommandInputRequest}s.
     *
     * @return The {@link DelayedCommandInputManager} registered by the platform.
     */
    @NonNull DelayedCommandInputManager getDelayedCommandInputManager();

    /**
     * Gets the {@link IPServer} instance.
     *
     * @return The {@link IPServer} instance.
     */
    @NonNull IPServer getPServer();

    /**
     * Gets the {@link LimitsManager}.
     *
     * @return The {@link LimitsManager}.
     */
    @NonNull LimitsManager getLimitsManager();
}
