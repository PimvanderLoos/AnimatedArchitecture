package nl.pim16aap2.bigdoors.spigot.events.dooraction;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEvent;
import nl.pim16aap2.bigdoors.spigot.events.BigDoorsSpigotEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@AllArgsConstructor
abstract class DoorToggleEvent extends BigDoorsSpigotEvent implements IDoorEvent
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
}
