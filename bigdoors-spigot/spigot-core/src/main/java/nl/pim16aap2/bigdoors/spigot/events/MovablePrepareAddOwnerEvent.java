package nl.pim16aap2.bigdoors.spigot.events;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.events.IMovablePrepareAddOwnerEvent;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableOwner;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the event where a new owner is added to a movable.
 *
 * @author Pim
 */
@ToString
public class MovablePrepareAddOwnerEvent extends MovableEvent implements IMovablePrepareAddOwnerEvent
{
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    @Getter
    @Setter
    private boolean isCancelled = false;

    @Getter
    private final MovableOwner newMovableOwner;

    public MovablePrepareAddOwnerEvent(
        AbstractMovable movable, @Nullable IPPlayer responsible, MovableOwner newMovableOwner)
    {
        super(movable, responsible);
        this.newMovableOwner = newMovableOwner;
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