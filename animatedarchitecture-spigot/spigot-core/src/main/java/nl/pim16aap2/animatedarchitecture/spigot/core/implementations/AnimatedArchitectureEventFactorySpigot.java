package nl.pim16aap2.animatedarchitecture.spigot.core.implementations;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IAnimatedArchitectureEventFactory;
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
import nl.pim16aap2.animatedarchitecture.spigot.core.events.StructureCreatedEvent;
import nl.pim16aap2.animatedarchitecture.spigot.core.events.StructureEventToggleEnd;
import nl.pim16aap2.animatedarchitecture.spigot.core.events.StructureEventTogglePrepare;
import nl.pim16aap2.animatedarchitecture.spigot.core.events.StructureEventToggleStart;
import nl.pim16aap2.animatedarchitecture.spigot.core.events.StructurePrepareAddOwnerEvent;
import nl.pim16aap2.animatedarchitecture.spigot.core.events.StructurePrepareCreateEvent;
import nl.pim16aap2.animatedarchitecture.spigot.core.events.StructurePrepareDeleteEvent;
import nl.pim16aap2.animatedarchitecture.spigot.core.events.StructurePrepareLockChangeEvent;
import nl.pim16aap2.animatedarchitecture.spigot.core.events.StructurePrepareRemoveOwnerEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The implementation of {@link IAnimatedArchitectureEventFactory} for the Spigot platform.
 */
@Singleton
public class AnimatedArchitectureEventFactorySpigot implements IAnimatedArchitectureEventFactory
{
    @Inject
    public AnimatedArchitectureEventFactorySpigot()
    {
    }


    @Override
    public IStructureCreatedEvent createStructureCreatedEvent(Structure preview, @Nullable IPlayer responsible)
    {
        return new StructureCreatedEvent(preview, responsible);
    }

    @Override
    public IStructurePrepareCreateEvent createPrepareStructureCreateEvent(
        Structure structure,
        @Nullable IPlayer responsible)
    {
        return new StructurePrepareCreateEvent(structure, responsible);
    }

    @Override
    public IStructurePrepareDeleteEvent createPrepareDeleteStructureEvent(
        Structure structure,
        @Nullable IPlayer responsible)
    {
        return new StructurePrepareDeleteEvent(structure, responsible);
    }

    @Override
    public IStructurePrepareAddOwnerEvent createStructurePrepareAddOwnerEvent(
        Structure structure,
        StructureOwner newOwner,
        @Nullable IPlayer responsible)
    {
        return new StructurePrepareAddOwnerEvent(structure, responsible, newOwner);
    }

    @Override
    public IStructurePrepareRemoveOwnerEvent createStructurePrepareRemoveOwnerEvent(
        Structure structure,
        StructureOwner removedOwner,
        @Nullable IPlayer responsible)
    {
        return new StructurePrepareRemoveOwnerEvent(structure, responsible, removedOwner);
    }

    @Override
    public IStructurePrepareLockChangeEvent createStructurePrepareLockChangeEvent(
        Structure structure,
        boolean newLockStatus,
        @Nullable IPlayer responsible)
    {
        return new StructurePrepareLockChangeEvent(structure, responsible, newLockStatus);
    }

    @Override
    public IStructureEventTogglePrepare createTogglePrepareEvent(
        StructureSnapshot snapshot,
        StructureActionCause cause,
        StructureActionType actionType,
        IPlayer responsible,
        double time,
        boolean skipAnimation,
        Cuboid newCuboid)
    {
        return new StructureEventTogglePrepare(
            snapshot,
            cause,
            actionType,
            responsible,
            time,
            skipAnimation,
            newCuboid
        );
    }

    @Override
    public IStructureEventToggleStart createToggleStartEvent(
        Structure structure,
        StructureSnapshot structureSnapshot,
        StructureActionCause cause,
        StructureActionType actionType,
        IPlayer responsible,
        double time,
        boolean skipAnimation,
        Cuboid newCuboid)

    {
        return new StructureEventToggleStart(
            structure,
            structureSnapshot,
            cause,
            actionType,
            responsible,
            time,
            skipAnimation,
            newCuboid
        );
    }

    @Override
    public IStructureEventToggleEnd createToggleEndEvent(
        Structure structure,
        StructureSnapshot snapshot,
        StructureActionCause cause,
        StructureActionType actionType,
        IPlayer responsible,
        double time,
        boolean skipAnimation)
    {
        return new StructureEventToggleEnd(structure, snapshot, cause, actionType, responsible, time, skipAnimation);
    }
}
