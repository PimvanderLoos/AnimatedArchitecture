package nl.pim16aap2.animatedarchitecture.core.events;

import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;

/**
 * Represents an event where a structure is toggled.
 * <p>
 * The subclasses of this class define the different stages of the toggle event.
 * <p>
 * Preparation stages can be cancelled, while the start and end stages cannot.
 */
public interface IStructureToggleEvent extends IAnimatedArchitectureEvent
{
    /**
     * Before the structure is animated, a snapshot of the data is created which is then used for all calculations.
     * <p>
     * The snapshots are read-only, so this snapshot contains the old data of the structure before the animation was
     * applied.
     *
     * @return A snapshot of the structure created at the time the toggle action was requested.
     */
    StructureSnapshot getSnapshot();

    /**
     * Gets what caused the structure action request to be created.
     *
     * @return The cause of the structure action request.
     */
    StructureActionCause getCause();

    /**
     * Gets the UUID of the player responsible for this structure action. This either means the player who directly
     * requested this action or, if it was requested indirectly (e.g. via redstone), the prime owner of the structure.
     * Therefore, this player might not be online.
     *
     * @return The player that is responsible for this event.
     */
    IPlayer getResponsible();

    /**
     * Gets the type of the requested action.
     *
     * @return The type of the requested action.
     */
    StructureActionType getActionType();

    /**
     * Checks if the event requested the structure to skip its animation and open instantly.
     *
     * @return True if the event requested the structure to skip its animation and open instantly.
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

