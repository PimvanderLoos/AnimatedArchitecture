package nl.pim16aap2.animatedarchitecture.spigot.util.implementations;

import lombok.experimental.ExtensionMethod;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.IRestartable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import nl.pim16aap2.animatedarchitecture.core.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Implementation of {@link IExecutor} for the Spigot platform.
 */
@Singleton
@Flogger
@ExtensionMethod(CompletableFutureExtensions.class)
public final class ExecutorSpigot implements IExecutor, IRestartable, IDebuggable
{
    private final JavaPlugin plugin;
    private final long mainThreadId;

    private volatile ExecutorService executor = newVirtualExecutorService();

    @Inject
    public ExecutorSpigot(
        JavaPlugin plugin,
        @Named("mainThreadId") long mainThreadId,
        RestartableHolder restartableHolder)
    {
        this.plugin = plugin;
        this.mainThreadId = mainThreadId;

        restartableHolder.registerRestartable(this);
    }

    @Override
    public ExecutorService getVirtualExecutor()
    {
        return executor;
    }

    @Override
    public <T> CompletableFuture<T> scheduleOnMainThread(Supplier<T> supplier)
    {
        final CompletableFuture<T> result = new CompletableFuture<>();
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> result.complete(supplier.get()));
        return result;
    }

    @Override
    public void scheduleOnMainThread(Runnable runnable)
    {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, safeRunnable(runnable));
    }

    @Override
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier)
    {
        final CompletableFuture<T> result = new CompletableFuture<>();
        //noinspection deprecation
        Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, () -> result.complete(loggedSupplier(supplier).get()));
        return result;
    }

    @Override
    public int runAsync(Runnable runnable)
    {
        //noinspection deprecation
        return Bukkit.getScheduler().scheduleAsyncDelayedTask(plugin, safeRunnable(runnable), 0);
    }

    @Override
    public int runSync(Runnable runnable)
    {
        return Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, safeRunnable(runnable), 0);
    }

    private static long toTicks(long milliseconds)
    {
        return Math.round(milliseconds / 50D);
    }

    @Override
    public int runAsyncRepeated(TimerTask timerTask, long delay, long period)
    {
        // This is deprecated only because the name is supposedly confusing
        // (one might read it as scheduling "a sync" task).
        //noinspection deprecation
        return Bukkit.getScheduler()
            .scheduleAsyncRepeatingTask(plugin, safeTimerTask(timerTask), toTicks(delay), toTicks(period));
    }

    @Override
    public int runAsyncRepeated(Runnable runnable, long delay, long period)
    {
        // This is deprecated only because the name is supposedly confusing
        // (one might read it as scheduling "a sync" task).
        //noinspection deprecation
        return Bukkit.getScheduler()
            .scheduleAsyncRepeatingTask(plugin, safeRunnable(runnable), toTicks(delay), toTicks(period));
    }

    @Override
    public int runSyncRepeated(TimerTask timerTask, long delay, long period)
    {
        return Bukkit.getScheduler()
            .scheduleSyncRepeatingTask(plugin, safeTimerTask(timerTask), toTicks(delay), toTicks(period));
    }

    @Override
    public int runSyncRepeated(Runnable runnable, long delay, long period)
    {
        return Bukkit.getScheduler()
            .scheduleSyncRepeatingTask(plugin, safeRunnable(runnable), toTicks(delay), toTicks(period));
    }

    @Override
    public void runAsyncLater(TimerTask timerTask, long delay)
    {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, safeTimerTask(timerTask), toTicks(delay));
    }

    @Override
    public void runAsyncLater(Runnable runnable, long delay)
    {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, safeRunnable(runnable), toTicks(delay));
    }

    @Override
    public void runSyncLater(TimerTask timerTask, long delay)
    {
        Bukkit.getScheduler().runTaskLater(plugin, safeTimerTask(timerTask), toTicks(delay));
    }

    @Override
    public void runSyncLater(Runnable runnable, long delay)
    {
        Bukkit.getScheduler().runTaskLater(plugin, safeRunnable(runnable), toTicks(delay));
    }

    @Override
    public void cancel(TimerTask timerTask, int taskID)
    {
        timerTask.cancel();
        Bukkit.getScheduler().cancelTask(taskID);
    }

    @Override
    public boolean isMainThread(long threadId)
    {
        return threadId == mainThreadId;
    }

    private Runnable safeRunnable(Runnable runnable)
    {
        return () ->
        {
            try
            {
                runnable.run();
            }
            catch (Exception e)
            {
                log.atSevere().withCause(e).log("Encountered an exception while executing a runnable.");
            }
        };
    }

    private TimerTask safeTimerTask(TimerTask timerTask)
    {
        return new TimerTask()
        {
            @Override
            public void run()
            {
                try
                {
                    timerTask.run();
                }
                catch (Exception e)
                {
                    log.atSevere().withCause(e).log("Encountered an exception while executing a timer task.");
                }
            }
        };
    }

    private <T> Supplier<T> loggedSupplier(Supplier<T> supplier)
    {
        return () ->
        {
            try
            {
                return supplier.get();
            }
            catch (Exception e)
            {
                log.atSevere().withCause(e).log("Encountered an exception while executing a supplier.");
                throw new RuntimeException(e);
            }
        };
    }

    @Override
    public void initialize()
    {
        // It is already initialized and running before the first call to this method.
        if (!executor.isShutdown())
            return;
        executor = newVirtualExecutorService();
    }

    @Override
    public void shutDown()
    {
        final var executor0 = executor;
        executor0.shutdown();
        try
        {
            if (!executor0.awaitTermination(30, TimeUnit.SECONDS))
                log.atSevere().log("Timed out waiting to terminate General ExecutorService!");
        }
        catch (InterruptedException exception)
        {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                "Thread got interrupted waiting for General ExecutorService to terminate!",
                exception
            );
        }
    }

    /**
     * Instantiates a new virtual executor service.
     *
     * @return the executor service
     */
    private static ExecutorService newVirtualExecutorService()
    {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public String getDebugInformation()
    {
        return "General " + StringUtil.toString(executor);
    }
}
