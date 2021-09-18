package nl.pim16aap2.bigdoors.spigot.util;

import nl.pim16aap2.bigdoors.api.DebugReporter;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import nl.pim16aap2.bigdoors.spigot.BigDoorsPlugin;
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
import nl.pim16aap2.bigdoors.spigot.util.api.IBigDoorsSpigotSubPlatform;
import nl.pim16aap2.bigdoors.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DebugReporterSpigot extends DebugReporter
{
    private final BigDoorsPlugin bigDoorsPlugin;
    private final IPLogger logger;

    private final @Nullable DoorTypeManager doorTypeManager;
    private final @Nullable IConfigLoader config;
    private final @Nullable IBigDoorsSpigotSubPlatform subPlatform;

    @Inject
    public DebugReporterSpigot(BigDoorsPlugin bigDoorsPlugin, IPLogger logger, @Nullable IBigDoorsPlatform platform,
                               @Nullable DoorTypeManager doorTypeManager, @Nullable IConfigLoader config,
                               @Nullable IBigDoorsSpigotSubPlatform subPlatform)
    {
        super(platform);
        this.bigDoorsPlugin = bigDoorsPlugin;
        this.logger = logger;

        this.doorTypeManager = doorTypeManager;
        this.config = config;
        this.subPlatform = subPlatform;
    }

    @Override
    public String getDump()
    {
        return new StringBuilder(super.getDump())
            // Try to use the platform's version first because that might contain more information (build id etc.)
            // But if that's not available, use the JavaPlugin's version instead.
            .append("BigDoors version: ").append(platform == null ?
                                                 bigDoorsPlugin.getDescription().getVersion() : platform.getVersion())
            .append('\n')
            .append("Server version: ").append(Bukkit.getServer().getVersion())
            .append('\n')
            .append("Registered door types: ")
            .append(Util.toString(doorTypeManager == null ? "" : doorTypeManager.getRegisteredDoorTypes()))
            .append('\n')
            .append("Enabled door types:    ")
            .append(Util.toString(doorTypeManager == null ? "" : doorTypeManager.getEnabledDoorTypes()))
            .append('\n')
            .append("SpigotPlatform: ").append(platform == null ? "null" : platform.getClass().getName())
            .append('\n')
            .append("SpigotSubPlatform: ").append(subPlatform == null ? "null" : subPlatform.getClass().getName())
            .append('\n')
            .append("Registered plugins: ").append(bigDoorsPlugin.getRegisteredPlugins())
            .append('\n')

//            // TODO: Implement this:
//            .append("Enabled protection hooks: ")
//            .append(getAllProtectionHooksOrSomething())

            .append("EventListeners:\n").append(getListeners(DoorPrepareAddOwnerEvent.class,
                                                             DoorPrepareCreateEvent.class,
                                                             DoorPrepareDeleteEvent.class,
                                                             DoorPrepareLockChangeEvent.class,
                                                             DoorPrepareRemoveOwnerEvent.class,
                                                             DoorCreatedEvent.class,
                                                             DoorEventToggleEnd.class,
                                                             DoorEventTogglePrepare.class,
                                                             DoorEventToggleStart.class))
            .append("Config: ").append(config)
            .append('\n')
            .toString();
    }

    private String getListeners(Class<?>... classes)
    {
        final StringBuilder sb = new StringBuilder();
        for (final Class<?> clz : classes)
        {
            if (!(BigDoorsSpigotEvent.class.isAssignableFrom(clz)))
            {
                sb.append("ERROR: ").append(clz.getName()).append('\n');
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
                  .append('\n');
            }
            catch (Exception e)
            {
                logger.logThrowable(new RuntimeException("Failed to find MethodHandle for handlers!", e));
                sb.append("ERROR: ").append(clz.getName()).append('\n');
            }
        }

        return sb.toString();
    }

    private static String formatRegisteredListener(RegisteredListener listener)
    {
        return String.format("{%s: %s (%s)}",
                             listener.getPlugin(), listener.getListener(), listener.getPriority());
    }

    @Override
    public String toString()
    {
        return getDump();
    }
}
