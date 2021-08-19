package nl.pim16aap2.bigdoors.spigot.events;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.events.IDoorPrepareLockChangeEvent;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the event where a door will be (un)locked.
 *
 * @author Pim
 */
@ToString
public class DoorPrepareLockChangeEvent extends DoorEvent implements IDoorPrepareLockChangeEvent
{
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    @Getter
    @Setter
    private boolean isCancelled = false;

    private final boolean newLockStatus;

    public DoorPrepareLockChangeEvent(final AbstractDoor door,
                                      final @Nullable IPPlayer responsible,
                                      final boolean newLockStatus)
    {
        super(door, responsible);
        this.newLockStatus = newLockStatus;
    }

    @Override
    public HandlerList getHandlers()
    {
        return HANDLERS_LIST;
    }

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
