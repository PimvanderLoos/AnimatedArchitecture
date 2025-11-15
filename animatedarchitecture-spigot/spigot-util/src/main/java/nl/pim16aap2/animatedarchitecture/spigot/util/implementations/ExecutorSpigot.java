package nl.pim16aap2.animatedarchitecture.spigot.util.implementations;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import lombok.experimental.ExtensionMethod;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.IRestartable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import nl.pim16aap2.animatedarchitecture.core.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Implementation of {@link IExecutor} for the Spigot platform.
 */
@Singleton
@CustomLog
@ExtensionMethod(CompletableFutureExtensions.class)
public final class ExecutorSpigot implements IExecutor, IRestartable, IDebuggable
{
    private final JavaPlugin plugin;
    private final long mainThreadId;

    @Getter
    private volatile ExecutorService virtualExecutor = newVirtualExecutorService();

    @Getter
    private volatile ScheduledExecutorService scheduler = newSchedulerService();

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
    public <T> CompletableFuture<T> scheduleOnMainThread(Supplier<T> supplier)
    {
        final CompletableFuture<T> result = new CompletableFuture<>();
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> result.complete(loggedSupplier(supplier).get()));
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
        return CompletableFuture.supplyAsync(
            loggedSupplier(supplier),
            virtualExecutor
        );
    }

    @Override
    public CompletableFuture<Void> runAsync(Runnable runnable)
    {
        return CompletableFuture.runAsync(
            safeRunnable(runnable),
            virtualExecutor
        );
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
    public ScheduledFuture<?> runAsyncRepeated(TimerTask timerTask, long delay, long period)
    {
        return scheduler.scheduleAtFixedRate(
            safeTimerTask(timerTask),
            delay,
            period,
            TimeUnit.MILLISECONDS
        );
    }

    @Override
    public ScheduledFuture<?> runAsyncRepeated(Runnable runnable, long delay, long period)
    {
        return scheduler.scheduleAtFixedRate(
            safeRunnable(runnable),
            delay,
            period,
            TimeUnit.MILLISECONDS
        );
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
        CompletableFuture.runAsync(
            safeTimerTask(timerTask),
            CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS, virtualExecutor)
        );
    }

    @Override
    public void runAsyncLater(Runnable runnable, long delay)
    {
        CompletableFuture.runAsync(
            safeRunnable(runnable),
            CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS, virtualExecutor)
        );
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
                log.atError().withCause(e).log("Encountered an exception while executing a runnable.");
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
                    log.atError().withCause(e).log("Encountered an exception while executing a timer task.");
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
                log.atError().withCause(e).log("Encountered an exception while executing a supplier.");
                throw new RuntimeException(e);
            }
        };
    }

    @Override
    public void initialize()
    {
        // It is already initialized and running before the first call to this method.
        if (virtualExecutor.isShutdown())
            virtualExecutor = newVirtualExecutorService();
        if (scheduler.isShutdown())
            scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    private void shutDownService(ExecutorService executor0)
    {
        executor0.shutdown();
        try
        {
            if (!executor0.awaitTermination(30, TimeUnit.SECONDS))
                log.atError().log("Timed out waiting to terminate General ExecutorService!");
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

    @Override
    public void shutDown()
    {
        shutDownService(virtualExecutor);
        shutDownService(scheduler);
    }

    /**
     * Instantiates a new virtual executor service.
     *
     * @return the executor service
     */
    private static ExecutorService newVirtualExecutorService()
    {
        return Executors.newThreadPerTaskExecutor(
            newThreadFactory("animated-architecture-")
        );
    }

    private static ScheduledExecutorService newSchedulerService()
    {
        return Executors.newSingleThreadScheduledExecutor(
            newThreadFactory("animated-architecture-scheduler-")
        );
    }

    private static ThreadFactory newThreadFactory(String namePrefix)
    {
        return Thread.ofVirtual()
            .name(namePrefix, 0)
            .factory();
    }

    @Override
    public String getDebugInformation()
    {
        return "Executor:  " + StringUtil.toString(virtualExecutor) + "\n" +
            "Scheduler: " + StringUtil.toString(scheduler);
    }
}
