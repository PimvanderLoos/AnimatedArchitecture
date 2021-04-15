package nl.pim16aap2.bigdoors.api.factories;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEvent;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEventToggleEnd;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEventTogglePrepare;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEventToggleStart;
import nl.pim16aap2.bigdoors.util.CuboidConst;

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
     * @param newCuboid     The {@link CuboidConst} representing the area the door will take up after the toggle.
     */
    @NonNull IDoorEventTogglePrepare createPrepareEvent(final @NonNull AbstractDoorBase door,
                                                        final @NonNull DoorActionCause cause,
                                                        final @NonNull DoorActionType actionType,
                                                        final @NonNull IPPlayer responsible, final double time,
                                                        final boolean skipAnimation,
                                                        final @NonNull CuboidConst newCuboid);

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
     * @param newCuboid     The {@link CuboidConst} representing the area the door will take up after the toggle.
     */
    @NonNull IDoorEventToggleStart createStartEvent(final @NonNull AbstractDoorBase door,
                                                    final @NonNull DoorActionCause cause,
                                                    final @NonNull DoorActionType actionType,
                                                    final @NonNull IPPlayer responsible, final double time,
                                                    final boolean skipAnimation, final @NonNull CuboidConst newCuboid);

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
    @NonNull IDoorEventToggleEnd createEndEvent(final @NonNull AbstractDoorBase door,
                                                final @NonNull DoorActionCause cause,
                                                final @NonNull DoorActionType actionType,
                                                final @NonNull IPPlayer responsible, final double time,
                                                final boolean skipAnimation);
}
