package nl.pim16aap2.bigdoors.managers;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.util.Restartable;

public class AutoCloseScheduler extends Restartable
{
    private final Map<Long, BukkitTask> timers;

    public AutoCloseScheduler(final BigDoors plugin)
    {
        super(plugin);
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

    public void scheduleAutoClose(Door door, double time, boolean instantOpen)
    {
        int autoCloseTimer = door.getAutoClose();
        if (autoCloseTimer < 0 || !door.isOpen())
            return;

        // First delete any old timers that might still be running.
        deleteTimer(door.getDoorUID());

        timers.put(door.getDoorUID(), new BukkitRunnable()
        {
            @Override
            public void run()
            {
                if (door.isOpen())
                {
                    plugin.getDatabaseManager().setDoorAvailable(door.getDoorUID());
                    plugin.getDoorOpener(door.getType()).openDoor(plugin.getDatabaseManager().getDoor(null, door.getDoorUID()),
                                                                  time, instantOpen, false);
                }
                deleteTimer(door.getDoorUID());
            }
            // Hard-code 2 tick delay on top of MinimumDoorDelay, to make sure it's fully updated after the last toggle.
        }.runTaskLater(plugin, Math.min(plugin.getMinimumDoorDelay() + 2, autoCloseTimer * 20)));
    }

    @Override
    public void restart()
    {
        timers.forEach((K, V) -> V.cancel());
        timers.clear();
    }
}