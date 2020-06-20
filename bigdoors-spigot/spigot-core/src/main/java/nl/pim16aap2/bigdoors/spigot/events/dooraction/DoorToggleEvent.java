package nl.pim16aap2.bigdoors.spigot.events.dooraction;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

abstract class DoorToggleEvent extends Event implements IDoorEvent
{
    /**
     * The UID of the door this action will be applied to.
     */
    protected final CompletableFuture<Optional<AbstractDoorBase>> futureDoor;

    /**
     * What initiated this DoorAction event.
     */
    @NotNull
    protected final DoorActionCause cause;
    @NotNull
    protected final DoorActionType actionType;
    @NotNull
    protected final Optional<IPPlayer> responsible;
    protected boolean isCancelled = false;
    protected final double time;
    protected final boolean skipAnimation;

    DoorToggleEvent(final @NotNull CompletableFuture<Optional<AbstractDoorBase>> futureDoor,
                    final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType,
                    final @Nullable IPPlayer responsible, final double time, final boolean skipAnimation)
    {
        super(true);
        this.futureDoor = futureDoor;
        this.cause = cause;
        this.actionType = actionType;
        this.responsible = Optional.ofNullable(responsible);
        this.time = time;
        this.skipAnimation = skipAnimation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public CompletableFuture<Optional<AbstractDoorBase>> getDoor()
    {
        return futureDoor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public DoorActionCause getCause()
    {
        return cause;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<IPPlayer> getResponsible()
    {
        return responsible;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public DoorActionType getActionType()
    {
        return actionType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean skipAnimation()
    {
        return skipAnimation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getTime()
    {
        return time;
    }
}
