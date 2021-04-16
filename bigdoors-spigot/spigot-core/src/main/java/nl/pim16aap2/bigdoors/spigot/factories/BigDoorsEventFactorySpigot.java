package nl.pim16aap2.bigdoors.spigot.factories;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.IBigDoorsEventFactory;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
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
import nl.pim16aap2.bigdoors.util.CuboidConst;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import org.jetbrains.annotations.Nullable;

public class BigDoorsEventFactorySpigot implements IBigDoorsEventFactory
{
    @Override
    public @NonNull IDoorCreatedEvent createDoorCreatedEvent(final @NonNull AbstractDoorBase preview,
                                                             final @Nullable IPPlayer responsible)
    {
        return new DoorCreatedEvent(preview, responsible);
    }

    @Override
    public @NonNull IDoorPrepareCreateEvent createPrepareDoorCreateEvent(final @NonNull AbstractDoorBase door,
                                                                         final @Nullable IPPlayer responsible)
    {
        return new DoorPrepareCreateEvent(door, responsible);
    }

    @Override
    public @NonNull IDoorPrepareDeleteEvent createPrepareDeleteDoorEvent(final @NonNull AbstractDoorBase door,
                                                                         final @Nullable IPPlayer responsible)
    {
        return new DoorPrepareDeleteEvent(door, responsible);
    }

    @Override
    public @NonNull IDoorPrepareAddOwnerEvent createDoorPrepareAddOwnerEvent(
        final @NonNull AbstractDoorBase door, final @NonNull DoorOwner newOwner, final @Nullable IPPlayer responsible)
    {
        return new DoorPrepareAddOwnerEvent(door, responsible, newOwner);
    }

    @Override
    public @NonNull IDoorPrepareRemoveOwnerEvent createDoorPrepareRemoveOwnerEvent(
        final @NonNull AbstractDoorBase door, final @NonNull DoorOwner removedOwner,
        final @Nullable IPPlayer responsible)
    {
        return new DoorPrepareRemoveOwnerEvent(door, responsible, removedOwner);
    }

    @Override
    public @NonNull IDoorPrepareLockChangeEvent createDoorPrepareLockChangeEvent(
        final @NonNull AbstractDoorBase door, final boolean newLockStatus, final @Nullable IPPlayer responsible)
    {
        return new DoorPrepareLockChangeEvent(door, responsible, newLockStatus);
    }

    @Override
    public @NonNull IDoorEventTogglePrepare createPrepareEvent(final @NonNull AbstractDoorBase door,
                                                               final @NonNull DoorActionCause cause,
                                                               final @NonNull DoorActionType actionType,
                                                               final @NonNull IPPlayer responsible, final double time,
                                                               final boolean skipAnimation,
                                                               final @NonNull CuboidConst newCuboid)
    {
        return new DoorEventTogglePrepare(door, cause, actionType, responsible, time, skipAnimation, newCuboid);
    }

    @Override
    public @NonNull IDoorEventToggleStart createStartEvent(final @NonNull AbstractDoorBase door,
                                                           final @NonNull DoorActionCause cause,
                                                           final @NonNull DoorActionType actionType,
                                                           final @NonNull IPPlayer responsible, final double time,
                                                           final boolean skipAnimation,
                                                           final @NonNull CuboidConst newCuboid)

    {
        return new DoorEventToggleStart(door, cause, actionType, responsible, time, skipAnimation, newCuboid);
    }

    @Override
    public @NonNull IDoorEventToggleEnd createEndEvent(final @NonNull AbstractDoorBase door,
                                                       final @NonNull DoorActionCause cause,
                                                       final @NonNull DoorActionType actionType,
                                                       final @NonNull IPPlayer responsible, final double time,
                                                       final boolean skipAnimation)
    {
        return new DoorEventToggleEnd(door, cause, actionType, responsible, time, skipAnimation);
    }
}
