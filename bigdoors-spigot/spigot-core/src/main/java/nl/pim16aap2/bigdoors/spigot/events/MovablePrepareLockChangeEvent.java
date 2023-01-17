package nl.pim16aap2.bigdoors.spigot.events;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.events.IMovablePrepareLockChangeEvent;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the event where a movable will be (un)locked.
 *
 * @author Pim
 */
@ToString
public class MovablePrepareLockChangeEvent extends MovableEvent implements IMovablePrepareLockChangeEvent
{
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    @Getter
    @Setter
    private boolean isCancelled = false;

    private final boolean newLockStatus;

    public MovablePrepareLockChangeEvent(AbstractMovable movable, @Nullable IPPlayer responsible, boolean newLockStatus)
    {
        super(movable, responsible);
        this.newLockStatus = newLockStatus;
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

    @Override
    public boolean newLockStatus()
    {
        return newLockStatus;
    }
}
