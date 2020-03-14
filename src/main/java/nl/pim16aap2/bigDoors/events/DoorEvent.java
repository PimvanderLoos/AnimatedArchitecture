/**
 *
 */
package nl.pim16aap2.bigDoors.events;

import org.bukkit.event.Event;

import nl.pim16aap2.bigDoors.Door;

/**
 * Represents a Door-related event.
 *
 * @author Pim
 */
public abstract class DoorEvent extends Event
{
    final Door door;

    DoorEvent(final Door door)
    {
        this.door = door;
    }

    /**
     * Gets the {@link Door} that is the subject of this event.
     *
     * @return The {@link Door} that is the subject of this event.
     */
    public Door getDoor()
    {
        return door;
    }
}