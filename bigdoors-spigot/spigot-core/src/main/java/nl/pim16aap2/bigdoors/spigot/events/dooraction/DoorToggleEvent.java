package nl.pim16aap2.bigdoors.spigot.events.dooraction;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorToggleEvent;
import nl.pim16aap2.bigdoors.spigot.events.BigDoorsSpigotEvent;

@AllArgsConstructor
abstract class DoorToggleEvent extends BigDoorsSpigotEvent implements IDoorToggleEvent
{
    @Getter
    protected final AbstractDoor door;

    @Getter
    protected final DoorActionCause cause;

    @Getter
    protected final DoorActionType actionType;

    @Getter
    protected final IPPlayer responsible;

    @Getter
    protected final double time;

    @Getter
    protected final boolean animationSkipped;
}
