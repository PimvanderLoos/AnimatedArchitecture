package nl.pim16aap2.bigdoors.events.dooraction;

import nl.pim16aap2.bigdoors.events.ICancellableBigDoorsEvent;
import nl.pim16aap2.bigdoors.util.Cuboid;

/**
 * Represents a toggle action that might be applied to a door. Note that not cancelling this event does not mean that
 * action is guaranteed to take place, as other factors might prevent that from happening (e.g. the door being locked).
 * <p>
 * If you are looking for a guaranteed action, use {@link IDoorEventToggleStart} instead.
 *
 * @author Pim
 */
public interface IDoorEventTogglePrepare extends IDoorToggleEvent, ICancellableBigDoorsEvent
{
    /**
     * Gets the new coordinates of the door after the toggle.
     *
     * @return The new coordinates of the door after the toggle.
     */
    Cuboid getNewCuboid();
}
