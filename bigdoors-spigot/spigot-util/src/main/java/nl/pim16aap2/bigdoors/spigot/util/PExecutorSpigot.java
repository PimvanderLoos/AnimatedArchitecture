package nl.pim16aap2.bigdoors.spigot.util;

import nl.pim16aap2.bigdoors.api.IPExecutor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

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
    private final @NotNull JavaPlugin plugin;

    public PExecutorSpigot(final @NotNull JavaPlugin plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public @NotNull <T> CompletableFuture<T> supplyOnMainThread(final @NotNull Supplier<T> supplier)
    {
        CompletableFuture<T> result = new CompletableFuture<>();
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> result.complete(supplier.get()));
        return result;
    }

    @Override
    public void runOnMainThread(final @NotNull Runnable runnable)
    {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable);
    }

    @Override
    public @NotNull <T> CompletableFuture<T> supplyAsync(final @NotNull Supplier<T> supplier)
    {
        CompletableFuture<T> result = new CompletableFuture<>();
        //noinspection deprecation
        Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, () -> result.complete(supplier.get()));
        return result;
    }

    @Override
    public int runAsync(final @NotNull Runnable runnable)
    {
        //noinspection deprecation
        return Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, runnable, 0);
    }

    @Override
    public int runSync(final @NotNull Runnable runnable)
    {
        return Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable, 0);
    }

    @Override
    public int runAsyncRepeated(final @NotNull TimerTask timerTask, long delay, long period)
    {
        // This is deprecated only because the name is supposedly confusing
        // (one might read it as scheduling "a sync" task).
        //noinspection deprecation
        return Bukkit.getScheduler().scheduleAsyncRepeatingTask(plugin, timerTask, delay, period);
    }

    @Override
    public int runAsyncRepeated(final @NotNull Runnable runnable, long delay, long period)
    {
        // This is deprecated only because the name is supposedly confusing
        // (one might read it as scheduling "a sync" task).
        //noinspection deprecation
        return Bukkit.getScheduler().scheduleAsyncRepeatingTask(plugin, runnable, delay, period);
    }

    @Override
    public int runSyncRepeated(final @NotNull TimerTask timerTask, long delay, long period)
    {
        return Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, timerTask, delay, period);
    }

    @Override
    public int runSyncRepeated(final @NotNull Runnable runnable, long delay, long period)
    {
        return Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, runnable, delay, period);
    }

    @Override
    public void runAsyncLater(final @NotNull TimerTask runnable, long delay)
    {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
    }

    @Override
    public void runAsyncLater(final @NotNull Runnable runnable, long delay)
    {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
    }

    @Override
    public void runSyncLater(final @NotNull TimerTask timerTask, long delay)
    {
        Bukkit.getScheduler().runTaskLater(plugin, timerTask, delay);
    }

    @Override
    public void runSyncLater(final @NotNull Runnable runnable, long delay)
    {
        Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
    }

    @Override
    public void cancel(final @NotNull TimerTask timerTask, final int taskID)
    {
        timerTask.cancel();
        Bukkit.getScheduler().cancelTask(taskID);
    }
}
