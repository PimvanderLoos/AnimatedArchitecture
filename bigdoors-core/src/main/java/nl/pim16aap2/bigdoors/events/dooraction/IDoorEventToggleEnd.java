package nl.pim16aap2.bigdoors.events.dooraction;

import nl.pim16aap2.bigdoors.doors.AbstractDoor;

/**
 * Represents a toggle action that has been applied to a door.
 *
 * @author Pim
 */
public interface IDoorEventToggleEnd extends IDoorToggleEvent
{
    /**
     * Gets the door that is the subject of this event.
     *
     * @return The door.
     */
    AbstractDoor getDoor();
}
