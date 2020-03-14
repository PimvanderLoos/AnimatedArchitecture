/**
 *
 */
package nl.pim16aap2.bigDoors.events;

import nl.pim16aap2.bigDoors.Door;

/**
 * Represents a toggle event for a door.
 *
 * @author Pim
 */
public abstract class DoorEventToggle extends DoorEvent
{
    private final ToggleType toggleType;

    DoorEventToggle(final Door door, final ToggleType toggleType)
    {
        super(door);
        this.toggleType = toggleType;
    }

    /**
     * Gets the {@link ToggleType} of this event.
     *
     * @return The {@link ToggleType} of this event.
     */
    public ToggleType getToggleType()
    {
        return toggleType;
    }

    /**
     * Represents the different ways a door can be toggled.
     *
     * @author Pim
     */
    public enum ToggleType
    {
        /**
         * The door is being opened.
         */
        OPEN,

        /**
         * The door is being closed.
         */
        CLOSE,

        /**
         * The door's open-status does not change. This applies to doors for which the
         * end- and the start-coordinates are the same.
         */
        STATIC,
    }
}
