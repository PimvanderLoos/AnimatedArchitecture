package nl.pim16aap2.animatedarchitecture.core.events;

import nl.pim16aap2.animatedarchitecture.core.structures.Structure;

/**
 * Represents the different kinds of actions that are applicable to a structure.
 */
public enum StructureActionType
{
    /**
     * Open a {@link Structure} if it is currently open, otherwise close it.
     */
    TOGGLE,

    /**
     * Open a {@link Structure}, but only if it is currently closed.
     */
    OPEN,

    /**
     * Close a {@link Structure}, but only if it is currently opened.
     */
    CLOSE
}
