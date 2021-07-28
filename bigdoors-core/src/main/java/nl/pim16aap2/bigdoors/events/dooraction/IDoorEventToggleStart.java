package nl.pim16aap2.bigdoors.events.dooraction;

import nl.pim16aap2.bigdoors.util.Cuboid;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a toggle action that will be applied to a door.If you want to cancel the action, use {@link
 * IDoorEventTogglePrepare} instead.
 *
 * @author Pim
 */
public interface IDoorEventToggleStart extends IDoorToggleEvent
{
    /**
     * Gets the new coordinates of the door after the toggle.
     *
     * @return The new coordinates of the door after the toggle.
     */
    @NotNull Cuboid getNewCuboid();
}
