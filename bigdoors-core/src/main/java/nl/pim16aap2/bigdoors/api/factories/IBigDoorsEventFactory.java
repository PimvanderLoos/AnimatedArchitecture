package nl.pim16aap2.bigdoors.api.factories;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.IBigDoorsEvent;
import nl.pim16aap2.bigdoors.events.IDoorCreatedEvent;
import nl.pim16aap2.bigdoors.events.IDoorPrepareAddOwnerEvent;
import nl.pim16aap2.bigdoors.events.IDoorPrepareCreateEvent;
import nl.pim16aap2.bigdoors.events.IDoorPrepareDeleteEvent;
import nl.pim16aap2.bigdoors.events.IDoorPrepareLockChangeEvent;
import nl.pim16aap2.bigdoors.events.IDoorPrepareRemoveOwnerEvent;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEventToggleEnd;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEventTogglePrepare;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEventToggleStart;
import nl.pim16aap2.bigdoors.util.CuboidConst;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a class that can create {@link IBigDoorsEvent}s.
 *
 * @author Pim
 */
public interface IBigDoorsEventFactory
{
    /**
     * Constructs a new {@link IDoorCreatedEvent}.
     *
     * @param preview     The preview of the door that is to be created.
     * @param responsible The {@link IPPlayer} responsible for the action, if a player was responsible for it.
     */
    @NonNull IDoorCreatedEvent createDoorCreatedEvent(final @NonNull AbstractDoorBase preview,
                                                      final @Nullable IPPlayer responsible);

    /**
     * Constructs a new {@link IDoorCreatedEvent} and assumes it was not created by an {@link IPPlayer}.
     * <p>
     * When the door is created by an {@link IPPlayer}, consider using {@link #createDoorCreatedEvent(AbstractDoorBase,
     * IPPlayer)} instead.
     *
     * @param preview The preview of the door that is to be created.
     */
    default @NonNull IDoorCreatedEvent createDoorCreatedEvent(final @NonNull AbstractDoorBase preview)
    {
        return createDoorCreatedEvent(preview, null);
    }

    /**
     * Constructs a new {@link IDoorPrepareCreateEvent}.
     *
     * @param door        The door that was created.
     * @param responsible The {@link IPPlayer} responsible for the action, if a player was responsible for it.
     */
    @NonNull IDoorPrepareCreateEvent createPrepareDoorCreateEvent(final @NonNull AbstractDoorBase door,
                                                                  final @Nullable IPPlayer responsible);

    /**
     * Constructs a new {@link IDoorPrepareCreateEvent} and assumes it was not created by an {@link IPPlayer}.
     * <p>
     * When the door is created by an {@link IPPlayer}, consider using {@link #createPrepareDoorCreateEvent(AbstractDoorBase,
     * IPPlayer)} instead.
     *
     * @param door The door that was created.
     */
    default @NonNull IDoorPrepareCreateEvent createPrepareDoorCreateEvent(final @NonNull AbstractDoorBase door)
    {
        return createPrepareDoorCreateEvent(door, null);
    }

    /**
     * Constructs a new {@link IDoorPrepareDeleteEvent}.
     *
     * @param door        The {@link AbstractDoorBase} that will be deleted.
     * @param responsible The {@link IPPlayer} responsible for the action, if a player was responsible for it.
     */
    @NonNull IDoorPrepareDeleteEvent createPrepareDeleteDoorEvent(final @NonNull AbstractDoorBase door,
                                                                  final @Nullable IPPlayer responsible);

    /**
     * Constructs a new {@link IDoorPrepareDeleteEvent} and assumes it was not deleted by an {@link IPPlayer}.
     * <p>
     * When the door is deleted by an {@link IPPlayer}, consider using {@link #createPrepareDeleteDoorEvent(AbstractDoorBase,
     * IPPlayer)} instead.
     *
     * @param door The {@link AbstractDoorBase} that will be deleted.
     */
    default @NonNull IDoorPrepareDeleteEvent createPrepareDeleteDoorEvent(final @NonNull AbstractDoorBase door)
    {
        return createPrepareDeleteDoorEvent(door, null);
    }

    /**
     * Constructs a new {@link IDoorPrepareAddOwnerEvent}.
     *
     * @param door        The door to which a new owner is to be added.
     * @param newOwner    The new {@link DoorOwner} that is to be added to the door.
     * @param responsible The {@link IPPlayer} responsible for the action, if a player was responsible for it.
     */
    @NonNull IDoorPrepareAddOwnerEvent createDoorPrepareAddOwnerEvent(final @NonNull AbstractDoorBase door,
                                                                      final @NonNull DoorOwner newOwner,
                                                                      final @Nullable IPPlayer responsible);

