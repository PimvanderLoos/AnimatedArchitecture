package nl.pim16aap2.bigDoors.util;

import org.bukkit.scheduler.BukkitTask;

public abstract class Abortable
{
    private BukkitTask bukkitTask;

    public abstract void abort(boolean onDisable);

    protected void cancelTask()
    {
        bukkitTask.cancel();
    }

    public void abort()
    {
        abort(false);
    }

    public void setTask(BukkitTask task)
    {
        bukkitTask = task;
    }

    public BukkitTask getTask()
    {
        return bukkitTask;
    }
}
