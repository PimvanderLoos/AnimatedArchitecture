package nl.pim16aap2.bigdoors.spigot.events;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.IDoorCreatedEvent;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the event where a door was created.
 *
 * @author Pim
 */
public class DoorCreatedEvent extends DoorEvent implements IDoorCreatedEvent
{
    private static final @NonNull HandlerList HANDLERS_LIST = new HandlerList();

    public DoorCreatedEvent(final @NonNull AbstractDoorBase door,
                            final @Nullable IPPlayer responsible)
    {
        super(door, responsible);
    }

    @Override
    public @NonNull HandlerList getHandlers()
    {
        return HANDLERS_LIST;
    }

    public static @NonNull HandlerList getHandlerList()
    {
        return HANDLERS_LIST;
    }
}
