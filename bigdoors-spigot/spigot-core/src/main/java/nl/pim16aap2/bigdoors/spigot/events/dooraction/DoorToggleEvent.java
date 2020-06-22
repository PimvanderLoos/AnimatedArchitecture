package nl.pim16aap2.bigdoors.spigot.events.dooraction;

import lombok.Getter;
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
    /** {@inheritDoc} */
    @Getter(onMethod = @__({@Override}))
    @NotNull
    protected final AbstractDoorBase door;

    /** {@inheritDoc} */
    @Getter(onMethod = @__({@Override}))
    @NotNull
    protected final DoorActionCause cause;

    /** {@inheritDoc} */
    @Getter(onMethod = @__({@Override}))
    @NotNull
    protected final DoorActionType actionType;

    /** {@inheritDoc} */
    @Getter(onMethod = @__({@Override}))
    @NotNull
    protected final Optional<IPPlayer> responsible;

    /** {@inheritDoc} */
    @Getter(onMethod = @__({@Override}))
    protected final double time;

    /** {@inheritDoc} */
    @Getter(onMethod = @__({@Override}))
    protected final boolean animationSkipped;

    DoorToggleEvent(final @NotNull AbstractDoorBase door, final @NotNull DoorActionCause cause,
                    final @NotNull DoorActionType actionType, final @Nullable IPPlayer responsible, final double time,
                    final boolean animationSkipped)
    {
        super(false);
        this.door = door;
        this.cause = cause;
        this.actionType = actionType;
        this.responsible = Optional.ofNullable(responsible);
        this.time = time;
        this.animationSkipped = animationSkipped;
    }
}
