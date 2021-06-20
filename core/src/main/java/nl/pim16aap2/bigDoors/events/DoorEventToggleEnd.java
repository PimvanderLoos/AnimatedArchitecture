/**
 *
 */
package nl.pim16aap2.bigDoors.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import nl.pim16aap2.bigDoors.Door;

/**
 * Represents the end of a {@link DoorEventToggle}. It is fired after the actual
 * toggle takes place and is therefore NOT {@link Cancellable}.
 *
 * @author Pim
 */
public class DoorEventToggleEnd extends DoorEventToggle
{
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    public DoorEventToggleEnd(Door door, ToggleType toggleType, final boolean instantOpen)
    {
        super(door, toggleType, instantOpen);
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
