package nl.pim16aap2.bigdoors.spigot.util;

import nl.pim16aap2.bigdoors.api.IPExecutor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Implementation of {@link IPExecutor} for the Spigot platform.
 *
 * @author Pim
 */
public final class PExecutorSpigot implements IPExecutor
{
    private final JavaPlugin plugin;

    public PExecutorSpigot(final JavaPlugin plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public <T> CompletableFuture<T> supplyOnMainThread(final Supplier<T> supplier)
    {
        CompletableFuture<T> result = new CompletableFuture<>();
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> result.complete(supplier.get()));
        return result;
    }

    @Override
    public void runOnMainThread(final Runnable runnable)
    {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable);
    }

    @Override
    public <T> CompletableFuture<T> supplyAsync(final Supplier<T> supplier)
    {
        CompletableFuture<T> result = new CompletableFuture<>();
        //noinspection deprecation
        Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, () -> result.complete(supplier.get()));
        return result;
    }

    @Override
    public int runAsync(final Runnable runnable)
    {
        //noinspection deprecation
        return Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, runnable, 0);
    }

    @Override
    public int runSync(final Runnable runnable)
    {
        return Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable, 0);
    }

    @Override
    public int runAsyncRepeated(final TimerTask timerTask, long delay, long period)
    {
        // This is deprecated only because the name is supposedly confusing
        // (one might read it as scheduling "a sync" task).
        //noinspection deprecation
        return Bukkit.getScheduler().scheduleAsyncRepeatingTask(plugin, timerTask, delay, period);
    }

    @Override
    public int runAsyncRepeated(final Runnable runnable, long delay, long period)
    {
        // This is deprecated only because the name is supposedly confusing
        // (one might read it as scheduling "a sync" task).
        //noinspection deprecation
        return Bukkit.getScheduler().scheduleAsyncRepeatingTask(plugin, runnable, delay, period);
    }

    @Override
    public int runSyncRepeated(final TimerTask timerTask, long delay, long period)
    {
        return Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, timerTask, delay, period);
    }

    @Override
    public int runSyncRepeated(final Runnable runnable, long delay, long period)
    {
        return Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, runnable, delay, period);
    }

    @Override
    public void runAsyncLater(final TimerTask runnable, long delay)
    {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
    }

    @Override
    public void runAsyncLater(final Runnable runnable, long delay)
    {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
    }

    @Override
    public void runSyncLater(final TimerTask timerTask, long delay)
    {
        Bukkit.getScheduler().runTaskLater(plugin, timerTask, delay);
    }

    @Override
    public void runSyncLater(final Runnable runnable, long delay)
    {
        Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
    }

    @Override
    public void cancel(final TimerTask timerTask, final int taskID)
    {
        timerTask.cancel();
        Bukkit.getScheduler().cancelTask(taskID);
    }
}
