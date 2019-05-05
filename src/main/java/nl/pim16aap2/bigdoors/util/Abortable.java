package nl.pim16aap2.bigdoors.util;

import org.bukkit.scheduler.BukkitTask;

public abstract class Abortable
{
    private BukkitTask bukkitTask;

    // When disabling, some aborting logic might need to be different
    // (e.g. cannot launch new tasks). When implementing this, it NEEDS
    // to call cancelTask();
    public abstract void abort(boolean onDisable);

    protected void cancelTask()
    {
        bukkitTask.cancel();
    }

    public final void abort()
    {
        abort(false);
    }

    public abstract void abortSilently();

    public void setTask(BukkitTask task)
    {
        bukkitTask = task;
    }
}