    /**
     * Constructs a new {@link IDoorPrepareAddOwnerEvent} and assumes it was not added by an {@link IPPlayer}.
     * <p>
     * If the owner is added by an {@link IPPlayer}, consider using {@link #createDoorPrepareAddOwnerEvent(AbstractDoorBase,
     * DoorOwner, IPPlayer)} instead.
     *
     * @param door     The door to which a new owner is to be added.
     * @param newOwner The new {@link DoorOwner} that is to be added to the door.
     */
    default @NonNull IDoorPrepareAddOwnerEvent createDoorPrepareAddOwnerEvent(final @NonNull AbstractDoorBase door,
                                                                              final @NonNull DoorOwner newOwner)
    {
        return createDoorPrepareAddOwnerEvent(door, newOwner, null);
    }

    /**
     * Constructs a new {@link IDoorPrepareRemoveOwnerEvent}.
     *
     * @param door         The door from which an owner will be removed.
     * @param removedOwner The {@link DoorOwner} that is to be removed from the door.
     * @param responsible  The {@link IPPlayer} responsible for the action, if a player was responsible for it.
     */
    @NonNull IDoorPrepareRemoveOwnerEvent createDoorPrepareRemoveOwnerEvent(final @NonNull AbstractDoorBase door,
                                                                            final @NonNull DoorOwner removedOwner,
                                                                            final @Nullable IPPlayer responsible);

    /**
     * Constructs a new {@link IDoorPrepareRemoveOwnerEvent} and assumes the owner was not removed by an {@link
     * IPPlayer}.
     * <p>
     * If the owner is removed by an {@link IPPlayer}, consider using {@link #createDoorPrepareRemoveOwnerEvent(AbstractDoorBase,
     * DoorOwner, IPPlayer)} instead.
     *
     * @param door         The door from which an owner will be removed.
     * @param removedOwner The {@link DoorOwner} that is to be removed from the door.
     */
    default @NonNull IDoorPrepareRemoveOwnerEvent createDoorPrepareRemoveOwnerEvent(
        final @NonNull AbstractDoorBase door, final @NonNull DoorOwner removedOwner)
    {
        return createDoorPrepareRemoveOwnerEvent(door, removedOwner, null);
    }


    /**
     * Constructs a new {@link IDoorPrepareLockChangeEvent}.
     *
     * @param door          The door to which the lock status is to be changed
     * @param newLockStatus The new locked status of the door.
     * @param responsible   The {@link IPPlayer} responsible for the action, if a player was responsible for it.
     */
    @NonNull IDoorPrepareLockChangeEvent createDoorPrepareLockChangeEvent(final @NonNull AbstractDoorBase door,
                                                                          final boolean newLockStatus,
                                                                          final @Nullable IPPlayer responsible);

    /**
     * Constructs a new {@link IDoorPrepareLockChangeEvent} and assumes it was not added by an {@link IPPlayer}.
     * <p>
     * If the owner is added by an {@link IPPlayer}, consider using {@link #createDoorPrepareLockChangeEvent(AbstractDoorBase,
     * boolean, IPPlayer)} instead.
     *
     * @param door          The door to which the lock status is to be changed
     * @param newLockStatus The new locked status of the door.
     */
    default @NonNull IDoorPrepareLockChangeEvent createDoorPrepareLockChangeEvent(final @NonNull AbstractDoorBase door,
                                                                                  final boolean newLockStatus)
    {
        return createDoorPrepareLockChangeEvent(door, newLockStatus, null);
    }

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
    @NonNull IDoorEventTogglePrepare createTogglePrepareEvent(@NonNull AbstractDoorBase door,
                                                              @NonNull DoorActionCause cause,
                                                              @NonNull DoorActionType actionType,
                                                              @NonNull IPPlayer responsible, double time,
                                                              boolean skipAnimation,
                                                              @NonNull CuboidConst newCuboid);

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
    @NonNull IDoorEventToggleStart createToggleStartEvent(@NonNull AbstractDoorBase door,
                                                          @NonNull DoorActionCause cause,
                                                          @NonNull DoorActionType actionType,
                                                          @NonNull IPPlayer responsible, double time,
                                                          boolean skipAnimation, @NonNull CuboidConst newCuboid);

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
    @NonNull IDoorEventToggleEnd createToggleEndEvent(@NonNull AbstractDoorBase door,
                                                      @NonNull DoorActionCause cause,
                                                      @NonNull DoorActionType actionType,
                                                      @NonNull IPPlayer responsible, double time,
                                                      boolean skipAnimation);
}
