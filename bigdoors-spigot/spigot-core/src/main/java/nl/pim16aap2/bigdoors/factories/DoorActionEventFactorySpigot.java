package nl.pim16aap2.bigdoors.factories;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.IDoorActionEventFactory;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionEventSpigot;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class DoorActionEventFactorySpigot implements IDoorActionEventFactory
{
    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IDoorActionEvent create(final @NotNull CompletableFuture<Optional<AbstractDoorBase>> futureDoor,
                                   final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                                   final @Nullable IPPlayer responsible, final double time, final boolean skipAnimation)
    {
        return new DoorActionEventSpigot(futureDoor, cause, actionType, responsible, time, skipAnimation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IDoorActionEvent create(final @NotNull CompletableFuture<Optional<AbstractDoorBase>> futureDoor,
                                   final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                                   final @Nullable IPPlayer responsible, final double time)
    {
        return new DoorActionEventSpigot(futureDoor, cause, actionType, responsible, time);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IDoorActionEvent create(final @NotNull CompletableFuture<Optional<AbstractDoorBase>> futureDoor,
                                   final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                                   final @Nullable IPPlayer responsible, final boolean skipAnimation)
    {
        return new DoorActionEventSpigot(futureDoor, cause, actionType, responsible, skipAnimation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IDoorActionEvent create(final @NotNull CompletableFuture<Optional<AbstractDoorBase>> futureDoor,
                                   final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                                   final @Nullable IPPlayer responsible)
    {
        return new DoorActionEventSpigot(futureDoor, cause, actionType, responsible);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IDoorActionEvent create(final long doorUID, final @NotNull DoorActionCause cause,
                                   final @NotNull DoorActionType actionType, final @Nullable IPPlayer responsible,
                                   final double time, boolean skipAnimation)
    {
        return new DoorActionEventSpigot(doorUID, cause, actionType, responsible, time, skipAnimation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IDoorActionEvent create(final long doorUID, final @NotNull DoorActionCause cause,
                                   final @NotNull DoorActionType actionType, final @Nullable IPPlayer responsible,
                                   final double time)
    {
        return new DoorActionEventSpigot(doorUID, cause, actionType, responsible, time);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IDoorActionEvent create(final long doorUID, final @NotNull DoorActionCause cause,
                                   final @NotNull DoorActionType actionType, final @Nullable IPPlayer responsible,
                                   final boolean skipAnimation)
    {
        return new DoorActionEventSpigot(doorUID, cause, actionType, responsible, skipAnimation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IDoorActionEvent create(final long doorUID, final @NotNull DoorActionCause cause,
                                   final @NotNull DoorActionType actionType, final @Nullable IPPlayer responsible)
    {
        return new DoorActionEventSpigot(doorUID, cause, actionType, responsible);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IDoorActionEvent create(final @NotNull AbstractDoorBase door, final @NotNull DoorActionCause cause,
                                   final @NotNull DoorActionType actionType, final @Nullable IPPlayer responsible,
                                   final double time, boolean skipAnimation)
    {
        // TODO: Don't do this stuff.
        return new DoorActionEventSpigot(door.getDoorUID(), cause, actionType, responsible, time, skipAnimation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IDoorActionEvent create(final @NotNull AbstractDoorBase door, final @NotNull DoorActionCause cause,
                                   final @NotNull DoorActionType actionType, final @Nullable IPPlayer responsible,
                                   final double time)
    {
        // TODO: Don't just get the door UID from the door only to get the door again.
        return new DoorActionEventSpigot(door.getDoorUID(), cause, actionType, responsible, time);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IDoorActionEvent create(final @NotNull AbstractDoorBase door, final @NotNull DoorActionCause cause,
                                   final @NotNull DoorActionType actionType, final @Nullable IPPlayer responsible,
                                   final boolean skipAnimation)
    {
        // TODO: Don't just get the door UID from the door only to get the door again.
        return new DoorActionEventSpigot(door.getDoorUID(), cause, actionType, responsible, skipAnimation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IDoorActionEvent create(final @NotNull AbstractDoorBase door, final @NotNull DoorActionCause cause,
                                   final @NotNull DoorActionType actionType, final @Nullable IPPlayer responsible)
    {
        // TODO: Don't just get the door UID from the door only to get the door again.
        return new DoorActionEventSpigot(door.getDoorUID(), cause, actionType, responsible);
    }
}
