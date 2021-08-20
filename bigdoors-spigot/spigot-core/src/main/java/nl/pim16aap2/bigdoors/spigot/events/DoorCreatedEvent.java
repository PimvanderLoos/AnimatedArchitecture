package nl.pim16aap2.bigdoors.spigot.events;

import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.events.IDoorCreatedEvent;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the event where a door was created.
 *
 * @author Pim
 */
@ToString
public class DoorCreatedEvent extends DoorEvent implements IDoorCreatedEvent
{
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    public DoorCreatedEvent(AbstractDoor door, @Nullable IPPlayer responsible)
    {
        super(door, responsible);
    }

    @Override
    public @NotNull HandlerList getHandlers()
    {
        return HANDLERS_LIST;
    }

    @SuppressWarnings("squid:S4144")
    public static HandlerList getHandlerList()
    {
        return HANDLERS_LIST;
    }
}
