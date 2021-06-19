package nl.pim16aap2.bigdoors.spigot.events.dooraction;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorToggleEvent;
import nl.pim16aap2.bigdoors.spigot.events.BigDoorsSpigotEvent;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
abstract class DoorToggleEvent extends BigDoorsSpigotEvent implements IDoorToggleEvent
{
    @Getter
    protected final @NotNull AbstractDoorBase door;

    @Getter
    protected final @NotNull DoorActionCause cause;

    @Getter
    protected final @NotNull DoorActionType actionType;

    @Getter
    protected final @NotNull IPPlayer responsible;

    @Getter
    protected final double time;

    @Getter
    protected final boolean animationSkipped;
}
