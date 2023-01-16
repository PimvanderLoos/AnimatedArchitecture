package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionCause;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionType;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableToggleRequestBuilder;
import nl.pim16aap2.bigdoors.movable.movablearchetypes.ITimerToggleable;
import nl.pim16aap2.bigdoors.util.Constants;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

/**
 * Represents a scheduler that automatically closes movables after a certain amount of time.
 *
 * @author Pim
 */
@Singleton
public final class AutoCloseScheduler extends Restartable
{
    /**
     * A map of {@link TimerTask}s.
     * <p>
     * <b>Key:</b> movableUID
     * <p>
     * <b>Value:</b> A {@link TimerTask} to toggle this movable again after a certain amount of time.
     */
    private final Map<Long, TimerTask> timers = new HashMap<>();
    private final MovableActivityManager movableActivityManager;
    private final MovableToggleRequestBuilder movableToggleRequestBuilder;
    private final IPExecutor executor;

    @Inject
    public AutoCloseScheduler(
        RestartableHolder holder, MovableActivityManager movableActivityManager,
        MovableToggleRequestBuilder movableToggleRequestBuilder, IPExecutor executor)
    {
        super(holder);
        this.movableActivityManager = movableActivityManager;
        this.movableToggleRequestBuilder = movableToggleRequestBuilder;
        this.executor = executor;
    }

    /**
     * Cancels and deleted the {@link TimerTask} of a movable if it exists.
     *
     * @param movableUID
     *     The UID of the movable.
     */
    private synchronized void deleteTimer(long movableUID)
    {
        final @Nullable TimerTask task = timers.remove(movableUID);
        if (task != null)
            task.cancel();
    }

    /**
     * Unschedules automatically closing a movable.
     *
     * @param movableUID
     *     The UID of the movable.
     */
    public synchronized void unscheduleAutoClose(long movableUID)
    {
        deleteTimer(movableUID);
    }

    /**
     * Schedules closing a movable.
     *
     * @param player
     *     The player who requested the movable toggle. May be null.
     * @param movable
     *     The movable to close.
     * @param time
     *     The duration (in seconds) of the animation.
     * @param skipAnimation
     *     Whether the movable should be animated or not.
     */
    synchronized <T extends AbstractMovable & ITimerToggleable> void scheduleAutoClose(
        IPPlayer player, T movable, double time, boolean skipAnimation)
    {
        final int autoCloseTimer = movable.getAutoCloseTime();
        if (autoCloseTimer < 0 || !movable.isOpen())
            return;

        // First delete any old timers that might still be running.
        deleteTimer(movable.getUid());
        // Add 2 ticks to the minimum delay to make sure there's no overlap with setting the movable available again.
        final int delay = Math.min(Constants.MINIMUM_MOVABLE_DELAY + 2, autoCloseTimer * 20);

        final TimerTask task = new TimerTask()
        {
            @Override
            public void run()
            {
                if (movable.isOpen())
                {
                    movableActivityManager.setMovableAvailable(movable.getUid());
                    movableToggleRequestBuilder.builder()
                                               .movable(movable)
                                               .movableActionCause(MovableActionCause.AUTOCLOSE)
                                               .movableActionType(MovableActionType.CLOSE)
                                               .skipAnimation(skipAnimation)
                                               .time(time)
                                               .responsible(player)
                                               .build().execute();

                }
                deleteTimer(movable.getUid());
            }
        };
        timers.put(movable.getUid(), task);
        executor.runSyncLater(task, delay);
    }

    @Override
    public synchronized void initialize()
    {
    }

    @Override
    public synchronized void shutDown()
    {
        timers.forEach((key, val) -> val.cancel());
        timers.clear();
    }
}
