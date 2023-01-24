package nl.pim16aap2.bigdoors.events.movableaction;

import nl.pim16aap2.bigdoors.movable.AbstractMovable;

/**
 * Represents the different kinds of actions that are applicable to a movable.
 *
 * @author Pim
 */
public enum MovableActionType
{
    /**
     * Open a {@link AbstractMovable} if it is currently open, otherwise close it.
     */
    TOGGLE,

    /**
     * Open a {@link AbstractMovable}, but only if it is currently closed.
     */
    OPEN,

    /**
     * Close a {@link AbstractMovable}, but only if it is currently opened.
     */
    CLOSE
}
