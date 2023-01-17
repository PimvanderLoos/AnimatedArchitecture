package nl.pim16aap2.bigdoors.api.factories;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.events.IBigDoorsEvent;
import nl.pim16aap2.bigdoors.events.IMovableCreatedEvent;
import nl.pim16aap2.bigdoors.events.IMovablePrepareAddOwnerEvent;
import nl.pim16aap2.bigdoors.events.IMovablePrepareCreateEvent;
import nl.pim16aap2.bigdoors.events.IMovablePrepareDeleteEvent;
import nl.pim16aap2.bigdoors.events.IMovablePrepareLockChangeEvent;
import nl.pim16aap2.bigdoors.events.IMovablePrepareRemoveOwnerEvent;
import nl.pim16aap2.bigdoors.events.movableaction.IMovableEventToggleEnd;
import nl.pim16aap2.bigdoors.events.movableaction.IMovableEventTogglePrepare;
import nl.pim16aap2.bigdoors.events.movableaction.IMovableEventToggleStart;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionCause;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionType;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableOwner;
import nl.pim16aap2.bigdoors.movable.MovableSnapshot;
import nl.pim16aap2.bigdoors.util.Cuboid;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a class that can create {@link IBigDoorsEvent}s.
 *
 * @author Pim
 */
public interface IBigDoorsEventFactory
{
    /**
     * Constructs a new {@link IMovableCreatedEvent}.
     *
     * @param preview
     *     The preview of the movable that is to be created.
     * @param responsible
     *     The {@link IPPlayer} responsible for the action, if a player was responsible for it.
     */
    IMovableCreatedEvent createMovableCreatedEvent(AbstractMovable preview, @Nullable IPPlayer responsible);

    /**
     * Constructs a new {@link IMovableCreatedEvent} and assumes it was not created by an {@link IPPlayer}.
     * <p>
     * When the movable is created by an {@link IPPlayer}, consider using
     * {@link #createMovableCreatedEvent(AbstractMovable, IPPlayer)} instead.
     *
     * @param preview
     *     The preview of the movable that is to be created.
     */
    default IMovableCreatedEvent createMovableCreatedEvent(AbstractMovable preview)
    {
        return createMovableCreatedEvent(preview, null);
    }

    /**
     * Constructs a new {@link IMovablePrepareCreateEvent}.
     *
     * @param movable
     *     The movable that was created.
     * @param responsible
     *     The {@link IPPlayer} responsible for the action, if a player was responsible for it.
     */
    IMovablePrepareCreateEvent createPrepareMovableCreateEvent(AbstractMovable movable, @Nullable IPPlayer responsible);

    /**
     * Constructs a new {@link IMovablePrepareCreateEvent} and assumes it was not created by an {@link IPPlayer}.
     * <p>
     * When the movable is created by a player, consider using
     * {@link #createPrepareMovableCreateEvent(AbstractMovable, IPPlayer)} instead.
     *
     * @param movable
     *     The movable that was created.
     */
    default IMovablePrepareCreateEvent createPrepareMovableCreateEvent(AbstractMovable movable)
    {
        return createPrepareMovableCreateEvent(movable, null);
    }

    /**
     * Constructs a new {@link IMovablePrepareDeleteEvent}.
     *
     * @param movable
     *     The {@link AbstractMovable} that will be deleted.
     * @param responsible
     *     The {@link IPPlayer} responsible for the action, if a player was responsible for it.
     */
    IMovablePrepareDeleteEvent createPrepareDeleteMovableEvent(AbstractMovable movable, @Nullable IPPlayer responsible);

    /**
     * Constructs a new {@link IMovablePrepareDeleteEvent} and assumes it was not deleted by an {@link IPPlayer}.
     * <p>
     * When the movable is deleted by a player, consider using
     * {@link #createPrepareDeleteMovableEvent(AbstractMovable, IPPlayer)} instead.
     *
     * @param movable
     *     The {@link AbstractMovable} that will be deleted.
     */
    default IMovablePrepareDeleteEvent createPrepareDeleteMovableEvent(AbstractMovable movable)
    {
        return createPrepareDeleteMovableEvent(movable, null);
    }

    /**
     * Constructs a new {@link IMovablePrepareAddOwnerEvent}.
     *
     * @param movable
     *     The movable to which a new owner is to be added.
     * @param newOwner
     *     The new {@link MovableOwner} that is to be added to the movable.
     * @param responsible
     *     The {@link IPPlayer} responsible for the action, if a player was responsible for it.
     */
    IMovablePrepareAddOwnerEvent createMovablePrepareAddOwnerEvent(
        AbstractMovable movable, MovableOwner newOwner, @Nullable IPPlayer responsible);

    /**
     * Constructs a new {@link IMovablePrepareAddOwnerEvent} and assumes it was not added by an {@link IPPlayer}.
     * <p>
     * If the owner is added by an {@link IPPlayer}, consider using
     * {@link #createMovablePrepareAddOwnerEvent(AbstractMovable, MovableOwner, IPPlayer)} instead.
     *
     * @param movable
     *     The movable to which a new owner is to be added.
     * @param newOwner
     *     The new {@link MovableOwner} that is to be added to the movable.
     */
    default IMovablePrepareAddOwnerEvent createMovablePrepareAddOwnerEvent(
        AbstractMovable movable, MovableOwner newOwner)
    {
        return createMovablePrepareAddOwnerEvent(movable, newOwner, null);
    }

