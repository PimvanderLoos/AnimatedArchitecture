package nl.pim16aap2.bigdoors.api.factories;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEvent;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEventToggleEnd;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEventTogglePrepare;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEventToggleStart;
import nl.pim16aap2.bigdoors.util.vector.IVector3DiConst;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a class that can create {@link IDoorEvent}s.
 *
 * @author Pim
 */
public interface IDoorActionEventFactory
{
    /**
     * Constructs a {@link IDoorEventTogglePrepare}.
     *
     * @param door          The door.
     * @param cause         What caused the action.
     * @param actionType    The type of action.
     * @param responsible   Who is responsible for this door. Either the player who directly toggled it (via a command
     *                      or the GUI), or the original creator when this data is not available.
     * @param time          The number of seconds the door will take to open. Note that there are other factors that
     *                      affect the total time as well.
     * @param skipAnimation If true, the door will skip the animation and open instantly.
     * @param newMinimum    The new minimum coordinates of the door after the toggle.
     * @param newMaximum    The new maximum coordinates of the door after the toggle.
     */
    @NotNull
    IDoorEventTogglePrepare createPrepareEvent(final @NotNull AbstractDoorBase door,
                                               final @NotNull DoorActionCause cause,
                                               final @NotNull DoorActionType actionType,
                                               final @NotNull IPPlayer responsible, final double time,
                                               final boolean skipAnimation,
                                               final @NotNull IVector3DiConst newMinimum,
                                               final @NotNull IVector3DiConst newMaximum);

    /**
     * Constructs a {@link IDoorEventToggleStart}.
     *
     * @param door          The door.
     * @param cause         What caused the action.
     * @param actionType    The type of action.
     * @param responsible   Who is responsible for this door. Either the player who directly toggled it (via a command
     *                      or the GUI), or the original creator when this data is not available.
     * @param time          The number of seconds the door will take to open. Note that there are other factors that
     *                      affect the total time as well.
     * @param skipAnimation If true, the door will skip the animation and open instantly.
     * @param newMinimum    The new minimum coordinates of the door after the toggle.
     * @param newMaximum    The new maximum coordinates of the door after the toggle.
     */
    @NotNull
    IDoorEventToggleStart createStartEvent(final @NotNull AbstractDoorBase door,
                                           final @NotNull DoorActionCause cause,
                                           final @NotNull DoorActionType actionType,
                                           final @NotNull IPPlayer responsible, final double time,
                                           final boolean skipAnimation,
                                           final @NotNull IVector3DiConst newMinimum,
                                           final @NotNull IVector3DiConst newMaximum);

    /**
     * Constructs a {@link IDoorEventToggleEnd}.
     *
     * @param door          The door.
     * @param cause         What caused the action.
     * @param actionType    The type of action.
     * @param responsible   Who is responsible for this door. Either the player who directly toggled it (via a command
     *                      or the GUI), or the original creator when this data is not available.
     * @param time          The number of seconds the door will take to open. Note that there are other factors that
     *                      affect the total time as well.
     * @param skipAnimation If true, the door will skip the animation and open instantly.
     */
    @NotNull
    IDoorEventToggleEnd createEndEvent(final @NotNull AbstractDoorBase door,
                                       final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                                       final @NotNull IPPlayer responsible, final double time,
                                       final boolean skipAnimation);
}
