package nl.pim16aap2.bigDoors;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class AutoCloseScheduler
{
    private final BigDoors plugin;
    private Map<Long, BukkitTask> timers;

    public AutoCloseScheduler(final BigDoors plugin)
    {
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

    public void scheduleAutoClose(Door door, double time, boolean instantOpen)
    {
        if (door.getAutoClose() < 0 || !door.isOpen())
            return;

        if (door.getAutoClose() > BigDoors.get().getConfigLoader().maxAutoCloseTimer())
        {
            BigDoors.get().getMyLogger()
                .warn("Aborted autoCloseTimer for door: " + door.getDoorUID() + ", because it's autoCloseTimer ("
                    + door.getAutoClose() + ") exceeds the global limit ("
                    + BigDoors.get().getConfigLoader().maxAutoCloseTimer() + ").");
            BigDoors.get().getMyLogger()
                .warn("Either change the door's autoCloseTimer or increase the global limit in the config.");
            return;
        }

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
                    if (plugin.getCommander().isDoorBusy(door.getDoorUID()))
                    {
                        deleteTimer(door.getDoorUID());
                        cancel();
                    }
                    else
                        plugin.getDoorOpener(door.getType())
                            .openDoor(plugin.getCommander().getDoor(null, door.getDoorUID()), time, instantOpen, false);
                }
                deleteTimer(door.getDoorUID());
            }
        }.runTaskLater(plugin, delay));
    }
}
