package nl.pim16aap2.bigdoors.events.dooraction;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.util.CuboidConst;

/**
 * Represents a toggle action that will be applied to a door.If you want to cancel the action, use {@link
 * IDoorEventTogglePrepare} instead.
 *
 * @author Pim
 */
public interface IDoorEventToggleStart extends IDoorEvent
{
    /**
     * Gets the new coordinates of the door after the toggle.
     *
     * @return The new coordinates of the door after the toggle.
     */
    @NonNull CuboidConst getNewCuboid();
}
