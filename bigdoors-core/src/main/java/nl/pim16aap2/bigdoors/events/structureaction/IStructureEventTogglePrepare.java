package nl.pim16aap2.bigdoors.events.structureaction;

import nl.pim16aap2.bigdoors.events.ICancellableBigDoorsEvent;
import nl.pim16aap2.bigdoors.util.Cuboid;

/**
 * Represents a toggle action that might be applied to a structure. Note that not cancelling this event does not mean
 * that action is guaranteed to take place, as other factors might prevent that from happening (e.g. the structure being
 * locked).
 * <p>
 * If you are looking for a guaranteed action, use
 * {@link nl.pim16aap2.bigdoors.events.structureaction.IStructureEventToggleStart} instead.
 *
 * @author Pim
 */
public interface IStructureEventTogglePrepare
    extends nl.pim16aap2.bigdoors.events.structureaction.IStructureToggleEvent, ICancellableBigDoorsEvent
{
    /**
     * Gets the new coordinates of the structure after the toggle.
     *
     * @return The new coordinates of the structure after the toggle.
     */
    Cuboid getNewCuboid();
}
