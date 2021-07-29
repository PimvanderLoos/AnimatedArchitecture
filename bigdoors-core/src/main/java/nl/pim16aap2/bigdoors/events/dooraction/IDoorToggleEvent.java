package nl.pim16aap2.bigdoors.events.dooraction;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.events.IBigDoorsEvent;
import org.jetbrains.annotations.NotNull;

public interface IDoorToggleEvent extends IBigDoorsEvent
{
    /**
     * Gets the door that is the subject of this event.
     *
     * @return The door.
     */
    @NotNull AbstractDoor getDoor();

    /**
     * Gets what caused the door action request to be created.
     *
     * @return The cause of the door action request.
     */
    @NotNull DoorActionCause getCause();

    /**
     * Gets the UUID of the player responsible for this door action. This either means the player who directly requested
     * this action or, if it was requested indirectly (e.g. via redstone), the prime owner of the door. Therefore, this
     * player might not be online.
     *
     * @return The player that is responsible for this event.
     */
    @NotNull IPPlayer getResponsible();

    /**
     * Gets the type of the requested action.
     *
     * @return The type of the requested action.
     */
    @NotNull DoorActionType getActionType();

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

