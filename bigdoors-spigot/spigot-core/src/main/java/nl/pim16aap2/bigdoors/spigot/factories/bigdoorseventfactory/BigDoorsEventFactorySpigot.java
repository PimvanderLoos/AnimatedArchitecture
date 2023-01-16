package nl.pim16aap2.bigdoors.spigot.factories.bigdoorseventfactory;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.IBigDoorsEventFactory;
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
import nl.pim16aap2.bigdoors.spigot.events.MovableCreatedEvent;
import nl.pim16aap2.bigdoors.spigot.events.MovablePrepareAddOwnerEvent;
import nl.pim16aap2.bigdoors.spigot.events.MovablePrepareCreateEvent;
import nl.pim16aap2.bigdoors.spigot.events.MovablePrepareDeleteEvent;
import nl.pim16aap2.bigdoors.spigot.events.MovablePrepareLockChangeEvent;
import nl.pim16aap2.bigdoors.spigot.events.MovablePrepareRemoveOwnerEvent;
import nl.pim16aap2.bigdoors.spigot.events.dooraction.MovableEventToggleEnd;
import nl.pim16aap2.bigdoors.spigot.events.dooraction.MovableEventTogglePrepare;
import nl.pim16aap2.bigdoors.spigot.events.dooraction.MovableEventToggleStart;
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
    public IMovableCreatedEvent createMovableCreatedEvent(AbstractMovable preview, @Nullable IPPlayer responsible)
    {
        return new MovableCreatedEvent(preview, responsible);
    }

    @Override
    public IMovablePrepareCreateEvent createPrepareMovableCreateEvent(
        AbstractMovable movable, @Nullable IPPlayer responsible)
    {
        return new MovablePrepareCreateEvent(movable, responsible);
    }

    @Override
    public IMovablePrepareDeleteEvent createPrepareDeleteMovableEvent(
        AbstractMovable movable, @Nullable IPPlayer responsible)
    {
        return new MovablePrepareDeleteEvent(movable, responsible);
    }

    @Override
    public IMovablePrepareAddOwnerEvent createMovablePrepareAddOwnerEvent(
        AbstractMovable movable, MovableOwner newOwner,
        @Nullable IPPlayer responsible)
    {
        return new MovablePrepareAddOwnerEvent(movable, responsible, newOwner);
    }

    @Override
    public IMovablePrepareRemoveOwnerEvent createMovablePrepareRemoveOwnerEvent(
        AbstractMovable movable, MovableOwner removedOwner,
        @Nullable IPPlayer responsible)
    {
        return new MovablePrepareRemoveOwnerEvent(movable, responsible, removedOwner);
    }

    @Override
    public IMovablePrepareLockChangeEvent createMovablePrepareLockChangeEvent(
        AbstractMovable movable, boolean newLockStatus,
        @Nullable IPPlayer responsible)
    {
        return new MovablePrepareLockChangeEvent(movable, responsible, newLockStatus);
    }

    @Override
    public IMovableEventTogglePrepare createTogglePrepareEvent(
        MovableSnapshot snapshot, MovableActionCause cause,
        MovableActionType actionType, IPPlayer responsible,
        double time, boolean skipAnimation, Cuboid newCuboid)
    {
        return new MovableEventTogglePrepare(snapshot, cause, actionType, responsible, time, skipAnimation, newCuboid);
    }

    @Override
    public IMovableEventToggleStart createToggleStartEvent(
        AbstractMovable movable, MovableSnapshot movableSnapshot, MovableActionCause cause,
        MovableActionType actionType, IPPlayer responsible, double time,
        boolean skipAnimation, Cuboid newCuboid)

    {
        return new MovableEventToggleStart(
            movable, movableSnapshot, cause, actionType, responsible, time, skipAnimation, newCuboid);
    }

    @Override
    public IMovableEventToggleEnd createToggleEndEvent(
        AbstractMovable movable, MovableSnapshot snapshot, MovableActionCause cause,
        MovableActionType actionType,
        IPPlayer responsible, double time, boolean skipAnimation)
    {
        return new MovableEventToggleEnd(movable, snapshot, cause, actionType, responsible, time, skipAnimation);
    }
}
