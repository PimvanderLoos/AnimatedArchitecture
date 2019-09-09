package nl.pim16aap2.bigdoors.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.TimerTask;
import java.util.function.Supplier;

/**
 * Represents an interface that allows scheduling (a)sync tasks.
 *
 * @author Pim
 */
public interface IPExecutor<T>
{
    /**
     * Schedules a task to be run on the main thread.
     *
     * @param supplier A function returning the value to be used to complete the returned IMainThreadExecutor.
     * @return The result or null if an error occurred.
     */
    @Nullable
    T supplyOnMainThread(final @NotNull Supplier<T> supplier);

    /**
     * Schedules an action to be run on the main thread.
     *
     * @param runnable The action to run.
     */
    void runOnMainThread(final @NotNull Runnable runnable);

    /**
     * Schedules a task to be run on the main thread.
     *
     * @param supplier A function returning the value to be used to complete the returned IMainThreadExecutor.
     * @return The result or null if an error occurred.
     */
    @Nullable
    T supplyAsync(final @NotNull Supplier<T> supplier);

    /**
     * Schedules an action to be run asynchronously.
     *
     * @param runnable The action to run.
     * @return The ID of the task.
     */
    int runAsync(final @NotNull Runnable runnable);

    /**
     * Schedules an action to be run on the main thread.
     *
     * @param runnable The action to run.
     * @return The ID of the task.
     */
    int runSync(final @NotNull Runnable runnable);

    /**
     * Schedules a repeated {@link TimerTask} to be run asynchronously.
     *
     * @param timerTask The task to run.
     * @param delay     The delay in ticks before the task is to be executed.
     * @param period    The time in ticks between successive task executions.
     */
    void runAsyncRepeated(final @NotNull TimerTask timerTask, final int delay, final int period);

    /**
     * Schedules a repeated {@link TimerTask} to be run on the main thread.
     *
     * @param timerTask The task to run.
     * @param delay     The delay in ticks before the task is to be executed.
     * @param period    The time in ticks between successive task executions.
     */
    void runSyncRepeated(final @NotNull TimerTask timerTask, final int delay, final int period);

    /**
     * Schedules a repeated {@link TimerTask} to be run asynchronously.
     *
     * @param timerTask The task to run.
     * @param delay     The delay in ticks before the task is to be executed.
     */
    void runAsyncLater(final @NotNull TimerTask timerTask, final int delay);

    /**
     * Schedules a repeated {@link TimerTask} to be run on the main thread.
     *
     * @param timerTask The task to run.
     * @param delay     The delay in ticks before the task is to be executed.
     */
    void runSyncLater(final @NotNull TimerTask timerTask, final int delay);
}
