package nl.pim16aap2.bigdoors.events.dooraction;

import nl.pim16aap2.bigdoors.util.vector.IVector3DiConst;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a toggle action that will be applied to a door.If you want to cancel the action, use {@link
 * IDoorEventTogglePrepare} instead.
 *
 * @author Pim
 */
public interface IDoorEventToggleStart extends IDoorEvent
{
    /**
     * Gets the new minimum coordinates of the door after the toggle.
     *
     * @return The new minimum coordinates of the door after the toggle.
     */
    @NotNull
    IVector3DiConst getNewMinimum();

    /**
     * Gets the new maximum coordinates of the door after the toggle.
     *
     * @return The new maximum coordinates of the door after the toggle.
     */
    @NotNull
    IVector3DiConst getNewMaximum();
}
