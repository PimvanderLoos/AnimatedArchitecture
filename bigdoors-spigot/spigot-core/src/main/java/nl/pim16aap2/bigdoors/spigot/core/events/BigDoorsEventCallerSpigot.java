package nl.pim16aap2.bigdoors.spigot.core.events;

import com.google.common.flogger.StackSize;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.api.IPExecutor;
import nl.pim16aap2.bigdoors.core.events.IBigDoorsEvent;
import nl.pim16aap2.bigdoors.core.events.IBigDoorsEventCaller;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Represents an implementation of {@link IBigDoorsEventCaller} for the Spigot platform.
 *
 * @author Pim
 */
@Singleton
@Flogger
public class BigDoorsEventCallerSpigot implements IBigDoorsEventCaller
{
    private final IPExecutor executor;
    private final JavaPlugin plugin;

    @Inject
    public BigDoorsEventCallerSpigot(IPExecutor executor, JavaPlugin plugin)
    {
        this.executor = executor;
        this.plugin = plugin;
    }

    @Override
    public <T extends IBigDoorsEvent> void callBigDoorsEvent(T bigDoorsEvent)
    {
        if (!(bigDoorsEvent instanceof BigDoorsSpigotEvent))
        {
            log.atSevere().withStackTrace(StackSize.FULL)
               .log("Event '%s', is not a Spigot event, but it was called on the Spigot platform!",
                    bigDoorsEvent.getEventName());
            return;
        }

        if (!plugin.isEnabled())
        {
            log.atFine().log("Is the server shutting down? Tried to call event while disabled: %s", bigDoorsEvent);
            return;
        }

        // Async events can only be called asynchronously and Sync events can only be called from the main thread.
        final boolean isMainThread = executor.isMainThread();
        if (isMainThread && bigDoorsEvent.isAsynchronous())
            executor.runAsync(() -> Bukkit.getPluginManager().callEvent((BigDoorsSpigotEvent) bigDoorsEvent));
        else if ((!isMainThread) && (!bigDoorsEvent.isAsynchronous()))
            executor.runSync(() -> Bukkit.getPluginManager().callEvent((BigDoorsSpigotEvent) bigDoorsEvent));
        else
            Bukkit.getPluginManager().callEvent((BigDoorsSpigotEvent) bigDoorsEvent);
    }
}
