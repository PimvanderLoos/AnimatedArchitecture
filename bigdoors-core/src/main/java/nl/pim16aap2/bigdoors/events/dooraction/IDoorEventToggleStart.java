package nl.pim16aap2.bigdoors.events.dooraction;

import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.util.Cuboid;

/**
 * Represents a toggle action that will be applied to a door.If you want to cancel the action, use
 * {@link IDoorEventTogglePrepare} instead.
 *
 * @author Pim
 */
public interface IDoorEventToggleStart extends IDoorToggleEvent
{
    /**
     * Gets the door that is the subject of this event.
     *
     * @return The door.
     */
    AbstractDoor getDoor();

    /**
     * Gets the new coordinates of the door after the toggle.
     *
     * @return The new coordinates of the door after the toggle.
     */
    Cuboid getNewCuboid();
}
