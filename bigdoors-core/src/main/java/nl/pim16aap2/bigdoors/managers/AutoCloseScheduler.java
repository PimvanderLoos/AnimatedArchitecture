package nl.pim16aap2.bigdoors.managers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.util.Restartable;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class AutoCloseScheduler extends Restartable
{
    private final Map<Long, BukkitTask> timers;
    protected final BigDoors plugin;

    public AutoCloseScheduler(final BigDoors plugin)
    {
        super(plugin);
        this.plugin = plugin;
        timers = new HashMap<>();
    }

    public boolean isDoorWaiting(long doorUID)
    {
        return timers.containsKey(doorUID);
    }

    private void deleteTimer(long doorUID)
    {
        if (timers.containsKey(doorUID))
        {
            timers.get(doorUID).cancel();
            timers.remove(doorUID);
        }
    }

    public void cancelTimer(long doorUID)
    {
        deleteTimer(doorUID);
    }

    public void scheduleAutoClose(DoorBase door, double time, boolean instantOpen)
    {
        if (door.getAutoClose() < 0 || !door.isOpen())
            return;

        // First delete any old timers that might still be running.
        deleteTimer(door.getDoorUID());
        int delay = Math.max(plugin.getMinimumDoorDelay(), door.getAutoClose() * 20);

        timers.put(door.getDoorUID(), new BukkitRunnable()
        {
            @Override
            public void run()
            {
                if (door.isOpen())
                {
                    plugin.getDatabaseManager().setDoorAvailable(door.getDoorUID());
                    plugin.getDoorOpener(door.getType())
                          .openDoor(plugin.getDatabaseManager().getDoor(door.getDoorUID()),
                                    time, instantOpen, false);
                }
                deleteTimer(door.getDoorUID());
            }
            // Hard-code a slight delay on top of MinimumDoorDelay, to make sure it's fully updated after the last toggle.
        }.runTaskLater(plugin, delay));
    }

    @Override
    public void restart()
    {
        timers.forEach((K, V) -> V.cancel());
        timers.clear();
    }
}
