package nl.pim16aap2.animatedarchitecture.core.events;

import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;

/**
 * Represents the different kinds of actions that are applicable to a structure.
 *
 * @author Pim
 */
public enum StructureActionType
{
    /**
     * Open a {@link AbstractStructure} if it is currently open, otherwise close it.
     */
    TOGGLE,

    /**
     * Open a {@link AbstractStructure}, but only if it is currently closed.
     */
    OPEN,

    /**
     * Close a {@link AbstractStructure}, but only if it is currently opened.
     */
    CLOSE
}
