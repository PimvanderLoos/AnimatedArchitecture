package nl.pim16aap2.bigdoors.core.api.factories;

import nl.pim16aap2.bigdoors.core.api.IPPlayer;
import nl.pim16aap2.bigdoors.core.events.IBigDoorsEvent;
import nl.pim16aap2.bigdoors.core.events.IStructureCreatedEvent;
import nl.pim16aap2.bigdoors.core.events.IStructurePrepareAddOwnerEvent;
import nl.pim16aap2.bigdoors.core.events.IStructurePrepareCreateEvent;
import nl.pim16aap2.bigdoors.core.events.IStructurePrepareDeleteEvent;
import nl.pim16aap2.bigdoors.core.events.IStructurePrepareLockChangeEvent;
import nl.pim16aap2.bigdoors.core.events.IStructurePrepareRemoveOwnerEvent;
import nl.pim16aap2.bigdoors.core.events.structureaction.IStructureEventToggleEnd;
import nl.pim16aap2.bigdoors.core.events.structureaction.IStructureEventTogglePrepare;
import nl.pim16aap2.bigdoors.core.events.structureaction.IStructureEventToggleStart;
import nl.pim16aap2.bigdoors.core.events.structureaction.StructureActionCause;
import nl.pim16aap2.bigdoors.core.events.structureaction.StructureActionType;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.structures.StructureOwner;
import nl.pim16aap2.bigdoors.core.structures.StructureSnapshot;
import nl.pim16aap2.bigdoors.core.util.Cuboid;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a class that can create {@link IBigDoorsEvent}s.
 *
 * @author Pim
 */
public interface IBigDoorsEventFactory
{
    /**
     * Constructs a new {@link IStructureCreatedEvent}.
     *
     * @param preview
     *     The preview of the structure that is to be created.
     * @param responsible
     *     The {@link IPPlayer} responsible for the action, if a player was responsible for it.
     */
    IStructureCreatedEvent createStructureCreatedEvent(AbstractStructure preview, @Nullable IPPlayer responsible);

    /**
     * Constructs a new {@link IStructureCreatedEvent} and assumes it was not created by an {@link IPPlayer}.
     * <p>
     * When the structure is created by an {@link IPPlayer}, consider using
     * {@link #createStructureCreatedEvent(AbstractStructure, IPPlayer)} instead.
     *
     * @param preview
     *     The preview of the structure that is to be created.
     */
    default IStructureCreatedEvent createStructureCreatedEvent(AbstractStructure preview)
    {
        return createStructureCreatedEvent(preview, null);
    }

    /**
     * Constructs a new {@link IStructurePrepareCreateEvent}.
     *
     * @param structure
     *     The structure that was created.
     * @param responsible
     *     The {@link IPPlayer} responsible for the action, if a player was responsible for it.
     */
    IStructurePrepareCreateEvent createPrepareStructureCreateEvent(
        AbstractStructure structure, @Nullable IPPlayer responsible);

    /**
     * Constructs a new {@link IStructurePrepareCreateEvent} and assumes it was not created by an {@link IPPlayer}.
     * <p>
     * When the structure is created by a player, consider using
     * {@link #createPrepareStructureCreateEvent(AbstractStructure, IPPlayer)} instead.
     *
     * @param structure
     *     The structure that was created.
     */
    default IStructurePrepareCreateEvent createPrepareStructureCreateEvent(AbstractStructure structure)
    {
        return createPrepareStructureCreateEvent(structure, null);
    }

    /**
     * Constructs a new {@link IStructurePrepareDeleteEvent}.
     *
     * @param structure
     *     The {@link AbstractStructure} that will be deleted.
     * @param responsible
     *     The {@link IPPlayer} responsible for the action, if a player was responsible for it.
     */
    IStructurePrepareDeleteEvent createPrepareDeleteStructureEvent(
        AbstractStructure structure, @Nullable IPPlayer responsible);

