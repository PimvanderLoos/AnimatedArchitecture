package nl.pim16aap2.bigdoors.events.movableaction;

import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.util.Cuboid;

/**
 * Represents a toggle action that will be applied to a movable.If you want to cancel the action, use
 * {@link IMovableEventTogglePrepare} instead.
 *
 * @author Pim
 */
public interface IMovableEventToggleStart extends IMovableToggleEvent
{
    /**
     * Gets the movable that is the subject of this event.
     *
     * @return The movable.
     */
    AbstractMovable getMovable();

    /**
     * Gets the new coordinates of the movable after the toggle.
     *
     * @return The new coordinates of the movable after the toggle.
     */
    Cuboid getNewCuboid();
}
