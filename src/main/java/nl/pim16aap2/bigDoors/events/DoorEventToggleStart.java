/**
 *
 */
package nl.pim16aap2.bigDoors.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import nl.pim16aap2.bigDoors.Door;

/**
 * Represents the start of a {@link DoorEventToggle}. It is fired before the
 * actual toggle takes place and is therefore {@link Cancellable}.
 *
 * @author Pim
 */
public class DoorEventToggleStart extends DoorEventToggle implements Cancellable
{
    private boolean isCancelled = false;
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    public DoorEventToggleStart(Door door, ToggleType toggleType)
    {
        super(door, toggleType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCancelled()
    {
        return isCancelled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCancelled(boolean val)
    {
        isCancelled = val;
    }

    /**
     * {@inheritDoc}
     */
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
