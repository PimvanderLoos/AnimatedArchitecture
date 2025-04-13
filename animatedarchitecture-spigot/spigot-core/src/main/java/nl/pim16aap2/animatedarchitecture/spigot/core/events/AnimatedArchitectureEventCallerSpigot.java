package nl.pim16aap2.animatedarchitecture.spigot.core.events;

import com.google.common.flogger.StackSize;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.events.IAnimatedArchitectureEvent;
import nl.pim16aap2.animatedarchitecture.core.events.IAnimatedArchitectureEventCaller;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Represents an implementation of {@link IAnimatedArchitectureEventCaller} for the Spigot platform.
 */
@Singleton
@Flogger
public class AnimatedArchitectureEventCallerSpigot implements IAnimatedArchitectureEventCaller
{
    private final IExecutor executor;
    private final JavaPlugin plugin;

    @Inject
    public AnimatedArchitectureEventCallerSpigot(IExecutor executor, JavaPlugin plugin)
    {
        this.executor = executor;
        this.plugin = plugin;
    }

    @Override
    public <T extends IAnimatedArchitectureEvent> void callAnimatedArchitectureEvent(T animatedArchitectureEvent)
    {
        if (!(animatedArchitectureEvent instanceof AnimatedArchitectureSpigotEvent animatedArchitectureSpigotEvent))
        {
            log.atSevere().withStackTrace(StackSize.FULL).log(
                "Event '%s', is not a Spigot event, but it was called on the Spigot platform!",
                animatedArchitectureEvent.getEventName()
            );
            return;
        }

        if (!plugin.isEnabled())
        {
            log.atFine().log(
                "Is the server shutting down? Tried to call event while disabled: %s",
                animatedArchitectureEvent
            );
            return;
        }

        // Async events can only be called asynchronously and Sync events can only be called from the main thread.
        final boolean isMainThread = executor.isMainThread();
        if (isMainThread && animatedArchitectureEvent.isAsynchronous())
            executor.runAsync(
                () -> Bukkit.getPluginManager().callEvent(animatedArchitectureSpigotEvent)
            );
        else if (!isMainThread && !animatedArchitectureEvent.isAsynchronous())
            executor.runSync(
                () -> Bukkit.getPluginManager().callEvent(animatedArchitectureSpigotEvent)
            );
        else
            Bukkit.getPluginManager().callEvent(animatedArchitectureSpigotEvent);
    }
}
