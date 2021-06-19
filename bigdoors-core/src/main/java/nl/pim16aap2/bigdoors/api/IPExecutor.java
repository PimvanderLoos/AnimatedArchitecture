package nl.pim16aap2.bigdoors.api;

import org.jetbrains.annotations.NotNull;

import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Represents an interface that allows scheduling (a)sync tasks.
 *
 * @author Pim
 */
public interface IPExecutor
{
    /**
     * Schedules a task to be run on the main thread.
     *
     * @param supplier A function returning the value to be used to complete the returned IMainThreadExecutor.
     * @return The result of the action.
     */
    @NotNull <T> CompletableFuture<T> supplyOnMainThread(@NotNull Supplier<T> supplier);

    /**
     * Schedules an action to be run on the main thread.
     *
     * @param runnable The action to run.
     */
    void runOnMainThread(@NotNull Runnable runnable);

    /**
     * Schedules a task to be run on the main thread.
     *
     * @param supplier A function returning the value to be used to complete the returned IMainThreadExecutor.
     * @return The result of the action.
     */
    @NotNull <T> CompletableFuture<T> supplyAsync(@NotNull Supplier<T> supplier);

    /**
     * Schedules an action to be run asynchronously.
     *
     * @param runnable The action to run.
     * @return The ID of the task.
     */
    int runAsync(@NotNull Runnable runnable);

    /**
     * Schedules an action to be run on the main thread.
     *
     * @param runnable The action to run.
     * @return The ID of the task.
     */
    int runSync(@NotNull Runnable runnable);

    /**
     * Schedules a repeated {@link TimerTask} to be run asynchronously.
     *
     * @param timerTask The task to run.
     * @param delay     The delay in ticks before the task is to be executed.
     * @param period    The time in ticks between successive task executions.
     * @return The ID of the task.
     */
    int runAsyncRepeated(@NotNull TimerTask timerTask, long delay, long period);

    /**
     * Schedules a repeated {@link Runnable} to be run asynchronously.
     *
     * @param runnable The task to run.
     * @param delay    The delay in ticks before the task is to be executed.
     * @param period   The time in ticks between successive task executions.
     * @return The ID of the task.
     */
    int runAsyncRepeated(@NotNull Runnable runnable, long delay, long period);

    /**
     * Schedules a repeated {@link TimerTask} to be run on the main thread.
     *
     * @param timerTask The task to run.
     * @param delay     The delay in ticks before the task is to be executed.
     * @param period    The time in ticks between successive task executions.
     * @return The ID of the task.
     */
    int runSyncRepeated(@NotNull TimerTask timerTask, long delay, long period);

    /**
     * Schedules a repeated {@link Runnable} to be run on the main thread.
     *
     * @param runnable The task to run.
     * @param delay    The delay in ticks before the task is to be executed.
     * @param period   The time in ticks between successive task executions.
     * @return The ID of the task.
     */
    int runSyncRepeated(@NotNull Runnable runnable, long delay, long period);

    /**
     * Schedules a repeated {@link TimerTask} to be run asynchronously.
     *
     * @param timerTask The task to run.
     * @param delay     The delay in ticks before the task is to be executed.
     */
    void runAsyncLater(@NotNull TimerTask timerTask, long delay);

    /**
     * Schedules a repeated {@link Runnable} to be run asynchronously.
     *
     * @param runnable The task to run.
     * @param delay    The delay in ticks before the task is to be executed.
     */
    void runAsyncLater(@NotNull Runnable runnable, long delay);

    /**
     * Schedules a repeated {@link TimerTask} to be run on the main thread.
     *
     * @param timerTask The task to run.
     * @param delay     The delay in ticks before the task is to be executed.
     */
    void runSyncLater(@NotNull TimerTask timerTask, long delay);

    /**
     * Schedules a repeated {@link Runnable} to be run on the main thread.
     *
     * @param runnable The task to run.
     * @param delay    The delay in ticks before the task is to be executed.
     */
    void runSyncLater(@NotNull Runnable runnable, long delay);

    /**
     * Cancels a task.
     *
     * @param timerTask The task that is to be cancelled.
     * @param taskID    The ID assigned to the task.
     */
    void cancel(@NotNull TimerTask timerTask, int taskID);
}
