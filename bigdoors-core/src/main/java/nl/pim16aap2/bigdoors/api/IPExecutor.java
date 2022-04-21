package nl.pim16aap2.bigdoors.api;

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
     * @param supplier
     *     A function returning the value to be used to complete the returned IMainThreadExecutor.
     * @return The result of the action.
     */
    <T> CompletableFuture<T> scheduleOnMainThread(Supplier<T> supplier);

    /**
     * Ensures a supplier is run on the main thread.
     * <p>
     * When called from the main thread, it will be executed immediately. When called from another thread, the supplier
     * is scheduled to run on the next tick of the main thread instead.
     *
     * @return The result of the action.
     */
    default <T> CompletableFuture<T> runOnMainThread(Supplier<T> supplier)
    {
        if (isMainThread())
            return CompletableFuture.completedFuture(supplier.get());
        else
            return scheduleOnMainThread(supplier);
    }

    /**
     * Schedules an action to be run on the main thread.
     */
    void scheduleOnMainThread(Runnable runnable);

    /**
     * Ensures a runnable is run on the main thread.
     * <p>
     * When called from the main thread, it will be executed immediately. When called from another thread, the supplier
     * is scheduled to run on the next tick of the main thread instead.
     */
    default void runOnMainThread(Runnable runnable)
    {
        if (isMainThread())
            runnable.run();
        else
            scheduleOnMainThread(runnable);
    }

    /**
     * Schedules a task to be run on the main thread.
     *
     * @param supplier
     *     A function returning the value to be used to complete the returned IMainThreadExecutor.
     * @return The result of the action.
     */
    <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier);

    /**
     * Schedules an action to be run asynchronously.
     *
     * @param runnable
     *     The action to run.
     * @return The ID of the task.
     */
    int runAsync(Runnable runnable);

    /**
     * Schedules an action to be run on the main thread.
     *
     * @param runnable
     *     The action to run.
     * @return The ID of the task.
     */
    int runSync(Runnable runnable);

    /**
     * Schedules a repeated {@link TimerTask} to be run asynchronously.
     *
     * @param timerTask
     *     The task to run.
     * @param delay
     *     The delay in ticks before the task is to be executed.
     * @param period
     *     The time in ticks between successive task executions.
     * @return The ID of the task.
     */
    int runAsyncRepeated(TimerTask timerTask, long delay, long period);

    /**
     * Schedules a repeated {@link Runnable} to be run asynchronously.
     *
     * @param runnable
     *     The task to run.
     * @param delay
     *     The delay in ticks before the task is to be executed.
     * @param period
     *     The time in ticks between successive task executions.
     * @return The ID of the task.
     */
    int runAsyncRepeated(Runnable runnable, long delay, long period);

    /**
     * Schedules a repeated {@link TimerTask} to be run on the main thread.
     *
     * @param timerTask
     *     The task to run.
     * @param delay
     *     The delay in ticks before the task is to be executed.
     * @param period
     *     The time in ticks between successive task executions.
     * @return The ID of the task.
     */
    int runSyncRepeated(TimerTask timerTask, long delay, long period);

    /**
     * Schedules a repeated {@link Runnable} to be run on the main thread.
     *
     * @param runnable
     *     The task to run.
     * @param delay
     *     The delay in ticks before the task is to be executed.
     * @param period
     *     The time in ticks between successive task executions.
     * @return The ID of the task.
     */
    int runSyncRepeated(Runnable runnable, long delay, long period);

    /**
     * Schedules a repeated {@link TimerTask} to be run asynchronously.
     *
     * @param timerTask
     *     The task to run.
     * @param delay
     *     The delay in ticks before the task is to be executed.
     */
    void runAsyncLater(TimerTask timerTask, long delay);

    /**
     * Schedules a repeated {@link Runnable} to be run asynchronously.
     *
     * @param runnable
     *     The task to run.
     * @param delay
     *     The delay in ticks before the task is to be executed.
     */
    void runAsyncLater(Runnable runnable, long delay);

    /**
     * Schedules a repeated {@link TimerTask} to be run on the main thread.
     *
     * @param timerTask
     *     The task to run.
     * @param delay
     *     The delay in ticks before the task is to be executed.
     */
    void runSyncLater(TimerTask timerTask, long delay);

    /**
     * Schedules a repeated {@link Runnable} to be run on the main thread.
     *
     * @param runnable
     *     The task to run.
     * @param delay
     *     The delay in ticks before the task is to be executed.
     */
    void runSyncLater(Runnable runnable, long delay);

    /**
     * Cancels a task.
     *
     * @param timerTask
     *     The task that is to be cancelled.
     * @param taskID
     *     The ID assigned to the task.
     */
    void cancel(TimerTask timerTask, int taskID);

    /**
     * Checks if a thread is the main thread.
     *
     * @param threadId
     *     The ID of the thread to compare.
     * @return True if the thread is the main thread.
     */
    boolean isMainThread(long threadId);

    /**
     * Checks if the current thread is the main thread.
     *
     * @return True if the current thread is the main thread.
     */
    default boolean isMainThread()
    {
        return isMainThread(Thread.currentThread().getId());
    }

    /**
     * Asserts that this method was called from the main thread. See {@link #isMainThread()}.
     *
     * @throws IllegalStateException
     *     When called from any thread that is not that main thread.
     */
    default void assertMainThread()
    {
        if (!isMainThread())
            throw new IllegalThreadStateException();
    }
}
