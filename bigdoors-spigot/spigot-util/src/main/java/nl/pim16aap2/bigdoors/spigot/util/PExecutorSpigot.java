package nl.pim16aap2.bigdoors.spigot.util;

import lombok.NonNull;
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
    private final @NonNull JavaPlugin plugin;

    public PExecutorSpigot(final @NonNull JavaPlugin plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public @NonNull <T> CompletableFuture<T> supplyOnMainThread(final @NonNull Supplier<T> supplier)
    {
        CompletableFuture<T> result = new CompletableFuture<>();
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> result.complete(supplier.get()));
        return result;
    }

    @Override
    public void runOnMainThread(final @NonNull Runnable runnable)
    {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable);
    }

    @Override
    public @NonNull <T> CompletableFuture<T> supplyAsync(final @NonNull Supplier<T> supplier)
    {
        CompletableFuture<T> result = new CompletableFuture<>();
        //noinspection deprecation
        Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, () -> result.complete(supplier.get()));
        return result;
    }

    @Override
    public int runAsync(final @NonNull Runnable runnable)
    {
        //noinspection deprecation
        return Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, runnable, 0);
    }

    @Override
    public int runSync(final @NonNull Runnable runnable)
    {
        return Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable, 0);
    }

    @Override
    public int runAsyncRepeated(final @NonNull TimerTask timerTask, long delay, long period)
    {
        // This is deprecated only because the name is supposedly confusing
        // (one might read it as scheduling "a sync" task).
        //noinspection deprecation
        return Bukkit.getScheduler().scheduleAsyncRepeatingTask(plugin, timerTask, delay, period);
    }

    @Override
    public int runSyncRepeated(final @NonNull TimerTask timerTask, long delay, long period)
    {
        return Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, timerTask, delay, period);
    }

    @Override
    public void runAsyncLater(final @NonNull TimerTask timerTask, long delay)
    {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, timerTask, delay);
    }

    @Override
    public void runSyncLater(final @NonNull TimerTask timerTask, long delay)
    {
        Bukkit.getScheduler().runTaskLater(plugin, timerTask, delay);
    }

    @Override
    public void cancel(final @NonNull TimerTask timerTask, final int taskID)
    {
        timerTask.cancel();
        Bukkit.getScheduler().cancelTask(taskID);
    }
}