    /**
     * Constructs a new {@link IStructurePrepareDeleteEvent} and assumes it was not deleted by an {@link IPPlayer}.
     * <p>
     * When the structure is deleted by a player, consider using
     * {@link #createPrepareDeleteStructureEvent(AbstractStructure, IPPlayer)} instead.
     *
     * @param structure
     *     The {@link AbstractStructure} that will be deleted.
     */
    default IStructurePrepareDeleteEvent createPrepareDeleteStructureEvent(AbstractStructure structure)
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
     *     The {@link IPPlayer} responsible for the action, if a player was responsible for it.
     */
    IStructurePrepareAddOwnerEvent createStructurePrepareAddOwnerEvent(
        AbstractStructure structure, StructureOwner newOwner, @Nullable IPPlayer responsible);

    /**
     * Constructs a new {@link IStructurePrepareAddOwnerEvent} and assumes it was not added by an {@link IPPlayer}.
     * <p>
     * If the owner is added by an {@link IPPlayer}, consider using
     * {@link #createStructurePrepareAddOwnerEvent(AbstractStructure, StructureOwner, IPPlayer)} instead.
     *
     * @param structure
     *     The structure to which a new owner is to be added.
     * @param newOwner
     *     The new {@link StructureOwner} that is to be added to the structure.
     */
    default IStructurePrepareAddOwnerEvent createStructurePrepareAddOwnerEvent(
        AbstractStructure structure, StructureOwner newOwner)
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
     *     The {@link IPPlayer} responsible for the action, if a player was responsible for it.
     */
    IStructurePrepareRemoveOwnerEvent createStructurePrepareRemoveOwnerEvent(
        AbstractStructure structure, StructureOwner removedOwner, @Nullable IPPlayer responsible);

    /**
     * Constructs a new {@link IStructurePrepareRemoveOwnerEvent} and assumes the owner was not removed by an
     * {@link IPPlayer}.
     * <p>
     * If the owner is removed by a player, consider using
     * {@link #createStructurePrepareRemoveOwnerEvent(AbstractStructure, StructureOwner, IPPlayer)} instead.
     *
     * @param structure
     *     The structure from which an owner will be removed.
     * @param removedOwner
     *     The {@link StructureOwner} that is to be removed from the structure.
     */
    default IStructurePrepareRemoveOwnerEvent createStructurePrepareRemoveOwnerEvent(
        AbstractStructure structure, StructureOwner removedOwner)
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
     *     The {@link IPPlayer} responsible for the action, if a player was responsible for it.
     */
    IStructurePrepareLockChangeEvent createStructurePrepareLockChangeEvent(
        AbstractStructure structure, boolean newLockStatus, @Nullable IPPlayer responsible);

    /**
     * Constructs a new {@link IStructurePrepareLockChangeEvent} and assumes it was not added by an {@link IPPlayer}.
     * <p>
     * If the owner is added by a player, consider using
     * {@link #createStructurePrepareLockChangeEvent(AbstractStructure, boolean, IPPlayer)} instead.
     *
     * @param structure
     *     The structure to which the lock status is to be changed
     * @param newLockStatus
     *     The new locked status of the structure.
     */
    default IStructurePrepareLockChangeEvent createStructurePrepareLockChangeEvent(
        AbstractStructure structure, boolean newLockStatus)
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
        StructureSnapshot snapshot, StructureActionCause cause, StructureActionType actionType, IPPlayer responsible,
        double time, boolean skipAnimation, Cuboid newCuboid);

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
        AbstractStructure structure, StructureSnapshot structureSnapshot, StructureActionCause cause,
        StructureActionType actionType, IPPlayer responsible, double time, boolean skipAnimation, Cuboid newCuboid);

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
        AbstractStructure structure, StructureSnapshot snapshot, StructureActionCause cause,
        StructureActionType actionType, IPPlayer responsible, double time, boolean skipAnimation);
}
