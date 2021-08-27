package nl.pim16aap2.bigdoors.spigot.util;

import lombok.AllArgsConstructor;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.DebugReporter;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.events.BigDoorsSpigotEvent;
import nl.pim16aap2.bigdoors.spigot.events.DoorCreatedEvent;
import nl.pim16aap2.bigdoors.spigot.events.DoorPrepareAddOwnerEvent;
import nl.pim16aap2.bigdoors.spigot.events.DoorPrepareCreateEvent;
import nl.pim16aap2.bigdoors.spigot.events.DoorPrepareDeleteEvent;
import nl.pim16aap2.bigdoors.spigot.events.DoorPrepareLockChangeEvent;
import nl.pim16aap2.bigdoors.spigot.events.DoorPrepareRemoveOwnerEvent;
import nl.pim16aap2.bigdoors.spigot.events.dooraction.DoorEventToggleEnd;
import nl.pim16aap2.bigdoors.spigot.events.dooraction.DoorEventTogglePrepare;
import nl.pim16aap2.bigdoors.spigot.events.dooraction.DoorEventToggleStart;
import nl.pim16aap2.bigdoors.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.Nullable;

@AllArgsConstructor
public class DebugReporterSpigot extends DebugReporter
{
    private final BigDoorsSpigot plugin;

    @Override
    public String getDump()
    {
        final StringBuilder sb = new StringBuilder(super.getDump());
        sb.append("BigDoors version: ").append(plugin.getDescription().getVersion()).append("\n");
        sb.append("Server version: ").append(Bukkit.getServer().getVersion()).append("\n");

        sb.append("Registered door types: ")
          .append(Util.toString(BigDoors.get().getDoorTypeManager().getRegisteredDoorTypes()))
          .append("\n");

        sb.append("Enabled door types:    ")
          .append(Util.toString(BigDoors.get().getDoorTypeManager().getEnabledDoorTypes()))
          .append("\n");

        final @Nullable var platform = plugin.getPlatformManagerSpigot().getSpigotPlatform();
        sb.append("SpigotPlatform: ").append(platform == null ? "NULL" : platform.getClass().getName()).append("\n");

        // TODO: Implement this:
//        sb.append("Enabled protection hooks: ")
//          .append(getAllProtectionHooksOrSomething())

        sb.append("EventListeners:\n").append(
            getListeners(DoorPrepareAddOwnerEvent.class, DoorPrepareCreateEvent.class, DoorPrepareDeleteEvent.class,
                         DoorPrepareLockChangeEvent.class, DoorPrepareRemoveOwnerEvent.class, DoorCreatedEvent.class,
                         DoorEventToggleEnd.class, DoorEventTogglePrepare.class, DoorEventToggleStart.class));

        sb.append("Config: ").append(BigDoorsSpigot.get().getConfigLoader()).append("\n");

        return sb.toString();
    }

    private String getListeners(Class<?>... classes)
    {
        final StringBuilder sb = new StringBuilder();
        for (Class<?> clz : classes)
        {
            if (!(BigDoorsSpigotEvent.class.isAssignableFrom(clz)))
            {
                sb.append("ERROR: ").append(clz.getName()).append("\n");
                continue;
            }
            try
            {
                final var handlerListMethod = clz.getDeclaredField("HANDLERS_LIST");
                handlerListMethod.setAccessible(true);
                final var handlers = (HandlerList) handlerListMethod.get(null);
                sb.append("    ").append(clz.getSimpleName()).append(": ")
                  .append(Util.toString(handlers.getRegisteredListeners(),
                                        DebugReporterSpigot::formatRegisteredListener))
                  .append("\n");
            }
            catch (Exception e)
            {
                BigDoors.get().getPLogger()
                        .logThrowable(new RuntimeException("Failed to find MethodHandle for handlers!", e));
                sb.append("ERROR: ").append(clz.getName()).append("\n");
            }
        }

        return sb.toString();
    }

    private static String formatRegisteredListener(RegisteredListener listener)
    {
        return String.format("{%s: %s (%s)}",
                             listener.getPlugin(), listener.getListener(), listener.getPriority());
    }
}
