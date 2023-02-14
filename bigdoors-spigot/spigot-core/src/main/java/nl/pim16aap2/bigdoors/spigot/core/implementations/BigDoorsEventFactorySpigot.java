package nl.pim16aap2.bigdoors.spigot.core.implementations;

import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.api.factories.IBigDoorsEventFactory;
import nl.pim16aap2.bigdoors.core.events.IStructureCreatedEvent;
import nl.pim16aap2.bigdoors.core.events.IStructureEventToggleEnd;
import nl.pim16aap2.bigdoors.core.events.IStructureEventTogglePrepare;
import nl.pim16aap2.bigdoors.core.events.IStructureEventToggleStart;
import nl.pim16aap2.bigdoors.core.events.IStructurePrepareAddOwnerEvent;
import nl.pim16aap2.bigdoors.core.events.IStructurePrepareCreateEvent;
import nl.pim16aap2.bigdoors.core.events.IStructurePrepareDeleteEvent;
import nl.pim16aap2.bigdoors.core.events.IStructurePrepareLockChangeEvent;
import nl.pim16aap2.bigdoors.core.events.IStructurePrepareRemoveOwnerEvent;
import nl.pim16aap2.bigdoors.core.events.StructureActionCause;
import nl.pim16aap2.bigdoors.core.events.StructureActionType;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.structures.StructureOwner;
import nl.pim16aap2.bigdoors.core.structures.StructureSnapshot;
import nl.pim16aap2.bigdoors.core.util.Cuboid;
import nl.pim16aap2.bigdoors.spigot.core.events.StructureCreatedEvent;
import nl.pim16aap2.bigdoors.spigot.core.events.StructureEventToggleEnd;
import nl.pim16aap2.bigdoors.spigot.core.events.StructureEventTogglePrepare;
import nl.pim16aap2.bigdoors.spigot.core.events.StructureEventToggleStart;
import nl.pim16aap2.bigdoors.spigot.core.events.StructurePrepareAddOwnerEvent;
import nl.pim16aap2.bigdoors.spigot.core.events.StructurePrepareCreateEvent;
import nl.pim16aap2.bigdoors.spigot.core.events.StructurePrepareDeleteEvent;
import nl.pim16aap2.bigdoors.spigot.core.events.StructurePrepareLockChangeEvent;
import nl.pim16aap2.bigdoors.spigot.core.events.StructurePrepareRemoveOwnerEvent;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BigDoorsEventFactorySpigot implements IBigDoorsEventFactory
{
    @Inject
    public BigDoorsEventFactorySpigot()
    {
    }


    @Override
    public IStructureCreatedEvent createStructureCreatedEvent(AbstractStructure preview, @Nullable IPlayer responsible)
    {
        return new StructureCreatedEvent(preview, responsible);
    }

    @Override
    public IStructurePrepareCreateEvent createPrepareStructureCreateEvent(
        AbstractStructure structure, @Nullable IPlayer responsible)
    {
        return new StructurePrepareCreateEvent(structure, responsible);
    }

    @Override
    public IStructurePrepareDeleteEvent createPrepareDeleteStructureEvent(
        AbstractStructure structure, @Nullable IPlayer responsible)
    {
        return new StructurePrepareDeleteEvent(structure, responsible);
    }

    @Override
    public IStructurePrepareAddOwnerEvent createStructurePrepareAddOwnerEvent(
        AbstractStructure structure, StructureOwner newOwner,
        @Nullable IPlayer responsible)
    {
        return new StructurePrepareAddOwnerEvent(structure, responsible, newOwner);
    }

    @Override
    public IStructurePrepareRemoveOwnerEvent createStructurePrepareRemoveOwnerEvent(
        AbstractStructure structure, StructureOwner removedOwner,
        @Nullable IPlayer responsible)
    {
        return new StructurePrepareRemoveOwnerEvent(structure, responsible, removedOwner);
    }

    @Override
    public IStructurePrepareLockChangeEvent createStructurePrepareLockChangeEvent(
        AbstractStructure structure, boolean newLockStatus,
        @Nullable IPlayer responsible)
    {
        return new StructurePrepareLockChangeEvent(structure, responsible, newLockStatus);
    }

    @Override
    public IStructureEventTogglePrepare createTogglePrepareEvent(
        StructureSnapshot snapshot, StructureActionCause cause,
        StructureActionType actionType, IPlayer responsible,
        double time, boolean skipAnimation, Cuboid newCuboid)
    {
        return new StructureEventTogglePrepare(snapshot, cause, actionType, responsible, time, skipAnimation,
                                               newCuboid);
    }

    @Override
    public IStructureEventToggleStart createToggleStartEvent(
        AbstractStructure structure, StructureSnapshot structureSnapshot, StructureActionCause cause,
        StructureActionType actionType, IPlayer responsible, double time,
        boolean skipAnimation, Cuboid newCuboid)

    {
        return new StructureEventToggleStart(
            structure, structureSnapshot, cause, actionType, responsible, time, skipAnimation, newCuboid);
    }

    @Override
    public IStructureEventToggleEnd createToggleEndEvent(
        AbstractStructure structure, StructureSnapshot snapshot, StructureActionCause cause,
        StructureActionType actionType,
        IPlayer responsible, double time, boolean skipAnimation)
    {
        return new StructureEventToggleEnd(structure, snapshot, cause, actionType, responsible, time, skipAnimation);
    }
}