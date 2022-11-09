package nl.pim16aap2.bigDoors.events;

import nl.pim16aap2.bigDoors.Door;
import org.bukkit.event.HandlerList;

/**
 * Called after a door is deleted.
 */
public class DoorDeleteEvent extends DoorEvent
{
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    public DoorDeleteEvent(Door door)
    {
        super(door);
    }

    @Override
    public HandlerList getHandlers()
    {
        return HANDLERS_LIST;
    }

    /**
     * Static accessor to {@link #HANDLERS_LIST}. See {@link #getHandlers()}.
     *
     * @return {@link #HANDLERS_LIST}.
     */
    public static HandlerList getHandlerList()
    {
        return HANDLERS_LIST;
    }
}
