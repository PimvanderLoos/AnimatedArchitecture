package nl.pim16aap2.bigdoors.managers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.util.Restartable;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a scheduler that automatically closes doors after a certain amount of time.
 */
public class AutoCloseScheduler extends Restartable
{
    protected final BigDoors plugin;
    /**
     * A map of {@link BukkitTask}s.
     * <p>
     * Key: doorUID
     * <p>
     * Value: A {@link BukkitTask} to toggle this door again after a certain amount of time.
     */
    private final Map<Long, BukkitTask> timers;

    public AutoCloseScheduler(final BigDoors plugin)
    {
        super(plugin);
        this.plugin = plugin;
        timers = new HashMap<>();
    }

    /**
     * Cancels and deleted the {@link BukkitTask} of a door if it exists.
     *
     * @param doorUID The UID of the door.
     */
    private void deleteTimer(long doorUID)
    {
        if (timers.containsKey(doorUID))
        {
            timers.get(doorUID).cancel();
            timers.remove(doorUID);
        }
    }

    /**
     * Unschedules automatically closing a door.
     *
     * @param doorUID The UID of the door.
     */
    public void unscheduleAutoClose(long doorUID)
    {
        deleteTimer(doorUID);
    }

    /**
     * Schedule closing a door.
     *
     * @param playerUUID  The {@link UUID} of the player who requested the door toggle. May be null.
     * @param door        The door to close.
     * @param speed       The speed at which the door should move.
     * @param instantOpen Whether the door should be animated or not.
     */
    public void scheduleAutoClose(@Nullable UUID playerUUID, @NotNull DoorBase door, double speed, boolean instantOpen)
    {
        int autoCloseTimer = door.getAutoClose();
        if (autoCloseTimer < 0 || !door.isOpen())
            return;

        // First delete any old timers that might still be running.
        deleteTimer(door.getDoorUID());
        // Add 2 ticks to the minimum delay to make sure there's no overlap with setting the door
        // available again.
        int delay = Math.min(plugin.getMinimumDoorDelay() + 2, autoCloseTimer * 20);

        timers.put(door.getDoorUID(), new BukkitRunnable()
        {
            @Override
            public void run()
            {
                if (door.isOpen())
                {
                    plugin.getDatabaseManager().setDoorAvailable(door.getDoorUID());
                    plugin.getDatabaseManager().getDoor(door.getDoorUID()).ifPresent(
                        door -> door.open(plugin.getDoorOpener(), DoorActionCause.REDSTONE, speed, instantOpen));
                }
                deleteTimer(door.getDoorUID());
            }
        }.runTaskLater(plugin, delay));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restart()
    {
        shutdown();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown()
    {
        timers.forEach((K, V) -> V.cancel());
        timers.clear();
    }
}
