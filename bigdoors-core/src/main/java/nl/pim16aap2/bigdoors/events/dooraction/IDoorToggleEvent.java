package nl.pim16aap2.bigdoors.events.dooraction;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.DoorSnapshot;
import nl.pim16aap2.bigdoors.events.IBigDoorsEvent;

public interface IDoorToggleEvent extends IBigDoorsEvent
{
    /**
     * Before the door is animated, a snapshot of the data is created which is then used for all calculations.
     * <p>
     * The snapshots are read-only, so this snapshot contains the old data of the door before the animation was
     * applied.
     *
     * @return A snapshot of the door created at the time the toggle action was requested.
     */
    DoorSnapshot getDoorSnapshot();

    /**
     * Gets what caused the door action request to be created.
     *
     * @return The cause of the door action request.
     */
    DoorActionCause getCause();

    /**
     * Gets the UUID of the player responsible for this door action. This either means the player who directly requested
     * this action or, if it was requested indirectly (e.g. via redstone), the prime owner of the door. Therefore, this
     * player might not be online.
     *
     * @return The player that is responsible for this event.
     */
    IPPlayer getResponsible();

    /**
     * Gets the type of the requested action.
     *
     * @return The type of the requested action.
     */
    DoorActionType getActionType();

    /**
     * Checks if the event requested the door to skip its animation and open instantly.
     *
     * @return True if the event requested the door to skip its animation and open instantly.
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

