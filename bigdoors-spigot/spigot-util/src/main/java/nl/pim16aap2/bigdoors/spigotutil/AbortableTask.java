package nl.pim16aap2.bigdoors.spigotutil;

import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an abortable BukkitTask.
 *
 * @author Pim
 */
public abstract class AbortableTask
{
    private BukkitTask bukkitTask;

    /**
     * Abort this bukkitTask. Aborting allows the abortable to finish up gracefully.
     * <p>
     * Implementations HAVE TO CALL {@link #killTask()}.
     *
     * @param onDisable Set to true if called when the plugin is being disabled.
     */
    public abstract void abort(final boolean onDisable);

    /**
     * Kill this bukkitTask.
     */
    protected final void killTask()
    {
        bukkitTask.cancel();
    }

    /**
     * Abort this bukkitTask.
     * <p>
     * Aborting allows the abortable to finish up gracefully.
     */
    public final void abort()
    {
        abort(false);
    }

    /**
     * Abort this bukkitTask without notifying the user.
     * <p>
     * Aborting allows the abortable to finish up gracefully.
     */
    public abstract void abortSilently();

    /**
     * Set the BukkitTask of this abortable.
     *
     * @param task The BukkitTask.
     */
    public final void setTask(final @NotNull BukkitTask task)
    {
        bukkitTask = task;
    }
}
