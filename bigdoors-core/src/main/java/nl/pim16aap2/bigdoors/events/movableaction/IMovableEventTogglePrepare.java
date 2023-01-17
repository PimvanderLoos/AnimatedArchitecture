package nl.pim16aap2.bigdoors.events.movableaction;

import nl.pim16aap2.bigdoors.events.ICancellableBigDoorsEvent;
import nl.pim16aap2.bigdoors.util.Cuboid;

/**
 * Represents a toggle action that might be applied to a movable. Note that not cancelling this event does not mean that
 * action is guaranteed to take place, as other factors might prevent that from happening (e.g. the movable being
 * locked).
 * <p>
 * If you are looking for a guaranteed action, use {@link IMovableEventToggleStart} instead.
 *
 * @author Pim
 */
public interface IMovableEventTogglePrepare extends IMovableToggleEvent, ICancellableBigDoorsEvent
{
    /**
     * Gets the new coordinates of the movable after the toggle.
     *
     * @return The new coordinates of the movable after the toggle.
     */
    Cuboid getNewCuboid();
}