    /**
     * Constructs a new {@link IMovablePrepareRemoveOwnerEvent}.
     *
     * @param movable
     *     The movable from which an owner will be removed.
     * @param removedOwner
     *     The {@link MovableOwner} that is to be removed from the movable.
     * @param responsible
     *     The {@link IPPlayer} responsible for the action, if a player was responsible for it.
     */
    IMovablePrepareRemoveOwnerEvent createMovablePrepareRemoveOwnerEvent(
        AbstractMovable movable, MovableOwner removedOwner, @Nullable IPPlayer responsible);

    /**
     * Constructs a new {@link IMovablePrepareRemoveOwnerEvent} and assumes the owner was not removed by an
     * {@link IPPlayer}.
     * <p>
     * If the owner is removed by a player, consider using
     * {@link #createMovablePrepareRemoveOwnerEvent(AbstractMovable, MovableOwner, IPPlayer)} instead.
     *
     * @param movable
     *     The movable from which an owner will be removed.
     * @param removedOwner
     *     The {@link MovableOwner} that is to be removed from the movable.
     */
    default IMovablePrepareRemoveOwnerEvent createMovablePrepareRemoveOwnerEvent(
        AbstractMovable movable, MovableOwner removedOwner)
    {
        return createMovablePrepareRemoveOwnerEvent(movable, removedOwner, null);
    }


    /**
     * Constructs a new {@link IMovablePrepareLockChangeEvent}.
     *
     * @param movable
     *     The movable to which the lock status is to be changed
     * @param newLockStatus
     *     The new locked status of the movable.
     * @param responsible
     *     The {@link IPPlayer} responsible for the action, if a player was responsible for it.
     */
    IMovablePrepareLockChangeEvent createMovablePrepareLockChangeEvent(
        AbstractMovable movable, boolean newLockStatus, @Nullable IPPlayer responsible);

    /**
     * Constructs a new {@link IMovablePrepareLockChangeEvent} and assumes it was not added by an {@link IPPlayer}.
     * <p>
     * If the owner is added by a player, consider using
     * {@link #createMovablePrepareLockChangeEvent(AbstractMovable, boolean, IPPlayer)} instead.
     *
     * @param movable
     *     The movable to which the lock status is to be changed
     * @param newLockStatus
     *     The new locked status of the movable.
     */
    default IMovablePrepareLockChangeEvent createMovablePrepareLockChangeEvent(
        AbstractMovable movable, boolean newLockStatus)
    {
        return createMovablePrepareLockChangeEvent(movable, newLockStatus, null);
    }

    /**
     * Constructs a {@link IMovableEventTogglePrepare}.
     *
     * @param snapshot
     *     The snapshot of the movable.
     * @param cause
     *     What caused the action.
     * @param actionType
     *     The type of action.
     * @param responsible
     *     Who is responsible for this movable. Either the player who directly toggled it (via a command or the GUI), or
     *     the original creator when this data is not available.
     * @param time
     *     The number of seconds the movable will take to open. Note that there are other factors that affect the total
     *     time as well.
     * @param skipAnimation
     *     If true, the movable will skip the animation and open instantly.
     * @param newCuboid
     *     The {@link Cuboid} representing the area the movable will take up after the toggle.
     */
    IMovableEventTogglePrepare createTogglePrepareEvent(
        MovableSnapshot snapshot, MovableActionCause cause, MovableActionType actionType, IPPlayer responsible,
        double time, boolean skipAnimation, Cuboid newCuboid);

    /**
     * Constructs a {@link IMovableEventToggleStart}.
     *
     * @param movable
     *     The movable itself.
     * @param movableSnapshot
     *     A snapshot of the movable created at the time the toggle action was requested.
     * @param cause
     *     What caused the action.
     * @param actionType
     *     The type of action.
     * @param responsible
     *     Who is responsible for this movable. Either the player who directly toggled it (via a command or the GUI), or
     *     the original creator when this data is not available.
     * @param time
     *     The number of seconds the movable will take to open. Note that there are other factors that affect the total
     *     time as well.
     * @param skipAnimation
     *     If true, the movable will skip the animation and open instantly.
     * @param newCuboid
     *     The {@link Cuboid} representing the area the movable will take up after the toggle.
     */
    IMovableEventToggleStart createToggleStartEvent(
        AbstractMovable movable, MovableSnapshot movableSnapshot, MovableActionCause cause,
        MovableActionType actionType, IPPlayer responsible, double time, boolean skipAnimation, Cuboid newCuboid);

    /**
     * Constructs a {@link IMovableEventToggleEnd}.
     *
     * @param movable
     *     The movable.
     * @param snapshot
     *     A snapshot of the movable created when the movable was preparing to toggle.
     * @param cause
     *     What caused the action.
     * @param actionType
     *     The type of action.
     * @param responsible
     *     Who is responsible for this movable. Either the player who directly toggled it (via a command or the GUI), or
     *     the original creator when this data is not available.
     * @param time
     *     The number of seconds the movable will take to open. Note that there are other factors that affect the total
     *     time as well.
     * @param skipAnimation
     *     If true, the movable will skip the animation and open instantly.
     */
    IMovableEventToggleEnd createToggleEndEvent(
        AbstractMovable movable, MovableSnapshot snapshot, MovableActionCause cause,
        MovableActionType actionType, IPPlayer responsible, double time, boolean skipAnimation);
}
