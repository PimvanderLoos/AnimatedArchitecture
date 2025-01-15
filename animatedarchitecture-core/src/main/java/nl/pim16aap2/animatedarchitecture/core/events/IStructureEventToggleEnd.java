package nl.pim16aap2.animatedarchitecture.core.events;

import nl.pim16aap2.animatedarchitecture.core.structures.Structure;

/**
 * Represents a toggle action that has been applied to a structure.
 */
public interface IStructureEventToggleEnd extends IStructureToggleEvent
{
    /**
     * Gets the structure that is the subject of this event.
     *
     * @return The structure.
     */
    Structure getStructure();
}
