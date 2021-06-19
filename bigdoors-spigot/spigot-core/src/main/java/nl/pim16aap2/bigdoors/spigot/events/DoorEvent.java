package nl.pim16aap2.bigdoors.spigot.events;

import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.IDoorEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

abstract class DoorEvent extends BigDoorsSpigotEvent implements IDoorEvent
{
    @Getter
    protected final @NotNull AbstractDoorBase door;

    @Getter
    protected final @NotNull Optional<IPPlayer> responsible;

    public DoorEvent(final @NotNull AbstractDoorBase door, final @Nullable IPPlayer responsible)
    {
        this.door = door;
        this.responsible = Optional.ofNullable(responsible);
    }
}
