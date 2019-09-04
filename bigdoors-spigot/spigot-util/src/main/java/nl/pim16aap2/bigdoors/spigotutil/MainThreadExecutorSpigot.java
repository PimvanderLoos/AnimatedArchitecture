package nl.pim16aap2.bigdoors.spigotutil;

import nl.pim16aap2.bigdoors.api.IMainThreadExecutor;
import nl.pim16aap2.bigdoors.util.PLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Supplier;

/**
 * Implementation of {@link IMainThreadExecutor} for the Spigot platform.
 *
 * @author Pim
 */
public final class MainThreadExecutorSpigot<T> implements IMainThreadExecutor<T>
{
    @NotNull
    private final JavaPlugin plugin;
    @NotNull
    private final PLogger pLogger;

    /**
     * Used to store the result of a supplier (as Bukkit's scheduler doesn't allow suppliers).
     */
    private final ArrayBlockingQueue<T> result = new ArrayBlockingQueue<>(1);

    public MainThreadExecutorSpigot(final @NotNull JavaPlugin plugin, final @NotNull PLogger pLogger)
    {
        this.plugin = plugin;
        this.pLogger = pLogger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nullable
    public T supplyOnMainThread(final @NotNull Supplier<T> supplier)
    {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> result.add(supplier.get()));
        try
        {
            return result.take();
        }
        catch (InterruptedException e)
        {
            pLogger.logException(e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runOnMainThread(final @NotNull Runnable runnable)
    {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable);
    }
}
