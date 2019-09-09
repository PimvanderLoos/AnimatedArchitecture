package nl.pim16aap2.bigdoors.api.factories;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a class that can create {@link IDoorActionEvent}s.
 *
 * @author Pim
 */
public interface IDoorActionEventFactory
{
    /**
     * Constructs a door action event.
     *
     * @param futureDoor    The door.
     * @param cause         What caused the action.
     * @param actionType    The type of action.
     * @param responsible   Who is responsible for this door. If null, the door's owner will be used.
     * @param time          The number of seconds the door will take to open. Note that there are other factors that
     *                      affect the total time as well.
     * @param skipAnimation If true, the door will skip the animation and open instantly.
     */
    @NotNull
    IDoorActionEvent create(final @NotNull CompletableFuture<Optional<AbstractDoorBase>> futureDoor,
                            final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                            final @Nullable IPPlayer responsible, final double time, final boolean skipAnimation);

    /**
     * Constructs a door action event.
     *
     * @param futureDoor  The door.
     * @param cause       What caused the action.
     * @param actionType  The type of action.
     * @param responsible Who is responsible for this door. If null, the door's owner will be used.
     * @param time        The number of seconds the door will take to open. Note that there are other factors that
     *                    affect the total time as well.
     */
    @NotNull
    IDoorActionEvent create(final @NotNull CompletableFuture<Optional<AbstractDoorBase>> futureDoor,
                            final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                            final @Nullable IPPlayer responsible, final double time);

    /**
     * Constructs a door action event.
     *
     * @param futureDoor    The door.
     * @param cause         What caused the action.
     * @param actionType    The type of action.
     * @param responsible   Who is responsible for this door. If null, the door's owner will be used.
     * @param skipAnimation If true, the door will skip the animation and open instantly.
     */
    @NotNull
    IDoorActionEvent create(final @NotNull CompletableFuture<Optional<AbstractDoorBase>> futureDoor,
                            final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                            final @Nullable IPPlayer responsible, final boolean skipAnimation);

    /**
     * Constructs a door action event.
     *
     * @param futureDoor  The door.
     * @param cause       What caused the action.
     * @param actionType  The type of action.
     * @param responsible Who is responsible for this door. If null, the door's owner will be used.
     */
    @NotNull
    IDoorActionEvent create(final @NotNull CompletableFuture<Optional<AbstractDoorBase>> futureDoor,
                            final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                            final @Nullable IPPlayer responsible);

    /**
     * Constructs a door action event.
     *
     * @param doorUID       The UID of the door.
     * @param cause         What caused the action.
     * @param actionType    The type of action.
     * @param responsible   Who is responsible for this door. If null, the door's owner will be used.
     * @param time          The number of seconds the door will take to open. Note that there are other factors that
     *                      affect the total time as well.
     * @param skipAnimation If true, the door will skip the animation and open instantly.
     */
    @NotNull
    IDoorActionEvent create(final long doorUID, final @NotNull DoorActionCause cause,
                            final @NotNull DoorActionType actionType, final @Nullable IPPlayer responsible,
                            final double time, final boolean skipAnimation);

    /**
     * Constructs a door action event.
     *
     * @param doorUID     The UID of the door.
     * @param cause       What caused the action.
     * @param actionType  The type of action.
     * @param responsible Who is responsible for this door. If null, the door's owner will be used.
     * @param time        The number of seconds the door will take to open. Note that there are other factors that
     *                    affect the total time as well.
     */
    @NotNull
    IDoorActionEvent create(final long doorUID, final @NotNull DoorActionCause cause,
                            final @NotNull DoorActionType actionType, final @Nullable IPPlayer responsible,
                            final double time);

    /**
     * Constructs a door action event.
     *
     * @param doorUID       The UID of the door.
     * @param cause         What caused the action.
     * @param actionType    The type of action.
     * @param responsible   Who is responsible for this door. If null, the door's owner will be used.
     * @param skipAnimation If true, the door will skip the animation and open instantly.
     */
    @NotNull
    IDoorActionEvent create(final long doorUID, final @NotNull DoorActionCause cause,
                            final @NotNull DoorActionType actionType, final @Nullable IPPlayer responsible,
                            final boolean skipAnimation);

    /**
     * Constructs a door action event.
     *
     * @param doorUID     The UID of the door.
     * @param cause       What caused the action.
     * @param actionType  The type of action.
     * @param responsible Who is responsible for this door. If null, the door's owner will be used.
     */
    @NotNull
    IDoorActionEvent create(final long doorUID, final @NotNull DoorActionCause cause,
                            final @NotNull DoorActionType actionType, final @Nullable IPPlayer responsible);
}
