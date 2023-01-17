package nl.pim16aap2.bigdoors.spigot.events;

import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.events.IMovableCreatedEvent;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the event where a movable was created.
 *
 * @author Pim
 */
@ToString
public class MovableCreatedEvent extends MovableEvent implements IMovableCreatedEvent
{
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    public MovableCreatedEvent(AbstractMovable movable, @Nullable IPPlayer responsible)
    {
        super(movable, responsible);
    }

    @Override
    public @NotNull HandlerList getHandlers()
    {
        return HANDLERS_LIST;
    }

    // This method is identical to the getHandlers method (S4144). However, this is required for Spigot.
    @SuppressWarnings("squid:S4144")
    public static HandlerList getHandlerList()
    {
        return HANDLERS_LIST;
    }
}
