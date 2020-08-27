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

@AllArgsConstructor
abstract class DoorToggleEvent extends BigDoorsSpigotEvent implements IDoorEvent
{
    @Getter(onMethod = @__({@Override}))
    @NotNull
    protected final AbstractDoorBase door;

    @Getter(onMethod = @__({@Override}))
    @NotNull
    protected final DoorActionCause cause;

    @Getter(onMethod = @__({@Override}))
    @NotNull
    protected final DoorActionType actionType;

    @Getter(onMethod = @__({@Override}))
    @NotNull
    protected final IPPlayer responsible;

    @Getter(onMethod = @__({@Override}))
    protected final double time;

    @Getter(onMethod = @__({@Override}))
    protected final boolean animationSkipped;
}
