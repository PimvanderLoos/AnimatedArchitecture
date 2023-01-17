package nl.pim16aap2.bigdoors.spigot.util;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatformProvider;
import nl.pim16aap2.bigdoors.api.debugging.DebugReporter;
import nl.pim16aap2.bigdoors.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.spigot.BigDoorsPlugin;
import nl.pim16aap2.bigdoors.spigot.events.BigDoorsSpigotEvent;
import nl.pim16aap2.bigdoors.spigot.events.MovableCreatedEvent;
import nl.pim16aap2.bigdoors.spigot.events.MovablePrepareAddOwnerEvent;
import nl.pim16aap2.bigdoors.spigot.events.MovablePrepareCreateEvent;
import nl.pim16aap2.bigdoors.spigot.events.MovablePrepareDeleteEvent;
import nl.pim16aap2.bigdoors.spigot.events.MovablePrepareLockChangeEvent;
import nl.pim16aap2.bigdoors.spigot.events.MovablePrepareRemoveOwnerEvent;
import nl.pim16aap2.bigdoors.spigot.events.dooraction.MovableEventToggleEnd;
import nl.pim16aap2.bigdoors.spigot.events.dooraction.MovableEventTogglePrepare;
import nl.pim16aap2.bigdoors.spigot.events.dooraction.MovableEventToggleStart;
import nl.pim16aap2.bigdoors.spigot.util.api.IBigDoorsSpigotSubPlatform;
import nl.pim16aap2.bigdoors.util.Util;
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
    private final BigDoorsPlugin bigDoorsPlugin;
    private final @Nullable IBigDoorsSpigotSubPlatform subPlatform;

    @Inject
    public DebugReporterSpigot(
        BigDoorsPlugin bigDoorsPlugin, IBigDoorsPlatformProvider platformProvider,
        @Nullable IBigDoorsSpigotSubPlatform subPlatform, DebuggableRegistry debuggableRegistry)
    {
        super(debuggableRegistry, platformProvider);
        this.bigDoorsPlugin = bigDoorsPlugin;
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
            .append("Registered addons: ").append(bigDoorsPlugin::getRegisteredPlugins)
            .append('\n')

//            // TODO: Implement this:
//            .append("Enabled protection hooks: ")
//            .append(getAllProtectionHooksOrSomething())

            .append("EventListeners:\n").append(getListeners(MovablePrepareAddOwnerEvent.class,
                                                             MovablePrepareCreateEvent.class,
                                                             MovablePrepareDeleteEvent.class,
                                                             MovablePrepareLockChangeEvent.class,
                                                             MovablePrepareRemoveOwnerEvent.class,
                                                             MovableCreatedEvent.class,
                                                             MovableEventToggleEnd.class,
                                                             MovableEventTogglePrepare.class,
                                                             MovableEventToggleStart.class))
            .append('\n')
            .toString();
    }

    private String getListeners(Class<?>... classes)
    {
        final SafeStringBuilder sb = new SafeStringBuilder();
        for (final Class<?> clz : classes)
        {
            if (!(BigDoorsSpigotEvent.class.isAssignableFrom(clz)))
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
