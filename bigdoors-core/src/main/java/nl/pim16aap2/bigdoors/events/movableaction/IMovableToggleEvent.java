package nl.pim16aap2.bigdoors.events.movableaction;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.events.IBigDoorsEvent;
import nl.pim16aap2.bigdoors.movable.MovableSnapshot;

public interface IMovableToggleEvent extends IBigDoorsEvent
{
    /**
     * Before the movable is animated, a snapshot of the data is created which is then used for all calculations.
     * <p>
     * The snapshots are read-only, so this snapshot contains the old data of the movable before the animation was
     * applied.
     *
     * @return A snapshot of the movable created at the time the toggle action was requested.
     */
    MovableSnapshot getSnapshot();

    /**
     * Gets what caused the movable action request to be created.
     *
     * @return The cause of the movable action request.
     */
    MovableActionCause getCause();

    /**
     * Gets the UUID of the player responsible for this movable action. This either means the player who directly
     * requested this action or, if it was requested indirectly (e.g. via redstone), the prime owner of the movable.
     * Therefore, this player might not be online.
     *
     * @return The player that is responsible for this event.
     */
    IPPlayer getResponsible();

    /**
     * Gets the type of the requested action.
     *
     * @return The type of the requested action.
     */
    MovableActionType getActionType();

    /**
     * Checks if the event requested the movable to skip its animation and open instantly.
     *
     * @return True if the event requested the movable to skip its animation and open instantly.
     */
    boolean isAnimationSkipped();

    /**
     * Gets requested duration of the animation. This may differ from the final duration based on other factors (such as
     * speed limits).
     *
     * @return The requested duration of the animation (in seconds).
     */
    double getTime();
}

