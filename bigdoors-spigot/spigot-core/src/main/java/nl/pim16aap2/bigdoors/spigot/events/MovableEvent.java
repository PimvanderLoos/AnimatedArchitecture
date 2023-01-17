package nl.pim16aap2.bigdoors.spigot.events;

import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.events.IMovableEvent;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

abstract class MovableEvent extends BigDoorsSpigotEvent implements IMovableEvent
{
    @Getter
    protected final AbstractMovable movable;

    @Getter
    protected final Optional<IPPlayer> responsible;

    protected MovableEvent(AbstractMovable movable, @Nullable IPPlayer responsible)
    {
        this.movable = movable;
        this.responsible = Optional.ofNullable(responsible);
    }
}
