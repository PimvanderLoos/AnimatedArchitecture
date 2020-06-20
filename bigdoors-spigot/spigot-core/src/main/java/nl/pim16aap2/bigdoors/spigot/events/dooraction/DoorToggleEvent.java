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

abstract class DoorToggleEvent extends Event implements IDoorEvent
{
    /**
     * The door this action will be applied to.
     */
    protected final AbstractDoorBase door;

    /**
     * What initiated this DoorAction event.
     */
    @NotNull
    protected final DoorActionCause cause;

    /**
     * The type of action that is requested.
     */
    @NotNull
    protected final DoorActionType actionType;

    /**
     * The {@link IPPlayer} that is held responsible for this action. This is either the player that iniatiated the
     * action (e.g. via a command), or the original creator of the door, in case the initiator is not available (e.g.
     * redstone).
     */
    @NotNull
    protected final Optional<IPPlayer> responsible;
    protected boolean isCancelled = false;
    protected final double time;
    protected final boolean skipAnimation;

    DoorToggleEvent(final @NotNull AbstractDoorBase door, final @NotNull DoorActionCause cause,
                    final @NotNull DoorActionType actionType, final @Nullable IPPlayer responsible, final double time,
                    final boolean skipAnimation)
    {
        super(false);
        this.door = door;
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
    public AbstractDoorBase getDoor()
    {
        return door;
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
