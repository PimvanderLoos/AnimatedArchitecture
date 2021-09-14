package nl.pim16aap2.bigdoors.spigot.util.implementations.pexecutor;

import nl.pim16aap2.bigdoors.api.IPExecutor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Implementation of {@link IPExecutor} for the Spigot platform.
 *
 * @author Pim
 */
@Singleton
public final class PExecutorSpigot implements IPExecutor
{
    private final JavaPlugin plugin;

    @Inject
    public PExecutorSpigot(JavaPlugin plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public <T> CompletableFuture<T> supplyOnMainThread(Supplier<T> supplier)
    {
        final CompletableFuture<T> result = new CompletableFuture<>();
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> result.complete(supplier.get()));
        return result;
    }

    @Override
    public void runOnMainThread(Runnable runnable)
    {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable);
    }

    @Override
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier)
    {
        final CompletableFuture<T> result = new CompletableFuture<>();
        //noinspection deprecation
        Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, () -> result.complete(supplier.get()));
        return result;
    }

    @Override
    public int runAsync(Runnable runnable)
    {
        //noinspection deprecation
        return Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, runnable, 0);
    }

    @Override
    public int runSync(Runnable runnable)
    {
        return Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable, 0);
    }

    @Override
    public int runAsyncRepeated(TimerTask timerTask, long delay, long period)
    {
        // This is deprecated only because the name is supposedly confusing
        // (one might read it as scheduling "a sync" task).
        //noinspection deprecation
        return Bukkit.getScheduler().scheduleAsyncRepeatingTask(plugin, timerTask, delay, period);
    }

    @Override
    public int runAsyncRepeated(Runnable runnable, long delay, long period)
    {
        // This is deprecated only because the name is supposedly confusing
        // (one might read it as scheduling "a sync" task).
        //noinspection deprecation
        return Bukkit.getScheduler().scheduleAsyncRepeatingTask(plugin, runnable, delay, period);
    }

    @Override
    public int runSyncRepeated(TimerTask timerTask, long delay, long period)
    {
        return Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, timerTask, delay, period);
    }

    @Override
    public int runSyncRepeated(Runnable runnable, long delay, long period)
    {
        return Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, runnable, delay, period);
    }

    @Override
    public void runAsyncLater(TimerTask runnable, long delay)
    {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
    }

    @Override
    public void runAsyncLater(Runnable runnable, long delay)
    {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
    }

    @Override
    public void runSyncLater(TimerTask timerTask, long delay)
    {
        Bukkit.getScheduler().runTaskLater(plugin, timerTask, delay);
    }

    @Override
    public void runSyncLater(Runnable runnable, long delay)
    {
        Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
    }

    @Override
    public void cancel(TimerTask timerTask, int taskID)
    {
        timerTask.cancel();
        Bukkit.getScheduler().cancelTask(taskID);
    }
}
