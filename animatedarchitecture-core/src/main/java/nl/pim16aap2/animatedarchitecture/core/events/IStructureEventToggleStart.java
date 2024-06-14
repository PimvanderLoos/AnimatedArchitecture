package nl.pim16aap2.animatedarchitecture.core.events;

import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;

/**
 * Represents a toggle action that will be applied to a structure.If you want to cancel the action, use
 * {@link IStructureEventTogglePrepare} instead.
 */
public interface IStructureEventToggleStart extends IStructureToggleEvent
{
    /**
     * Gets the structure that is the subject of this event.
     * <p>
     * Note that this structure is not the same as the structure in the snapshot. The snapshot contains the old data of
     * the structure before the animation was applied. The structure, however, may already have been modified.
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
