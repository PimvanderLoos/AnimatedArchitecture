package nl.pim16aap2.animatedarchitecture.spigot.core.implementations;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatformProvider;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebugReporter;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
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
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IAnimatedArchitectureSpigotSubPlatform;
import nl.pim16aap2.util.SafeStringBuilder;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Flogger
public class DebugReporterSpigot extends DebugReporter
{
    private final AnimatedArchitecturePlugin animatedArchitecturePlugin;
    private final @Nullable IAnimatedArchitectureSpigotSubPlatform subPlatform;

    @Inject
    public DebugReporterSpigot(
        AnimatedArchitecturePlugin animatedArchitecturePlugin, IAnimatedArchitecturePlatformProvider platformProvider,
        @Nullable IAnimatedArchitectureSpigotSubPlatform subPlatform, DebuggableRegistry debuggableRegistry)
    {
        super(debuggableRegistry, platformProvider);
        this.animatedArchitecturePlugin = animatedArchitecturePlugin;
        this.subPlatform = subPlatform;
    }

    @Override
    protected String getAdditionalDebugReport()
    {
        return new SafeStringBuilder()
            .append("Server version: ").append(() -> Bukkit.getServer().getVersion())
            .append('\n')
            .append("SpigotSubPlatform: ").append(() -> subPlatform == null ? "null" : subPlatform.getClass().getName())
            .append('\n')
            .append("Registered addons: ").append(animatedArchitecturePlugin::getRegisteredPlugins)
            .append('\n')

//            // TODO: Implement this:
//            .append("Enabled protection hooks: ")
//            .append(getAllProtectionHooksOrSomething())

            .append("EventListeners:\n").append(getListeners(StructurePrepareAddOwnerEvent.class,
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

    private String getListeners(Class<?>... classes)
    {
        final SafeStringBuilder sb = new SafeStringBuilder();
        for (final Class<?> clz : classes)
        {
            if (!(AnimatedArchitectureSpigotEvent.class.isAssignableFrom(clz)))
            {
                sb.append("ERROR: ").append(clz::getName).append('\n');
                continue;
            }
            try
            {
                final var handlerListMethod = clz.getDeclaredField("HANDLERS_LIST");
                handlerListMethod.setAccessible(true);
                final var handlers = (HandlerList) handlerListMethod.get(null);
                sb.append("    ").append(clz::getSimpleName).append(": ")
                  .append(() -> Util.toString(handlers.getRegisteredListeners(),
                                              DebugReporterSpigot::formatRegisteredListener))
                  .append('\n');
            }
            catch (Exception e)
            {
                log.atSevere().withCause(e).log("Failed to find MethodHandle for handlers!");
                sb.append("ERROR: ").append(clz::getName).append('\n');
            }
        }

        return sb.toString();
    }

    private static String formatRegisteredListener(RegisteredListener listener)
    {
        return String.format("{%s: %s (%s)}", listener.getPlugin(), listener.getListener(), listener.getPriority());
    }
}
