package nl.pim16aap2.animatedarchitecture.spigot.core.implementations;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatformProvider;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebugReporter;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.util.StringUtil;
import nl.pim16aap2.animatedarchitecture.spigot.core.AnimatedArchitecturePlugin;
import nl.pim16aap2.animatedarchitecture.spigot.core.events.AnimatedArchitectureSpigotEvent;
import nl.pim16aap2.animatedarchitecture.spigot.core.events.StructureCreatedEvent;
import nl.pim16aap2.animatedarchitecture.spigot.core.events.StructureEventToggleEnd;
import nl.pim16aap2.animatedarchitecture.spigot.core.events.StructureEventTogglePrepare;
import nl.pim16aap2.animatedarchitecture.spigot.core.events.StructureEventToggleStart;
import nl.pim16aap2.animatedarchitecture.spigot.core.events.StructurePrepareAddOwnerEvent;
import nl.pim16aap2.animatedarchitecture.spigot.core.events.StructurePrepareCreateEvent;
import nl.pim16aap2.animatedarchitecture.spigot.core.events.StructurePrepareDeleteEvent;
import nl.pim16aap2.animatedarchitecture.spigot.core.events.StructurePrepareLockChangeEvent;
import nl.pim16aap2.animatedarchitecture.spigot.core.events.StructurePrepareRemoveOwnerEvent;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.ISpigotSubPlatform;
import nl.pim16aap2.util.SafeStringBuilder;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.stream.Stream;

/**
 * A {@link DebugReporter} implementation for the Spigot platform.
 * <p>
 * This class provides additional debug information specific to the Spigot platform such as the server version,
 * registered addons, and event listeners.
 */
@Singleton
@Flogger
public class DebugReporterSpigot extends DebugReporter
{
    private final AnimatedArchitecturePlugin animatedArchitecturePlugin;
    private final @Nullable ISpigotSubPlatform subPlatform;

    @Inject
    public DebugReporterSpigot(
        AnimatedArchitecturePlugin animatedArchitecturePlugin,
        IAnimatedArchitecturePlatformProvider platformProvider,
        @Nullable ISpigotSubPlatform subPlatform,
        DebuggableRegistry debuggableRegistry)
    {
        super(debuggableRegistry, platformProvider);
        this.subPlatform = subPlatform;
        this.animatedArchitecturePlugin = animatedArchitecturePlugin;
    }

    @Override
    protected String getAdditionalDebugReport()
    {
        return new SafeStringBuilder()
            .append("Server version: ")
            .append(() -> Bukkit.getServer().getVersion())
            .append('\n')
            .append("Sub-platform: ")
            .append(() -> subPlatform == null ? "null" : subPlatform.getClass().getSimpleName())
            .append('\n')
            .append("Registered addons: ")
            .append(animatedArchitecturePlugin::getRegisteredPlugins)
            .append('\n')
            .append("EventListeners:\n")
            .append(formatListeners(
                StructurePrepareAddOwnerEvent.class,
                StructurePrepareCreateEvent.class,
                StructurePrepareDeleteEvent.class,
                StructurePrepareLockChangeEvent.class,
                StructurePrepareRemoveOwnerEvent.class,
                StructureCreatedEvent.class,
                StructureEventToggleEnd.class,
                StructureEventTogglePrepare.class,
                StructureEventToggleStart.class))
            .append('\n')
            .toString();
    }

    private String formatListeners(Class<?>... classes)
    {
        final SafeStringBuilder stringBuilder = new SafeStringBuilder();
        for (final Class<?> clz : classes)
        {
            if (!AnimatedArchitectureSpigotEvent.class.isAssignableFrom(clz))
            {
                stringBuilder.append("ERROR: ").append(clz::getName).append('\n');
                continue;
            }
            try
            {
                final var handlerListMethod = clz.getDeclaredField("HANDLERS_LIST");
                handlerListMethod.setAccessible(true);
                final var handlers = (HandlerList) handlerListMethod.get(null);
                stringBuilder.append("  ").append(clz::getSimpleName)
                    .append(Stream
                        .of(handlers.getRegisteredListeners())
                        .map(DebugReporterSpigot::formatRegisteredListener)
                        .collect(StringUtil.stringCollector("\n    - ", "[]"))
                    );
            }
            catch (Exception e)
            {
                log.atSevere().withCause(e).log("Failed to find MethodHandle for handlers!");
                stringBuilder.append("ERROR: ").append(clz::getName).append('\n');
            }
        }

        return stringBuilder.toString();
    }

    private static String formatRegisteredListener(RegisteredListener listener)
    {
        return String.format("%s: %s (%s)", listener.getPlugin(), listener.getListener(), listener.getPriority());
    }
}
