package nl.pim16aap2.bigdoors.events.movableaction;

import nl.pim16aap2.bigdoors.movable.AbstractMovable;

/**
 * Represents a toggle action that has been applied to a movable.
 *
 * @author Pim
 */
public interface IMovableEventToggleEnd extends IMovableToggleEvent
{
    /**
     * Gets the movable that is the subject of this event.
     *
     * @return The movable.
     */
    AbstractMovable getMovable();
}
