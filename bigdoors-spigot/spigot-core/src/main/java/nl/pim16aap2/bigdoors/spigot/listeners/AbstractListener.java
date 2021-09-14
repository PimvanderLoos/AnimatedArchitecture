package nl.pim16aap2.bigdoors.spigot.listeners;

import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Represents a base Listener class.
 *
 * @author Pim
 */
public abstract class AbstractListener implements Listener, IRestartable
{
    private final @Nullable Supplier<Boolean> enabler;
    protected final JavaPlugin plugin;

    protected boolean isRegistered = false;

    // Private back-end constructor that has the true nullability annotations for the different types.
    private AbstractListener(JavaPlugin plugin, @Nullable RestartableHolder holder,
                             @Nullable Supplier<Boolean> enabler)
    {
        if (holder != null)
            holder.registerRestartable(this);
        this.plugin = plugin;
        this.enabler = enabler;
        init();
    }

    /**
     * @param plugin
     *     The {@link JavaPlugin} to use for (un)registering this listener. May be null.
     * @param enabler
     *     A supplier that is used to query whether this listener should be enabled. When this is not provided, it is
     *     assumed that this listener should always be enabled.
     */
    public AbstractListener(RestartableHolder holder, JavaPlugin plugin, Supplier<Boolean> enabler)
    {
        this(plugin, holder, enabler);
    }

    /**
     * See {@link #AbstractListener(RestartableHolder, JavaPlugin, Supplier)}
     */
    protected AbstractListener(RestartableHolder holder, JavaPlugin plugin)
    {
        this(plugin, holder, null);
    }

    /**
     * See {@link #AbstractListener(RestartableHolder, JavaPlugin, Supplier)}
     */
    protected AbstractListener(JavaPlugin plugin)
    {
        this(plugin, null, null);
    }

    private void init()
    {
        if (enabler == null || enabler.get())
            register();
    }

    /**
     * Registers this listener if it isn't already registered.
     */
    protected void register()
    {
        if (isRegistered)
            return;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        isRegistered = true;
    }

    /**
     * Unregisters this listener if it isn't already unregistered.
     */
    protected void unregister()
    {
        if (!isRegistered)
            return;
        HandlerList.unregisterAll(this);
        isRegistered = false;
    }

    @SuppressWarnings("unused")
    public boolean canRestart()
    {
        return enabler != null;
    }

    @Override
    public void restart()
    {
        shutdown();

        if (enabler == null || enabler.get())
            register();
    }

    @Override
    public void shutdown()
    {
        unregister();
    }
}
