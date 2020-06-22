package nl.pim16aap2.bigdoors.spigot.util;

import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.util.PLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Supplier;

/**
 * Implementation of {@link IPExecutor} for the Spigot platform.
 *
 * @author Pim
 */
public final class PExecutorSpigot<T> implements IPExecutor<T>
{
    @NotNull
    private final JavaPlugin plugin;
    @NotNull
    private final PLogger pLogger;

    /**
     * Used to store the result of a supplier (as Bukkit's scheduler doesn't allow suppliers).
     */
    private final ArrayBlockingQueue<T> result = new ArrayBlockingQueue<>(1);

    public PExecutorSpigot(final @NotNull JavaPlugin plugin, final @NotNull PLogger pLogger)
    {
        this.plugin = plugin;
        this.pLogger = pLogger;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable
    public synchronized T supplyOnMainThread(final @NotNull Supplier<T> supplier)
    {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> result.add(supplier.get()));
        try
        {
            return result.take();
        }
        catch (InterruptedException e)
        {
            pLogger.logException(e);
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void runOnMainThread(final @NotNull Runnable runnable)
    {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable);
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public synchronized T supplyAsync(final @NotNull Supplier<T> supplier)
    {
        //noinspection deprecation
        Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, () -> result.add(supplier.get()));
        try
        {
            return result.take();
        }
        catch (InterruptedException e)
        {
            pLogger.logException(e);
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized int runAsync(final @NotNull Runnable runnable)
    {
        //noinspection deprecation
        return Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, runnable, 0);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized int runSync(final @NotNull Runnable runnable)
    {
        return Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable, 0);
    }

    /** {@inheritDoc} */
    @Override
    public int runAsyncRepeated(final @NotNull TimerTask timerTask, int delay, int period)
    {
        // This is deprecated only because the name is supposedly confusing
        // (one might read it as scheduling "a sync" task).
        return Bukkit.getScheduler().scheduleAsyncRepeatingTask(plugin, timerTask, delay, period);
    }

    /** {@inheritDoc} */
    @Override
    public int runSyncRepeated(final @NotNull TimerTask timerTask, int delay, int period)
    {
        return Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, timerTask, delay, period);
    }

    /** {@inheritDoc} */
    @Override
    public void runAsyncLater(final @NotNull TimerTask timerTask, int delay)
    {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, timerTask, delay);
    }

    /** {@inheritDoc} */
    @Override
    public void runSyncLater(final @NotNull TimerTask timerTask, int delay)
    {
        Bukkit.getScheduler().runTaskLater(plugin, timerTask, delay);
    }

    /** {@inheritDoc} */
    @Override
    public void cancel(final @NotNull TimerTask timerTask, final int taskID)
    {
        timerTask.cancel();
        Bukkit.getScheduler().cancelTask(taskID);
    }
}
