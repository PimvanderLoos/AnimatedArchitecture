package nl.pim16aap2.bigdoors.core.events.structureaction;

import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.util.Cuboid;

/**
 * Represents a toggle action that will be applied to a structure.If you want to cancel the action, use
 * {@link IStructureEventTogglePrepare} instead.
 *
 * @author Pim
 */
public interface IStructureEventToggleStart extends IStructureToggleEvent
{
    /**
     * Gets the structure that is the subject of this event.
     *
     * @return The structure.
     */
    AbstractStructure getStructure();

    /**
     * Gets the new coordinates of the structure after the toggle.
     *
     * @return The new coordinates of the structure after the toggle.
     */
    Cuboid getNewCuboid();
}
