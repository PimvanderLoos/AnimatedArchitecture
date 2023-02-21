package nl.pim16aap2.animatedarchitecture.core.events;

import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;

/**
 * Represents a toggle action that has been applied to a structure.
 *
 * @author Pim
 */
public interface IStructureEventToggleEnd extends IStructureToggleEvent
{
    /**
     * Gets the structure that is the subject of this event.
     *
     * @return The structure.
     */
    AbstractStructure getStructure();
}
