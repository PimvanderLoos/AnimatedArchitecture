package nl.pim16aap2.bigdoors.spigot.util;

import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.util.PLogger;
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
public final class PExecutorSpigot<T> implements IPExecutor<T>
{
    @NotNull
    private final JavaPlugin plugin;
    @NotNull
    private final PLogger pLogger;

    @NotNull
    private final CompletableFuture<T> result = new CompletableFuture<>();

    public PExecutorSpigot(final @NotNull JavaPlugin plugin, final @NotNull PLogger pLogger)
    {
        this.plugin = plugin;
        this.pLogger = pLogger;
    }

    @Override
    public synchronized @NotNull CompletableFuture<T> supplyOnMainThread(final @NotNull Supplier<T> supplier)
    {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> result.complete(supplier.get()));
        return result;
    }

    @Override
    public synchronized void runOnMainThread(final @NotNull Runnable runnable)
    {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable);
    }

    @Override
    public synchronized @NotNull CompletableFuture<T> supplyAsync(final @NotNull Supplier<T> supplier)
    {
        //noinspection deprecation
        Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, () -> result.complete(supplier.get()));
        return result;
    }

    @Override
    public synchronized int runAsync(final @NotNull Runnable runnable)
    {
        //noinspection deprecation
        return Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, runnable, 0);
    }

    @Override
    public synchronized int runSync(final @NotNull Runnable runnable)
    {
        return Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable, 0);
    }

    @Override
    public int runAsyncRepeated(final @NotNull TimerTask timerTask, int delay, int period)
    {
        // This is deprecated only because the name is supposedly confusing
        // (one might read it as scheduling "a sync" task).
        return Bukkit.getScheduler().scheduleAsyncRepeatingTask(plugin, timerTask, delay, period);
    }

    @Override
    public int runSyncRepeated(final @NotNull TimerTask timerTask, int delay, int period)
    {
        return Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, timerTask, delay, period);
    }

    @Override
    public void runAsyncLater(final @NotNull TimerTask timerTask, int delay)
    {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, timerTask, delay);
    }

    @Override
    public void runSyncLater(final @NotNull TimerTask timerTask, int delay)
    {
        Bukkit.getScheduler().runTaskLater(plugin, timerTask, delay);
    }

    @Override
    public void cancel(final @NotNull TimerTask timerTask, final int taskID)
    {
        timerTask.cancel();
        Bukkit.getScheduler().cancelTask(taskID);
    }
}
