package nl.pim16aap2.bigdoors.spigot.factories;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.IBigDoorsEventFactory;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
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
import nl.pim16aap2.bigdoors.util.DoorOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BigDoorsEventFactorySpigot implements IBigDoorsEventFactory
{
    @Override
    public @NotNull IDoorCreatedEvent createDoorCreatedEvent(final @NotNull AbstractDoor preview,
                                                             final @Nullable IPPlayer responsible)
    {
        return new DoorCreatedEvent(preview, responsible);
    }

    @Override
    public @NotNull IDoorPrepareCreateEvent createPrepareDoorCreateEvent(final @NotNull AbstractDoor door,
                                                                         final @Nullable IPPlayer responsible)
    {
        return new DoorPrepareCreateEvent(door, responsible);
    }

    @Override
    public @NotNull IDoorPrepareDeleteEvent createPrepareDeleteDoorEvent(final @NotNull AbstractDoor door,
                                                                         final @Nullable IPPlayer responsible)
    {
        return new DoorPrepareDeleteEvent(door, responsible);
    }

    @Override
    public @NotNull IDoorPrepareAddOwnerEvent createDoorPrepareAddOwnerEvent(
        final @NotNull AbstractDoor door, final @NotNull DoorOwner newOwner, final @Nullable IPPlayer responsible)
    {
        return new DoorPrepareAddOwnerEvent(door, responsible, newOwner);
    }

    @Override
    public @NotNull IDoorPrepareRemoveOwnerEvent createDoorPrepareRemoveOwnerEvent(
        final @NotNull AbstractDoor door, final @NotNull DoorOwner removedOwner,
        final @Nullable IPPlayer responsible)
    {
        return new DoorPrepareRemoveOwnerEvent(door, responsible, removedOwner);
    }

    @Override
    public @NotNull IDoorPrepareLockChangeEvent createDoorPrepareLockChangeEvent(
        final @NotNull AbstractDoor door, final boolean newLockStatus, final @Nullable IPPlayer responsible)
    {
        return new DoorPrepareLockChangeEvent(door, responsible, newLockStatus);
    }

    @Override
    public @NotNull IDoorEventTogglePrepare createTogglePrepareEvent(final @NotNull AbstractDoor door,
                                                                     final @NotNull DoorActionCause cause,
                                                                     final @NotNull DoorActionType actionType,
                                                                     final @NotNull IPPlayer responsible,
                                                                     final double time,
                                                                     final boolean skipAnimation,
                                                                     final @NotNull Cuboid newCuboid)
    {
        return new DoorEventTogglePrepare(door, cause, actionType, responsible, time, skipAnimation, newCuboid);
    }

    @Override
    public @NotNull IDoorEventToggleStart createToggleStartEvent(final @NotNull AbstractDoor door,
                                                                 final @NotNull DoorActionCause cause,
                                                                 final @NotNull DoorActionType actionType,
                                                                 final @NotNull IPPlayer responsible, final double time,
                                                                 final boolean skipAnimation,
                                                                 final @NotNull Cuboid newCuboid)

    {
        return new DoorEventToggleStart(door, cause, actionType, responsible, time, skipAnimation, newCuboid);
    }

    @Override
    public @NotNull IDoorEventToggleEnd createToggleEndEvent(final @NotNull AbstractDoor door,
                                                             final @NotNull DoorActionCause cause,
                                                             final @NotNull DoorActionType actionType,
                                                             final @NotNull IPPlayer responsible, final double time,
                                                             final boolean skipAnimation)
    {
        return new DoorEventToggleEnd(door, cause, actionType, responsible, time, skipAnimation);
    }
}
