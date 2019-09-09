package nl.pim16aap2.bigdoors.listeners;

import nl.pim16aap2.bigdoors.doors.DoorOpener;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionEventSpigot;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public final class DoorActionListener implements Listener
{
    @NotNull
    private static final DoorActionListener instance = new DoorActionListener();

    private DoorActionListener()
    {
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDoorAction(final @NotNull DoorActionEventSpigot doorActionEvent)
    {
        DoorOpener.get().animateDoorAsync(doorActionEvent.getFutureDoor(), doorActionEvent.getCause(),
                                          doorActionEvent.getResponsible().orElse(null), doorActionEvent.getTime(),
                                          doorActionEvent.skipAnimation(), doorActionEvent.getActionType());
    }

    /**
     * Gets the instance of the {@link DoorActionListener} if it exists.
     *
     * @return The instance of the {@link DoorActionListener}.
     */
    @NotNull
    public static DoorActionListener get()
    {
        return instance;
    }
}
