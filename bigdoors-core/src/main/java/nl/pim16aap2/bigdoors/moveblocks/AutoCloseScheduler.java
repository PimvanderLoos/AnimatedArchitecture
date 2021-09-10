package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.restartable.IRestartableHolder;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorToggleRequestFactory;
import nl.pim16aap2.bigdoors.doors.doorarchetypes.ITimerToggleable;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.util.Constants;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

/**
 * Represents a scheduler that automatically closes doors after a certain amount of time.
 *
 * @author Pim
 */
@Singleton
public final class AutoCloseScheduler extends Restartable
{
    /**
     * A map of {@link TimerTask}s.
     * <p>
     * <b>Key:</b> doorUID
     * <p>
     * <b>Value:</b> A {@link TimerTask} to toggle this door again after a certain amount of time.
     */
    private final Map<Long, TimerTask> timers = new HashMap<>();
    private final DoorActivityManager doorActivityManager;
    private final DoorToggleRequestFactory doorToggleRequestFactory;

    @Inject
    public AutoCloseScheduler(IRestartableHolder holder, DoorActivityManager doorActivityManager,
                              DoorToggleRequestFactory doorToggleRequestFactory)
    {
        super(holder);
        this.doorActivityManager = doorActivityManager;
        this.doorToggleRequestFactory = doorToggleRequestFactory;
    }

    /**
     * Cancels and deleted the {@link TimerTask} of a door if it exists.
     *
     * @param doorUID
     *     The UID of the door.
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
     * @param doorUID
     *     The UID of the door.
     */
    public synchronized void unscheduleAutoClose(long doorUID)
    {
        deleteTimer(doorUID);
    }

    /**
     * Schedules closing a door.
     *
     * @param player
     *     The player who requested the door toggle. May be null.
     * @param door
     *     The door to close.
     * @param time
     *     The duration (in seconds) of the animation.
     * @param skipAnimation
     *     Whether the door should be animated or not.
     */
    synchronized <T extends AbstractDoor & ITimerToggleable> void scheduleAutoClose(IPPlayer player, T door,
                                                                                    double time, boolean skipAnimation)
    {
        final int autoCloseTimer = door.getAutoCloseTime();
        if (autoCloseTimer < 0 || !door.isOpen())
            return;

        // First delete any old timers that might still be running.
        deleteTimer(door.getDoorUID());
        // Add 2 ticks to the minimum delay to make sure there's no overlap with setting the door available again.
        final int delay = Math.min(Constants.MINIMUM_DOOR_DELAY + 2, autoCloseTimer * 20);

        final TimerTask task = new TimerTask()
        {
            @Override
            public void run()
            {
                if (door.isOpen())
                {
                    doorActivityManager.setDoorAvailable(door.getDoorUID());
                    doorToggleRequestFactory.builder()
                                            .door(door)
                                            .doorActionCause(DoorActionCause.AUTOCLOSE)
                                            .doorActionType(DoorActionType.CLOSE)
                                            .skipAnimation(skipAnimation)
                                            .time(time)
                                            .responsible(player)
                                            .build().execute();

                }
                deleteTimer(door.getDoorUID());
            }
        };
        timers.put(door.getDoorUID(), task);
        BigDoors.get().getPlatform().getPExecutor().runSyncLater(task, delay);
    }

    @Override
    public synchronized void restart()
    {
        shutdown();
    }

    @Override
    public synchronized void shutdown()
    {
        timers.forEach((key, val) -> val.cancel());
        timers.clear();
    }
}
