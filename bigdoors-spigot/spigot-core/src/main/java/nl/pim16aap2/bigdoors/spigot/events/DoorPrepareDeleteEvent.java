package nl.pim16aap2.bigdoors.spigot.events;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.events.IDoorPrepareDeleteEvent;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the event where a door was deleted.
 *
 * @author Pim
 */
@ToString
public class DoorPrepareDeleteEvent extends DoorEvent implements IDoorPrepareDeleteEvent
{
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    @Getter
    @Setter
    private boolean isCancelled = false;

    public DoorPrepareDeleteEvent(final AbstractDoor door,
                                  final @Nullable IPPlayer responsible)
    {
        super(door, responsible);
    }

    @Override
    public HandlerList getHandlers()
    {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList()
    {
        return HANDLERS_LIST;
    }
}
