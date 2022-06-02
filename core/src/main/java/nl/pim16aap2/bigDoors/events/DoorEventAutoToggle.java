package nl.pim16aap2.bigDoors.events;

import nl.pim16aap2.bigDoors.Door;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Called when an auto toggle is about to be scheduled.
 */
public class DoorEventAutoToggle extends DoorEvent implements Cancellable
{
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    private boolean isCancelled = false;

    public DoorEventAutoToggle(Door door)
    {
        super(door);
    }

    @Override
    public boolean isCancelled()
    {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean val)
    {
        isCancelled = val;
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
