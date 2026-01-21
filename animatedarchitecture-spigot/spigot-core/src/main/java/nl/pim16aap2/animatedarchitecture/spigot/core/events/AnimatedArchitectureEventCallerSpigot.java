package nl.pim16aap2.animatedarchitecture.spigot.core.events;

import com.google.common.flogger.StackSize;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.events.IAnimatedArchitectureEvent;
import nl.pim16aap2.animatedarchitecture.core.events.IAnimatedArchitectureEventCaller;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Represents an implementation of {@link IAnimatedArchitectureEventCaller} for the Spigot platform.
 */
@Singleton
@CustomLog
@ExtensionMethod(CompletableFutureExtensions.class)
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
            log.atError().withStackTrace(StackSize.FULL).log(
                "Event '%s', is not a Spigot event, but it was called on the Spigot platform!",
                animatedArchitectureEvent.getEventName()
            );
            return;
        }

        if (!plugin.isEnabled())
        {
            log.atDebug().log(
                "Is the server shutting down? Tried to call event while disabled: %s",
                animatedArchitectureEvent
            );
            return;
        }

        // Async events can only be called asynchronously and Sync events can only be called from the main thread.
        final boolean isMainThread = executor.isMainThread();
        if (isMainThread && animatedArchitectureEvent.isAsynchronous())
        {
            executor.runAsync(() -> Bukkit.getPluginManager().callEvent(animatedArchitectureSpigotEvent))
                .handleExceptional(ex -> log.atError().withCause(ex).log(
                    "Failed to call async event %s",
                    animatedArchitectureEvent
                ));
        }
        else if (!isMainThread && !animatedArchitectureEvent.isAsynchronous())
        {
            executor.runSync(() -> Bukkit.getPluginManager().callEvent(animatedArchitectureSpigotEvent));
        }
        else
        {
            Bukkit.getPluginManager().callEvent(animatedArchitectureSpigotEvent);
        }
    }
}
