package nl.pim16aap2.animatedarchitecture.core.api.factories;

import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.events.IAnimatedArchitectureEvent;
import nl.pim16aap2.animatedarchitecture.core.events.IStructureCreatedEvent;
import nl.pim16aap2.animatedarchitecture.core.events.IStructureEventToggleEnd;
import nl.pim16aap2.animatedarchitecture.core.events.IStructureEventTogglePrepare;
import nl.pim16aap2.animatedarchitecture.core.events.IStructureEventToggleStart;
import nl.pim16aap2.animatedarchitecture.core.events.IStructurePrepareAddOwnerEvent;
import nl.pim16aap2.animatedarchitecture.core.events.IStructurePrepareCreateEvent;
import nl.pim16aap2.animatedarchitecture.core.events.IStructurePrepareDeleteEvent;
import nl.pim16aap2.animatedarchitecture.core.events.IStructurePrepareLockChangeEvent;
import nl.pim16aap2.animatedarchitecture.core.events.IStructurePrepareRemoveOwnerEvent;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionCause;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionType;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureOwner;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a class that can create {@link IAnimatedArchitectureEvent}s.
 */
public interface IAnimatedArchitectureEventFactory
{
    /**
     * Constructs a new {@link IStructureCreatedEvent}.
     *
     * @param preview
     *     The preview of the structure that is to be created.
     * @param responsible
     *     The {@link IPlayer} responsible for the action, if a player was responsible for it.
     */
    IStructureCreatedEvent createStructureCreatedEvent(Structure preview, @Nullable IPlayer responsible);

    /**
     * Constructs a new {@link IStructureCreatedEvent} and assumes it was not created by an {@link IPlayer}.
     * <p>
     * When the structure is created by an {@link IPlayer}, consider using
     * {@link #createStructureCreatedEvent(Structure, IPlayer)} instead.
     *
     * @param preview
     *     The preview of the structure that is to be created.
     */
    default IStructureCreatedEvent createStructureCreatedEvent(Structure preview)
    {
        return createStructureCreatedEvent(preview, null);
    }

    /**
     * Constructs a new {@link IStructurePrepareCreateEvent}.
     *
     * @param structure
     *     The structure that was created.
     * @param responsible
     *     The {@link IPlayer} responsible for the action, if a player was responsible for it.
     */
    IStructurePrepareCreateEvent createPrepareStructureCreateEvent(
        Structure structure,
        @Nullable IPlayer responsible
    );

    /**
     * Constructs a new {@link IStructurePrepareCreateEvent} and assumes it was not created by an {@link IPlayer}.
     * <p>
     * When the structure is created by a player, consider using
     * {@link #createPrepareStructureCreateEvent(Structure, IPlayer)} instead.
     *
     * @param structure
     *     The structure that was created.
     */
    default IStructurePrepareCreateEvent createPrepareStructureCreateEvent(Structure structure)
    {
        return createPrepareStructureCreateEvent(structure, null);
    }

    /**
     * Constructs a new {@link IStructurePrepareDeleteEvent}.
     *
     * @param structure
     *     The {@link Structure} that will be deleted.
     * @param responsible
     *     The {@link IPlayer} responsible for the action, if a player was responsible for it.
     */
    IStructurePrepareDeleteEvent createPrepareDeleteStructureEvent(
        Structure structure,
        @Nullable IPlayer responsible
    );

    /**
     * Constructs a new {@link IStructurePrepareDeleteEvent} and assumes it was not deleted by an {@link IPlayer}.
     * <p>
     * When the structure is deleted by a player, consider using
     * {@link #createPrepareDeleteStructureEvent(Structure, IPlayer)} instead.
     *
     * @param structure
     *     The {@link Structure} that will be deleted.
     */
    default IStructurePrepareDeleteEvent createPrepareDeleteStructureEvent(Structure structure)
    {
        return createPrepareDeleteStructureEvent(structure, null);
    }

    /**
     * Constructs a new {@link IStructurePrepareAddOwnerEvent}.
     *
     * @param structure
     *     The structure to which a new owner is to be added.
     * @param newOwner
     *     The new {@link StructureOwner} that is to be added to the structure.
     * @param responsible
     *     The {@link IPlayer} responsible for the action, if a player was responsible for it.
     */
    IStructurePrepareAddOwnerEvent createStructurePrepareAddOwnerEvent(
        Structure structure,
        StructureOwner newOwner,
        @Nullable IPlayer responsible
    );

    /**
     * Constructs a new {@link IStructurePrepareAddOwnerEvent} and assumes it was not added by an {@link IPlayer}.
     * <p>
     * If the owner is added by an {@link IPlayer}, consider using
     * {@link #createStructurePrepareAddOwnerEvent(Structure, StructureOwner, IPlayer)} instead.
     *
     * @param structure
     *     The structure to which a new owner is to be added.
     * @param newOwner
     *     The new {@link StructureOwner} that is to be added to the structure.
     */
    default IStructurePrepareAddOwnerEvent createStructurePrepareAddOwnerEvent(
        Structure structure,
        StructureOwner newOwner)
    {
        return createStructurePrepareAddOwnerEvent(structure, newOwner, null);
    }

