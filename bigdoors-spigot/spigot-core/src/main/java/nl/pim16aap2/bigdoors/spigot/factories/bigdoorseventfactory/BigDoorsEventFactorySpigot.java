package nl.pim16aap2.bigdoors.spigot.factories.bigdoorseventfactory;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.IBigDoorsEventFactory;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorOwner;
import nl.pim16aap2.bigdoors.doors.IDoorConst;
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
import nl.pim16aap2.bigdoors.spigot.events.DoorCreatedEvent;
import nl.pim16aap2.bigdoors.spigot.events.DoorPrepareAddOwnerEvent;
import nl.pim16aap2.bigdoors.spigot.events.DoorPrepareCreateEvent;
import nl.pim16aap2.bigdoors.spigot.events.DoorPrepareDeleteEvent;
import nl.pim16aap2.bigdoors.spigot.events.DoorPrepareLockChangeEvent;
import nl.pim16aap2.bigdoors.spigot.events.DoorPrepareRemoveOwnerEvent;
import nl.pim16aap2.bigdoors.spigot.events.dooraction.DoorEventToggleEnd;
import nl.pim16aap2.bigdoors.spigot.events.dooraction.DoorEventTogglePrepare;
import nl.pim16aap2.bigdoors.spigot.events.dooraction.DoorEventToggleStart;
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
    public IDoorCreatedEvent createDoorCreatedEvent(AbstractDoor preview, @Nullable IPPlayer responsible)
    {
        return new DoorCreatedEvent(preview, responsible);
    }

    @Override
    public IDoorPrepareCreateEvent createPrepareDoorCreateEvent(AbstractDoor door, @Nullable IPPlayer responsible)
    {
        return new DoorPrepareCreateEvent(door, responsible);
    }

    @Override
    public IDoorPrepareDeleteEvent createPrepareDeleteDoorEvent(AbstractDoor door, @Nullable IPPlayer responsible)
    {
        return new DoorPrepareDeleteEvent(door, responsible);
    }

    @Override
    public IDoorPrepareAddOwnerEvent createDoorPrepareAddOwnerEvent(
        AbstractDoor door, DoorOwner newOwner,
        @Nullable IPPlayer responsible)
    {
        return new DoorPrepareAddOwnerEvent(door, responsible, newOwner);
    }

    @Override
    public IDoorPrepareRemoveOwnerEvent createDoorPrepareRemoveOwnerEvent(
        AbstractDoor door, DoorOwner removedOwner,
        @Nullable IPPlayer responsible)
    {
        return new DoorPrepareRemoveOwnerEvent(door, responsible, removedOwner);
    }

    @Override
    public IDoorPrepareLockChangeEvent createDoorPrepareLockChangeEvent(
        AbstractDoor door, boolean newLockStatus,
        @Nullable IPPlayer responsible)
    {
        return new DoorPrepareLockChangeEvent(door, responsible, newLockStatus);
    }

    @Override
    public IDoorEventTogglePrepare createTogglePrepareEvent(
        IDoorConst door, DoorActionCause cause,
        DoorActionType actionType, IPPlayer responsible,
        double time, boolean skipAnimation, Cuboid newCuboid)
    {
        return new DoorEventTogglePrepare(door, cause, actionType, responsible, time, skipAnimation, newCuboid);
    }

    @Override
    public IDoorEventToggleStart createToggleStartEvent(
        AbstractDoor door, DoorActionCause cause,
        DoorActionType actionType, IPPlayer responsible, double time,
        boolean skipAnimation, Cuboid newCuboid)

    {
        return new DoorEventToggleStart(door, cause, actionType, responsible, time, skipAnimation, newCuboid);
    }

    @Override
    public IDoorEventToggleEnd createToggleEndEvent(
        AbstractDoor door, DoorActionCause cause, DoorActionType actionType,
        IPPlayer responsible, double time, boolean skipAnimation)
    {
        return new DoorEventToggleEnd(door, cause, actionType, responsible, time, skipAnimation);
    }
}
