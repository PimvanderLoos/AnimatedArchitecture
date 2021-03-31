package nl.pim16aap2.bigdoors.api;

import lombok.NonNull;

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
    @NonNull <T> CompletableFuture<T> supplyOnMainThread(final @NonNull Supplier<T> supplier);

    /**
     * Schedules an action to be run on the main thread.
     *
     * @param runnable The action to run.
     */
    void runOnMainThread(final @NonNull Runnable runnable);

    /**
     * Schedules a task to be run on the main thread.
     *
     * @param supplier A function returning the value to be used to complete the returned IMainThreadExecutor.
     * @return The result of the action.
     */
    @NonNull <T> CompletableFuture<T> supplyAsync(final @NonNull Supplier<T> supplier);

    /**
     * Schedules an action to be run asynchronously.
     *
     * @param runnable The action to run.
     * @return The ID of the task.
     */
    int runAsync(final @NonNull Runnable runnable);

    /**
     * Schedules an action to be run on the main thread.
     *
     * @param runnable The action to run.
     * @return The ID of the task.
     */
    int runSync(final @NonNull Runnable runnable);

    /**
     * Schedules a repeated {@link TimerTask} to be run asynchronously.
     *
     * @param timerTask The task to run.
     * @param delay     The delay in ticks before the task is to be executed.
     * @param period    The time in ticks between successive task executions.
     * @return The ID of the task.
     */
    int runAsyncRepeated(final @NonNull TimerTask timerTask, final long delay, final long period);

    /**
     * Schedules a repeated {@link Runnable} to be run asynchronously.
     *
     * @param runnable The task to run.
     * @param delay    The delay in ticks before the task is to be executed.
     * @param period   The time in ticks between successive task executions.
     * @return The ID of the task.
     */
    int runAsyncRepeated(final @NonNull Runnable runnable, final long delay, final long period);

    /**
     * Schedules a repeated {@link TimerTask} to be run on the main thread.
     *
     * @param timerTask The task to run.
     * @param delay     The delay in ticks before the task is to be executed.
     * @param period    The time in ticks between successive task executions.
     * @return The ID of the task.
     */
    int runSyncRepeated(final @NonNull TimerTask timerTask, final long delay, final long period);

    /**
     * Schedules a repeated {@link Runnable} to be run on the main thread.
     *
     * @param runnable The task to run.
     * @param delay    The delay in ticks before the task is to be executed.
     * @param period   The time in ticks between successive task executions.
     * @return The ID of the task.
     */
    int runSyncRepeated(final @NonNull Runnable runnable, final long delay, final long period);

    /**
     * Schedules a repeated {@link TimerTask} to be run asynchronously.
     *
     * @param timerTask The task to run.
     * @param delay     The delay in ticks before the task is to be executed.
     */
    void runAsyncLater(final @NonNull TimerTask timerTask, final long delay);

    /**
     * Schedules a repeated {@link Runnable} to be run asynchronously.
     *
     * @param runnable The task to run.
     * @param delay    The delay in ticks before the task is to be executed.
     */
    void runAsyncLater(final @NonNull Runnable runnable, final long delay);

    /**
     * Schedules a repeated {@link TimerTask} to be run on the main thread.
     *
     * @param timerTask The task to run.
     * @param delay     The delay in ticks before the task is to be executed.
     */
    void runSyncLater(final @NonNull TimerTask timerTask, final long delay);

    /**
     * Schedules a repeated {@link Runnable} to be run on the main thread.
     *
     * @param runnable The task to run.
     * @param delay    The delay in ticks before the task is to be executed.
     */
    void runSyncLater(final @NonNull Runnable runnable, final long delay);

    /**
     * Cancels a task.
     *
     * @param timerTask The task that is to be cancelled.
     * @param taskID    The ID assigned to the task.
     */
    void cancel(final @NonNull TimerTask timerTask, final int taskID);
}
