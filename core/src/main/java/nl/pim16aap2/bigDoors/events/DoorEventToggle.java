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
//    private final double speed;
    private final boolean instantOpen;

    DoorEventToggle(final Door door, final ToggleType toggleType, final boolean instantOpen)
    {
        super(door);
        this.toggleType = toggleType;
        this.instantOpen = instantOpen;
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
     * Checks if the door will be opened instantaneously (i.e. skipping the
     * animation).
     *
     * @return True if the door will skip the animation and be opened
     *         instantaneously.
     */
    public boolean instantOpen()
    {
        return instantOpen;
    }

//    /**
//     * Gets the number of seconds in which the door will <b>TRY</b> to open.
//     * <p>
//     * Note that this is very unlikely to be the actual time of the animation
//     * because:
//     * <p>
//     * <ul>
//     * <li>There are speed limits that have not yet been taken into account. If the
//     * animation duration is too low for the dimensions and type of this specific
//     * door, the animation duration will be longer than the value returned here.
//     * <li>Some aspects of the animation are not taken into account. For example the
//     * ending phase (where the animated blocks are standing still in their final
//     * position for a little while) or the cooldown are not part of this value.
//     * </ul>
//     *
//     * @return The number of seconds in which the door will <b>TRY</b> to open.
//     */
//    public double getAnimationDuration()
//    {
//        return speed;
//    }

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
