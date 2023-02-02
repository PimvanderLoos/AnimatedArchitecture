package nl.pim16aap2.bigdoors.spigot.factories.bigdoorseventfactory;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.IBigDoorsEventFactory;
import nl.pim16aap2.bigdoors.events.IStructureCreatedEvent;
import nl.pim16aap2.bigdoors.events.IStructurePrepareAddOwnerEvent;
import nl.pim16aap2.bigdoors.events.IStructurePrepareCreateEvent;
import nl.pim16aap2.bigdoors.events.IStructurePrepareDeleteEvent;
import nl.pim16aap2.bigdoors.events.IStructurePrepareLockChangeEvent;
import nl.pim16aap2.bigdoors.events.IStructurePrepareRemoveOwnerEvent;
import nl.pim16aap2.bigdoors.events.structureaction.IStructureEventToggleEnd;
import nl.pim16aap2.bigdoors.events.structureaction.IStructureEventTogglePrepare;
import nl.pim16aap2.bigdoors.events.structureaction.IStructureEventToggleStart;
import nl.pim16aap2.bigdoors.events.structureaction.StructureActionCause;
import nl.pim16aap2.bigdoors.events.structureaction.StructureActionType;
import nl.pim16aap2.bigdoors.spigot.events.StructureCreatedEvent;
import nl.pim16aap2.bigdoors.spigot.events.StructurePrepareAddOwnerEvent;
import nl.pim16aap2.bigdoors.spigot.events.StructurePrepareCreateEvent;
import nl.pim16aap2.bigdoors.spigot.events.StructurePrepareDeleteEvent;
import nl.pim16aap2.bigdoors.spigot.events.StructurePrepareLockChangeEvent;
import nl.pim16aap2.bigdoors.spigot.events.StructurePrepareRemoveOwnerEvent;
import nl.pim16aap2.bigdoors.spigot.events.structureaction.StructureEventToggleEnd;
import nl.pim16aap2.bigdoors.spigot.events.structureaction.StructureEventTogglePrepare;
import nl.pim16aap2.bigdoors.spigot.events.structureaction.StructureEventToggleStart;
import nl.pim16aap2.bigdoors.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.structures.StructureOwner;
import nl.pim16aap2.bigdoors.structures.StructureSnapshot;
import nl.pim16aap2.bigdoors.util.Cuboid;
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
    public IStructureCreatedEvent createStructureCreatedEvent(AbstractStructure preview, @Nullable IPPlayer responsible)
    {
        return new StructureCreatedEvent(preview, responsible);
    }

    @Override
    public IStructurePrepareCreateEvent createPrepareStructureCreateEvent(
        AbstractStructure structure, @Nullable IPPlayer responsible)
    {
        return new StructurePrepareCreateEvent(structure, responsible);
    }

    @Override
    public IStructurePrepareDeleteEvent createPrepareDeleteStructureEvent(
        AbstractStructure structure, @Nullable IPPlayer responsible)
    {
        return new StructurePrepareDeleteEvent(structure, responsible);
    }

    @Override
    public IStructurePrepareAddOwnerEvent createStructurePrepareAddOwnerEvent(
        AbstractStructure structure, StructureOwner newOwner,
        @Nullable IPPlayer responsible)
    {
        return new StructurePrepareAddOwnerEvent(structure, responsible, newOwner);
    }

    @Override
    public IStructurePrepareRemoveOwnerEvent createStructurePrepareRemoveOwnerEvent(
        AbstractStructure structure, StructureOwner removedOwner,
        @Nullable IPPlayer responsible)
    {
        return new StructurePrepareRemoveOwnerEvent(structure, responsible, removedOwner);
    }

    @Override
    public IStructurePrepareLockChangeEvent createStructurePrepareLockChangeEvent(
        AbstractStructure structure, boolean newLockStatus,
        @Nullable IPPlayer responsible)
    {
        return new StructurePrepareLockChangeEvent(structure, responsible, newLockStatus);
    }

    @Override
    public IStructureEventTogglePrepare createTogglePrepareEvent(
        StructureSnapshot snapshot, StructureActionCause cause,
        StructureActionType actionType, IPPlayer responsible,
        double time, boolean skipAnimation, Cuboid newCuboid)
    {
        return new StructureEventTogglePrepare(snapshot, cause, actionType, responsible, time, skipAnimation,
                                               newCuboid);
    }

    @Override
    public IStructureEventToggleStart createToggleStartEvent(
        AbstractStructure structure, StructureSnapshot structureSnapshot, StructureActionCause cause,
        StructureActionType actionType, IPPlayer responsible, double time,
        boolean skipAnimation, Cuboid newCuboid)

    {
        return new StructureEventToggleStart(
            structure, structureSnapshot, cause, actionType, responsible, time, skipAnimation, newCuboid);
    }

    @Override
    public IStructureEventToggleEnd createToggleEndEvent(
        AbstractStructure structure, StructureSnapshot snapshot, StructureActionCause cause,
        StructureActionType actionType,
        IPPlayer responsible, double time, boolean skipAnimation)
    {
        return new StructureEventToggleEnd(structure, snapshot, cause, actionType, responsible, time, skipAnimation);
    }
}
