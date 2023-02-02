package nl.pim16aap2.bigdoors.events.structureaction;

import nl.pim16aap2.bigdoors.structures.AbstractStructure;

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
