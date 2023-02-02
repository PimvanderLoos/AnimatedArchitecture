package nl.pim16aap2.bigdoors.events.structureaction;

import nl.pim16aap2.bigdoors.structures.AbstractStructure;

/**
 * Represents a toggle action that has been applied to a structure.
 *
 * @author Pim
 */
public interface IStructureEventToggleEnd extends nl.pim16aap2.bigdoors.events.structureaction.IStructureToggleEvent
{
    /**
     * Gets the structure that is the subject of this event.
     *
     * @return The structure.
     */
    AbstractStructure getStructure();
}
