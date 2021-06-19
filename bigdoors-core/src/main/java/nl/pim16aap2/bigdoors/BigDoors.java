package nl.pim16aap2.bigdoors;

import nl.pim16aap2.bigdoors.api.DebugReporter;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IMessagingInterface;
import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.commands.DelayedCommandInputRequest;
import nl.pim16aap2.bigdoors.commands.IPServer;
import nl.pim16aap2.bigdoors.doors.DoorOpener;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the core class of BigDoors.
 *
 * @author Pim
 */
public final class BigDoors extends RestartableHolder
{
    private static final @NotNull BigDoors INSTANCE = new BigDoors();

    /**
     * Gets the {@link DelayedCommandInputManager} to manage {@link DelayedCommandInputRequest}s.
     *
     * @return The {@link DelayedCommandInputManager} registered by the platform.
     */
    public @NotNull DelayedCommandInputManager getDelayedCommandInputManager()
    {
        return getPlatform().getDelayedCommandInputManager();
    }

    /**
     * The platform to use. e.g. "Spigot".
     */
    private @Nullable IBigDoorsPlatform platform;

    private BigDoors()
    {
    }

    /**
     * Gets the instance of this class.
     *
     * @return The instance of this class.
     */
    public static @NotNull BigDoors get()
    {
        return INSTANCE;
    }

    /**
     * Sets the platform implementing BigDoor's internal API.
     *
     * @param platform The platform implementing BigDoor's internal API.
     */
    public void setBigDoorsPlatform(final @NotNull IBigDoorsPlatform platform)
    {
        this.platform = platform;
    }

    /**
     * gets the platform implementing BigDoor's internal API.
     *
     * @return The platform implementing BigDoor's internal API.
     */
    public @NotNull IBigDoorsPlatform getPlatform()
    {
        if (platform == null)
            throw new IllegalStateException("No platform currently registered!");
        return platform;
    }

    /**
     * Gets the {@link DoorRegistry}.
     *
     * @return The {@link DoorRegistry}.
     */
    public @NotNull DoorRegistry getDoorRegistry()
    {
        return getPlatform().getDoorRegistry();
    }

    /**
     * Gets the {@link DoorOpener}.
     *
     * @return The {@link DoorOpener}.
     */
    public @NotNull DoorOpener getDoorOpener()
    {
        return getPlatform().getDoorOpener();
    }

    /**
     * Gets the {@link LimitsManager}.
     *
     * @return The {@link LimitsManager}.
     */
    public @NotNull LimitsManager getLimitsManager()
    {
        return getPlatform().getLimitsManager();
    }

    /**
     * Gets the {@link PowerBlockManager}.
     *
     * @return The {@link PowerBlockManager}.
     */
    public @NotNull PowerBlockManager getPowerBlockManager()
    {
        return getPlatform().getPowerBlockManager();
    }

    /**
     * Gets the {@link DoorActivityManager} instance.
     *
     * @return The {@link DoorActivityManager} instance.
     */
    public @NotNull DoorActivityManager getDoorActivityManager()
    {
        return getPlatform().getDoorActivityManager();
    }

    /**
     * Gets the {@link AutoCloseScheduler} instance.
     *
     * @return The {@link AutoCloseScheduler} instance.
     */
    public @NotNull AutoCloseScheduler getAutoCloseScheduler()
    {
        return getPlatform().getAutoCloseScheduler();
    }

    /**
     * Gets the {@link DoorSpecificationManager} instance.
     *
     * @return The {@link DoorSpecificationManager} instance.
     */
    public @NotNull DoorSpecificationManager getDoorSpecificationManager()
    {
        return getPlatform().getDoorSpecificationManager();
    }

    /**
     * Gets the {@link DoorTypeManager} instance.
     *
     * @return The {@link DoorTypeManager} instance.
     */
    public @NotNull DoorTypeManager getDoorTypeManager()
    {
        return getPlatform().getDoorTypeManager();
    }

    /**
     * Gets the {@link IPServer} instance.
     *
     * @return The {@link IPServer} instance.
     */
    public @NotNull IPServer getPServer()
    {
        return getPlatform().getPServer();
    }

    /**
     * Gets the {@link ToolUserManager} instance.
     *
     * @return The {@link ToolUserManager} instance.
     */
    public @NotNull ToolUserManager getToolUserManager()
    {
        return getPlatform().getToolUserManager();
    }

    /**
     * Gets the currently used {@link IMessagingInterface}. If the current one isn't set, {@link
     * IBigDoorsPlatform#getMessagingInterface} is used instead.
     *
     * @return The currently used {@link IMessagingInterface}.
     */
    public @NotNull IMessagingInterface getMessagingInterface()
    {
        return getPlatform().getMessagingInterface();
    }

    /**
     * Gets the currently set {@link IPLogger}.
     *
     * @return The currently set {@link IPLogger}..
     */
    public @NotNull IPLogger getPLogger()
    {
        return getPlatform().getPLogger();
    }

    /**
     * Gets the {@link DatabaseManager}.
     *
     * @return The {@link DatabaseManager}.
     */
    public @NotNull DatabaseManager getDatabaseManager()
    {
        return getPlatform().getDatabaseManager();
    }

    /**
     * Gets the {@link DebugReporter}.
     *
     * @return The {@link DebugReporter}.
     */
    public @NotNull DebugReporter getDebugReporter()
    {
        return getPlatform().getDebugReporter();
    }

    /**
     * Gets the version of BigDoors that is currently running.
     *
     * @return The version of BigDoors that is currently running.
     */
    public @NotNull String getVersion()
    {
        return getPlatform().getVersion();
    }

    /**
     * Handles a restart.
     */
    public void restart()
    {
        restartables.forEach(IRestartable::restart);
    }

    /**
     * Handles a shutdown.
     */
    public void shutdown()
    {
        restartables.forEach(IRestartable::shutdown);
    }
}
