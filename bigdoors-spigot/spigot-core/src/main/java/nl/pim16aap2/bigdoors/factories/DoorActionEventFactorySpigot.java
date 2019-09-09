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
    public @NotNull IDoorActionEvent create(final @NotNull CompletableFuture<Optional<AbstractDoorBase>> futureDoor,
                                            final @NotNull DoorActionCause cause,
                                            final @NotNull DoorActionType actionType,
                                            final @Nullable IPPlayer responsible, final double time,
                                            final boolean skipAnimation)
    {
        return new DoorActionEventSpigot(futureDoor, cause, actionType, responsible, time, skipAnimation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull IDoorActionEvent create(final @NotNull CompletableFuture<Optional<AbstractDoorBase>> futureDoor,
                                            final @NotNull DoorActionCause cause,
                                            final @NotNull DoorActionType actionType,
                                            final @Nullable IPPlayer responsible, final double time)
    {
        return new DoorActionEventSpigot(futureDoor, cause, actionType, responsible, time);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull IDoorActionEvent create(final @NotNull CompletableFuture<Optional<AbstractDoorBase>> futureDoor,
                                            final @NotNull DoorActionCause cause,
                                            final @NotNull DoorActionType actionType,
                                            final @Nullable IPPlayer responsible, final boolean skipAnimation)
    {
        return new DoorActionEventSpigot(futureDoor, cause, actionType, responsible, skipAnimation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull IDoorActionEvent create(final @NotNull CompletableFuture<Optional<AbstractDoorBase>> futureDoor,
                                            final @NotNull DoorActionCause cause,
                                            final @NotNull DoorActionType actionType,
                                            final @Nullable IPPlayer responsible)
    {
        return new DoorActionEventSpigot(futureDoor, cause, actionType, responsible);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull IDoorActionEvent create(final long doorUID, final @NotNull DoorActionCause cause,
                                            final @NotNull DoorActionType actionType,
                                            final @Nullable IPPlayer responsible, final double time,
                                            boolean skipAnimation)
    {
        return new DoorActionEventSpigot(doorUID, cause, actionType, responsible, time, skipAnimation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull IDoorActionEvent create(final long doorUID, final @NotNull DoorActionCause cause,
                                            final @NotNull DoorActionType actionType,
                                            final @Nullable IPPlayer responsible, final double time)
    {
        return new DoorActionEventSpigot(doorUID, cause, actionType, responsible, time);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull IDoorActionEvent create(final long doorUID, final @NotNull DoorActionCause cause,
                                            final @NotNull DoorActionType actionType,
                                            final @Nullable IPPlayer responsible, final boolean skipAnimation)
    {
        return new DoorActionEventSpigot(doorUID, cause, actionType, responsible, skipAnimation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull IDoorActionEvent create(final long doorUID, final @NotNull DoorActionCause cause,
                                            final @NotNull DoorActionType actionType,
                                            final @Nullable IPPlayer responsible)
    {
        return new DoorActionEventSpigot(doorUID, cause, actionType, responsible);
    }
}
