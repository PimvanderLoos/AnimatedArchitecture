package nl.pim16aap2.bigdoors.managers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.DoorOpener;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.ITimerToggleableArchetype;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.util.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

/**
 * Represents a scheduler that automatically closes doors after a certain amount of time.
 *
 * @author Pim
 */
public final class AutoCloseScheduler extends Restartable
{
    private static final @NotNull AutoCloseScheduler INSTANCE = new AutoCloseScheduler();

    /**
     * A map of {@link TimerTask}s.
     * <p>
     * <b>Key:</b> doorUID
     * <p>
     * <b>Value:</b> A {@link TimerTask} to toggle this door again after a certain amount of time.
     */
    private final @NotNull Map<Long, TimerTask> timers = new HashMap<>();

    private AutoCloseScheduler()
    {
        super(BigDoors.get());
    }

    /**
     * Gets the instance of the {@link AutoCloseScheduler} if it exists.
     *
     * @return The instance of the {@link AutoCloseScheduler}.
     */
    public static @NotNull AutoCloseScheduler get()
    {
//        Preconditions.checkState(instance != null,
//                                 "Instance has not yet been initialized. Be sure #init() has been invoked");
        return INSTANCE;
    }

    /**
     * Cancels and deleted the {@link TimerTask} of a door if it exists.
     *
     * @param doorUID The UID of the door.
     */
    private synchronized void deleteTimer(long doorUID)
    {
        final @Nullable TimerTask task = timers.remove(doorUID);
        if (task != null)
            task.cancel();
    }

    /**
     * Unschedules automatically closing a door.
     *
     * @param doorUID The UID of the door.
     */
    public synchronized void unscheduleAutoClose(long doorUID)
    {
        deleteTimer(doorUID);
    }

    /**
     * Schedule closing a door.
     *
     * @param cause         What caused the door to be opened.
     * @param player        The player who requested the door toggle. May be null.
     * @param door          The door to close.
     * @param speed         The speed at which the door should move.
     * @param skipAnimation Whether the door should be animated or not.
     */
    public synchronized <T extends AbstractDoorBase & ITimerToggleableArchetype> void scheduleAutoClose(
        final @NotNull DoorActionCause cause, final @NotNull IPPlayer player,
        final T door, double speed, boolean skipAnimation)
    {
        int autoCloseTimer = door.getAutoCloseTime();
        if (autoCloseTimer < 0 || !door.isOpen())
            return;

        // First delete any old timers that might still be running.
        deleteTimer(door.getDoorUID());
        // Add 2 ticks to the minimum delay to make sure there's no overlap with setting the door available again.
        final int delay = Math.min(Constants.MINIMUMDOORDELAY + 2, autoCloseTimer * 20);

        final @NotNull TimerTask task = new TimerTask()
        {
            @Override
            public void run()
            {
                if (door.isOpen())
                {
                    // TODO: Verify that reusing the door object won't result in any issues.
                    BigDoors.get().getDoorManager().setDoorAvailable(door.getDoorUID());
                    DoorOpener.get().animateDoorAsync(door, cause, player, speed, skipAnimation, DoorActionType.CLOSE);
                }
                deleteTimer(door.getDoorUID());
            }
        };
        timers.put(door.getDoorUID(), task);
        BigDoors.get().getPlatform().newPExecutor().runSyncLater(task, delay);
    }

    /**
     * Schedule closing a door.
     *
     * @param player        The player who requested the door toggle. May be null.
     * @param door          The door to close.
     * @param speed         The speed at which the door should move.
     * @param skipAnimation Whether the door should be animated or not.
     */
    public synchronized <T extends AbstractDoorBase & ITimerToggleableArchetype> void scheduleAutoClose(
        final @NotNull IPPlayer player, final @NotNull T door,
        double speed, boolean skipAnimation)
    {
        scheduleAutoClose(DoorActionCause.REDSTONE, player, door, speed, skipAnimation);
    }

    @Override
    public synchronized void restart()
    {
        shutdown();
    }

    @Override
    public synchronized void shutdown()
    {
        timers.forEach((K, V) -> V.cancel());
        timers.clear();
    }
}
