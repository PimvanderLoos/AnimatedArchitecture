package nl.pim16aap2.bigdoors.events.dooraction;

import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;

/**
 * Represents the different kinds of actions that are applicable to a door.
 *
 * @author Pim
 */
public enum DoorActionType
{
    /**
     * Open a {@link AbstractDoorBase} if it is currently open, otherwise close it.
     */
    TOGGLE,

    /**
     * Open a {@link AbstractDoorBase}, but only if it is currently closed.
     */
    OPEN,

    /**
     * Close a {@link AbstractDoorBase}, but only if it is currently opened.
     */
    CLOSE
}
