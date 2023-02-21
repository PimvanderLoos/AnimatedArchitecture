package nl.pim16aap2.animatedarchitecture.core.events;

import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;

/**
 * Represents a toggle action that might be applied to a structure. Note that not cancelling this event does not mean
 * that action is guaranteed to take place, as other factors might prevent that from happening (e.g. the structure being
 * locked).
 * <p>
 * If you are looking for a guaranteed action, use {@link IStructureEventToggleStart} instead.
 *
 * @author Pim
 */
public interface IStructureEventTogglePrepare extends IStructureToggleEvent, ICancellableAnimatedArchitectureEvent
{
    /**
     * Gets the new coordinates of the structure after the toggle.
     *
     * @return The new coordinates of the structure after the toggle.
     */
    Cuboid getNewCuboid();
}
