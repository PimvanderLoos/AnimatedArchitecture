package nl.pim16aap2.bigdoors.spigot.events;

import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.events.IDoorEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

abstract class DoorEvent extends BigDoorsSpigotEvent implements IDoorEvent
{
    @Getter
    protected final AbstractDoor door;

    @Getter
    protected final Optional<IPPlayer> responsible;

    protected DoorEvent(final AbstractDoor door, final @Nullable IPPlayer responsible)
    {
        this.door = door;
        this.responsible = Optional.ofNullable(responsible);
    }
}
