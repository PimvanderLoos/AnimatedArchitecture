package nl.pim16aap2.bigdoors.spigot.events.dooraction;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEvent;
import nl.pim16aap2.bigdoors.spigot.events.BigDoorsSpigotEvent;

@AllArgsConstructor
abstract class DoorToggleEvent extends BigDoorsSpigotEvent implements IDoorEvent
{
    @Getter
    protected final @NonNull AbstractDoorBase door;

    @Getter
    protected final @NonNull DoorActionCause cause;

    @Getter
    protected final @NonNull DoorActionType actionType;

    @Getter
    protected final @NonNull IPPlayer responsible;

    @Getter
    protected final double time;

    @Getter
    protected final boolean animationSkipped;
}
