package nl.pim16aap2.bigdoors.spigot.events;

import com.google.common.flogger.StackSize;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.events.IBigDoorsEvent;
import nl.pim16aap2.bigdoors.events.IBigDoorsEventCaller;
import org.bukkit.Bukkit;

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

    @Inject
    public BigDoorsEventCallerSpigot(IPExecutor executor)
    {
        this.executor = executor;
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
