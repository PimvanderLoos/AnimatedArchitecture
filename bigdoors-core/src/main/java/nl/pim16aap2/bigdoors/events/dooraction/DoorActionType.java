package nl.pim16aap2.bigdoors.events.dooraction;

import nl.pim16aap2.bigdoors.doors.DoorBase;

/**
 * Represents the different kinds of actions that are applicable to a door.
 *
 * @author Pim
 */
public enum DoorActionType
{
    /**
     * Open a {@link DoorBase} if it is currently open, otherwise close it.
     */
    TOGGLE,

    /**
     * Open a {@link DoorBase}, but only if it is currently closed.
     */
    OPEN,

    /**
     * Close a {@link DoorBase}, but only if it is currently opened.
     */
    CLOSE
}