    /**
     * Constructs a new {@link IStructurePrepareRemoveOwnerEvent}.
     *
     * @param structure
     *     The structure from which an owner will be removed.
     * @param removedOwner
     *     The {@link StructureOwner} that is to be removed from the structure.
     * @param responsible
     *     The {@link IPlayer} responsible for the action, if a player was responsible for it.
     */
    IStructurePrepareRemoveOwnerEvent createStructurePrepareRemoveOwnerEvent(
        Structure structure,
        StructureOwner removedOwner,
        @Nullable IPlayer responsible
    );

    /**
     * Constructs a new {@link IStructurePrepareRemoveOwnerEvent} and assumes the owner was not removed by an
     * {@link IPlayer}.
     * <p>
     * If the owner is removed by a player, consider using
     * {@link #createStructurePrepareRemoveOwnerEvent(Structure, StructureOwner, IPlayer)} instead.
     *
     * @param structure
     *     The structure from which an owner will be removed.
     * @param removedOwner
     *     The {@link StructureOwner} that is to be removed from the structure.
     */
    default IStructurePrepareRemoveOwnerEvent createStructurePrepareRemoveOwnerEvent(
        Structure structure,
        StructureOwner removedOwner)
    {
        return createStructurePrepareRemoveOwnerEvent(structure, removedOwner, null);
    }


    /**
     * Constructs a new {@link IStructurePrepareLockChangeEvent}.
     *
     * @param structure
     *     The structure to which the lock status is to be changed
     * @param newLockStatus
     *     The new locked status of the structure.
     * @param responsible
     *     The {@link IPlayer} responsible for the action, if a player was responsible for it.
     */
    IStructurePrepareLockChangeEvent createStructurePrepareLockChangeEvent(
        Structure structure,
        boolean newLockStatus,
        @Nullable IPlayer responsible
    );

    /**
     * Constructs a new {@link IStructurePrepareLockChangeEvent} and assumes it was not added by an {@link IPlayer}.
     * <p>
     * If the owner is added by a player, consider using
     * {@link #createStructurePrepareLockChangeEvent(Structure, boolean, IPlayer)} instead.
     *
     * @param structure
     *     The structure to which the lock status is to be changed
     * @param newLockStatus
     *     The new locked status of the structure.
     */
    default IStructurePrepareLockChangeEvent createStructurePrepareLockChangeEvent(
        Structure structure,
        boolean newLockStatus)
    {
        return createStructurePrepareLockChangeEvent(structure, newLockStatus, null);
    }

    /**
     * Constructs a {@link IStructureEventTogglePrepare}.
     *
     * @param snapshot
     *     The snapshot of the structure.
     * @param cause
     *     What caused the action.
     * @param actionType
     *     The type of action.
     * @param responsible
     *     Who is responsible for this structure. Either the player who directly toggled it (via a command or the GUI),
     *     or the original creator when this data is not available.
     * @param time
     *     The number of seconds the structure will take to open. Note that there are other factors that affect the
     *     total time as well.
     * @param skipAnimation
     *     If true, the structure will skip the animation and open instantly.
     * @param newCuboid
     *     The {@link Cuboid} representing the area the structure will take up after the toggle.
     */
    IStructureEventTogglePrepare createTogglePrepareEvent(
        StructureSnapshot snapshot,
        StructureActionCause cause,
        StructureActionType actionType,
        IPlayer responsible,
        double time,
        boolean skipAnimation,
        Cuboid newCuboid
    );

    /**
     * Constructs a {@link IStructureEventToggleStart}.
     *
     * @param structure
     *     The structure itself.
     * @param structureSnapshot
     *     A snapshot of the structure created at the time the toggle action was requested.
     * @param cause
     *     What caused the action.
     * @param actionType
     *     The type of action.
     * @param responsible
     *     Who is responsible for this structure. Either the player who directly toggled it (via a command or the GUI),
     *     or the original creator when this data is not available.
     * @param time
     *     The number of seconds the structure will take to open. Note that there are other factors that affect the
     *     total time as well.
     * @param skipAnimation
     *     If true, the structure will skip the animation and open instantly.
     * @param newCuboid
     *     The {@link Cuboid} representing the area the structure will take up after the toggle.
     */
    IStructureEventToggleStart createToggleStartEvent(
        Structure structure,
        StructureSnapshot structureSnapshot,
        StructureActionCause cause,
        StructureActionType actionType,
        IPlayer responsible,
        double time,
        boolean skipAnimation,
        Cuboid newCuboid
    );

    /**
     * Constructs a {@link IStructureEventToggleEnd}.
     *
     * @param structure
     *     The structure.
     * @param snapshot
     *     A snapshot of the structure created when the structure was preparing to toggle.
     * @param cause
     *     What caused the action.
     * @param actionType
     *     The type of action.
     * @param responsible
     *     Who is responsible for this structure. Either the player who directly toggled it (via a command or the GUI),
     *     or the original creator when this data is not available.
     * @param time
     *     The number of seconds the structure will take to open. Note that there are other factors that affect the
     *     total time as well.
     * @param skipAnimation
     *     If true, the structure will skip the animation and open instantly.
     */
    IStructureEventToggleEnd createToggleEndEvent(
        Structure structure,
        StructureSnapshot snapshot,
        StructureActionCause cause,
        StructureActionType actionType,
        IPlayer responsible,
        double time,
        boolean skipAnimation
    );
}
