package nl.pim16aap2.bigdoors.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Represents an interface that allows scheduling tasks on the main thread.
 *
 * @author Pim
 */
public interface IMainThreadExecutor<T>
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
}
