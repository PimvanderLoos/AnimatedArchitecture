package nl.pim16aap2.bigdoors;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IMessagingInterface;
import nl.pim16aap2.bigdoors.api.IRestartable;
import nl.pim16aap2.bigdoors.api.IRestartableHolder;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.managers.AutoCloseScheduler;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DoorActivityManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents the core class of BigDoors.
 *
 * @author Pim
 */
public final class BigDoors implements IRestartableHolder, IRestartable
{
    @NotNull
    private static final BigDoors INSTANCE = new BigDoors();

    @Nullable
    private IMessagingInterface messagingInterface = null;

    @NotNull
    private final Set<IRestartable> restartables = new HashSet<>();

    private IPLogger backupLogger;

    /**
     * The platform to use. e.g. "Spigot".
     */
    private IBigDoorsPlatform platform;

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
        if (this.platform != null)
            this.platform.deregisterRestartable(this);
        this.platform = platform;
        this.platform.registerRestartable(this);
    }

    /**
     * gets the platform implementing BigDoor's internal API.
     *
     * @return The platform implementing BigDoor's internal API.
     */
    public @NotNull IBigDoorsPlatform getPlatform()
    {
        return platform;
    }

    /**
     * Gets the {@link DoorActivityManager} instance.
     *
     * @return The {@link DoorActivityManager} instance.
     */
    public @NotNull DoorActivityManager getDoorManager()
    {
        return DoorActivityManager.get();
    }

    /**
     * Gets the {@link AutoCloseScheduler} instance.
     *
     * @return The {@link AutoCloseScheduler} instance.
     */
    public @NotNull AutoCloseScheduler getAutoCloseScheduler()
    {
        return AutoCloseScheduler.get();
    }

    /**
     * Gets the currently used {@link IMessagingInterface}. If the current one isn't set, {@link
     * IBigDoorsPlatform#getMessagingInterface} is used instead.
     *
     * @return The currently used {@link IMessagingInterface}.
     */
    public @NotNull IMessagingInterface getMessagingInterface()
    {
        if (messagingInterface == null)
            return getPlatform().getMessagingInterface();
        return messagingInterface;
    }

    /**
     * Gets the currently set {@link IPLogger}.
     *
     * @return The currently set {@link IPLogger}..
     */
    public @NonNull IPLogger getPLogger()
    {
        if (platform == null || getPlatform().getPLogger() == null)
            return backupLogger == null ? backupLogger = new BasicPLogger() : backupLogger;
        return getPlatform().getPLogger();
    }

    /**
     * Gets the {@link DatabaseManager}.
     *
     * @return The {@link DatabaseManager}.
     */
    public @NonNull DatabaseManager getDatabaseManager()
    {
        return getPlatform().getDatabaseManager();
    }

    public void setMessagingInterface(final @Nullable IMessagingInterface messagingInterface)
    {
        this.messagingInterface = messagingInterface;
    }

    @Override
    public void restart()
    {
        restartables.forEach(IRestartable::restart);
    }

    @Override
    public void shutdown()
    {
        restartables.forEach(IRestartable::shutdown);
    }

    @Override
    public void registerRestartable(final @NotNull IRestartable restartable)
    {
        restartables.add(restartable);
    }

    @Override
    public boolean isRestartableRegistered(final @NotNull IRestartable restartable)
    {
        return restartables.contains(restartable);
    }

    @Override
    public void deregisterRestartable(final @NotNull IRestartable restartable)
    {
        restartables.remove(restartable);
    }
}
