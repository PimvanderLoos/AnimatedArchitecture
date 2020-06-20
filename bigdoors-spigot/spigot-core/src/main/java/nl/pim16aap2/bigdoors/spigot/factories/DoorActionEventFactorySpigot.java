package nl.pim16aap2.bigdoors.spigot.factories;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.IDoorActionEventFactory;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.events.dooraction.DoorEventType;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEvent;
import nl.pim16aap2.bigdoors.spigot.events.dooraction.DoorEventToggleEnd;
import nl.pim16aap2.bigdoors.spigot.events.dooraction.DoorEventTogglePrepare;
import nl.pim16aap2.bigdoors.spigot.events.dooraction.DoorEventToggleStart;
import nl.pim16aap2.bigdoors.util.PLogger;
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
    public IDoorEvent create(final @NotNull DoorEventType doorEventType,
                             final @NotNull CompletableFuture<Optional<AbstractDoorBase>> futureDoor,
                             final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                             final @Nullable IPPlayer responsible, final double time, final boolean skipAnimation)
    {
        switch (doorEventType)
        {
            case PREPARE:
                return new DoorEventTogglePrepare(futureDoor, cause, actionType, responsible, time, skipAnimation);
            case START:
                return new DoorEventToggleStart(futureDoor, cause, actionType, responsible, time, skipAnimation);
            case END:
                return new DoorEventToggleEnd(futureDoor, cause, actionType, responsible, time, skipAnimation);
            default:
                IllegalArgumentException exception = new IllegalArgumentException(
                    "Failed to create event of type: " + doorEventType);
                PLogger.get().logException(exception);
                throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IDoorEvent create(final @NotNull DoorEventType doorEventType,
                             final @NotNull CompletableFuture<Optional<AbstractDoorBase>> futureDoor,
                             final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                             final @Nullable IPPlayer responsible, final double time)
    {
        switch (doorEventType)
        {
            case PREPARE:
                return new DoorEventTogglePrepare(futureDoor, cause, actionType, responsible, time);
            case START:
                return new DoorEventToggleStart(futureDoor, cause, actionType, responsible, time);
            case END:
                return new DoorEventToggleEnd(futureDoor, cause, actionType, responsible, time);
            default:
                IllegalArgumentException exception = new IllegalArgumentException(
                    "Failed to create event of type: " + doorEventType);
                PLogger.get().logException(exception);
                throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IDoorEvent create(final @NotNull DoorEventType doorEventType,
                             final @NotNull CompletableFuture<Optional<AbstractDoorBase>> futureDoor,
                             final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                             final @Nullable IPPlayer responsible, final boolean skipAnimation)
    {
        switch (doorEventType)
        {
            case PREPARE:
                return new DoorEventTogglePrepare(futureDoor, cause, actionType, responsible, skipAnimation);
            case START:
                return new DoorEventToggleStart(futureDoor, cause, actionType, responsible, skipAnimation);
            case END:
                return new DoorEventToggleEnd(futureDoor, cause, actionType, responsible, skipAnimation);
            default:
                IllegalArgumentException exception = new IllegalArgumentException(
                    "Failed to create event of type: " + doorEventType);
                PLogger.get().logException(exception);
                throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IDoorEvent create(final @NotNull DoorEventType doorEventType,
                             final @NotNull CompletableFuture<Optional<AbstractDoorBase>> futureDoor,
                             final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                             final @Nullable IPPlayer responsible)
    {
        switch (doorEventType)
        {
            case PREPARE:
                return new DoorEventTogglePrepare(futureDoor, cause, actionType, responsible);
            case START:
                return new DoorEventToggleStart(futureDoor, cause, actionType, responsible);
            case END:
                return new DoorEventToggleEnd(futureDoor, cause, actionType, responsible);
            default:
                IllegalArgumentException exception = new IllegalArgumentException(
                    "Failed to create event of type: " + doorEventType);
                PLogger.get().logException(exception);
                throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IDoorEvent create(final @NotNull DoorEventType doorEventType, final long doorUID,
                             final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                             final @Nullable IPPlayer responsible, final double time, boolean skipAnimation)
    {
        switch (doorEventType)
        {
            case PREPARE:
                return new DoorEventTogglePrepare(doorUID, cause, actionType, responsible, time, skipAnimation);
            case START:
                return new DoorEventToggleStart(doorUID, cause, actionType, responsible, time, skipAnimation);
            case END:
                return new DoorEventToggleEnd(doorUID, cause, actionType, responsible, time, skipAnimation);
            default:
                IllegalArgumentException exception = new IllegalArgumentException(
                    "Failed to create event of type: " + doorEventType);
                PLogger.get().logException(exception);
                throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IDoorEvent create(final @NotNull DoorEventType doorEventType, final long doorUID,
                             final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                             final @Nullable IPPlayer responsible, final double time)
    {
        switch (doorEventType)
        {
            case PREPARE:
                return new DoorEventTogglePrepare(doorUID, cause, actionType, responsible, time);
            case START:
                return new DoorEventToggleStart(doorUID, cause, actionType, responsible, time);
            case END:
                return new DoorEventToggleEnd(doorUID, cause, actionType, responsible, time);
            default:
                IllegalArgumentException exception = new IllegalArgumentException(
                    "Failed to create event of type: " + doorEventType);
                PLogger.get().logException(exception);
                throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IDoorEvent create(final @NotNull DoorEventType doorEventType, final long doorUID,
                             final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                             final @Nullable IPPlayer responsible, final boolean skipAnimation)
    {
        switch (doorEventType)
        {
            case PREPARE:
                return new DoorEventTogglePrepare(doorUID, cause, actionType, responsible, skipAnimation);
            case START:
                return new DoorEventToggleStart(doorUID, cause, actionType, responsible, skipAnimation);
            case END:
                return new DoorEventToggleEnd(doorUID, cause, actionType, responsible, skipAnimation);
            default:
                IllegalArgumentException exception = new IllegalArgumentException(
                    "Failed to create event of type: " + doorEventType);
                PLogger.get().logException(exception);
                throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IDoorEvent create(final @NotNull DoorEventType doorEventType, final long doorUID,
                             final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                             final @Nullable IPPlayer responsible)
    {
        switch (doorEventType)
        {
            case PREPARE:
                return new DoorEventTogglePrepare(doorUID, cause, actionType, responsible);
            case START:
                return new DoorEventToggleStart(doorUID, cause, actionType, responsible);
            case END:
                return new DoorEventToggleEnd(doorUID, cause, actionType, responsible);
            default:
                IllegalArgumentException exception = new IllegalArgumentException(
                    "Failed to create event of type: " + doorEventType);
                PLogger.get().logException(exception);
                throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IDoorEvent create(final @NotNull DoorEventType doorEventType, final @NotNull AbstractDoorBase door,
                             final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                             final @Nullable IPPlayer responsible, final double time, boolean skipAnimation)
    {
        // TODO: Don't do this stuff.
        switch (doorEventType)
        {
            case PREPARE:
                return new DoorEventTogglePrepare(door.getDoorUID(), cause, actionType, responsible, time,
                                                  skipAnimation);
            case START:
                return new DoorEventToggleStart(door.getDoorUID(), cause, actionType, responsible, time, skipAnimation);
            case END:
                return new DoorEventToggleEnd(door.getDoorUID(), cause, actionType, responsible, time, skipAnimation);
            default:
                IllegalArgumentException exception = new IllegalArgumentException(
                    "Failed to create event of type: " + doorEventType);
                PLogger.get().logException(exception);
                throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IDoorEvent create(final @NotNull DoorEventType doorEventType, final @NotNull AbstractDoorBase door,
                             final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                             final @Nullable IPPlayer responsible, final double time)
    {
        // TODO: Don't just get the door UID from the door only to get the door again.
        switch (doorEventType)
        {
            case PREPARE:
                return new DoorEventTogglePrepare(door.getDoorUID(), cause, actionType, responsible, time);
            case START:
                return new DoorEventToggleStart(door.getDoorUID(), cause, actionType, responsible, time);
            case END:
                return new DoorEventToggleEnd(door.getDoorUID(), cause, actionType, responsible, time);
            default:
                IllegalArgumentException exception = new IllegalArgumentException(
                    "Failed to create event of type: " + doorEventType);
                PLogger.get().logException(exception);
                throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IDoorEvent create(final @NotNull DoorEventType doorEventType, final @NotNull AbstractDoorBase door,
                             final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                             final @Nullable IPPlayer responsible, final boolean skipAnimation)
    {
        // TODO: Don't just get the door UID from the door only to get the door again.
        switch (doorEventType)
        {
            case PREPARE:
                return new DoorEventTogglePrepare(door.getDoorUID(), cause, actionType, responsible, skipAnimation);
            case START:
                return new DoorEventToggleStart(door.getDoorUID(), cause, actionType, responsible, skipAnimation);
            case END:
                return new DoorEventToggleEnd(door.getDoorUID(), cause, actionType, responsible, skipAnimation);
            default:
                IllegalArgumentException exception = new IllegalArgumentException(
                    "Failed to create event of type: " + doorEventType);
                PLogger.get().logException(exception);
                throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public IDoorEvent create(final @NotNull DoorEventType doorEventType, final @NotNull AbstractDoorBase door,
                             final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                             final @Nullable IPPlayer responsible)
    {
        // TODO: Don't just get the door UID from the door only to get the door again.
        switch (doorEventType)
        {
            case PREPARE:
                return new DoorEventTogglePrepare(door.getDoorUID(), cause, actionType, responsible);
            case START:
                return new DoorEventToggleStart(door.getDoorUID(), cause, actionType, responsible);
            case END:
                return new DoorEventToggleEnd(door.getDoorUID(), cause, actionType, responsible);
            default:
                IllegalArgumentException exception = new IllegalArgumentException(
                    "Failed to create event of type: " + doorEventType);
                PLogger.get().logException(exception);
                throw exception;
        }
    }
}
