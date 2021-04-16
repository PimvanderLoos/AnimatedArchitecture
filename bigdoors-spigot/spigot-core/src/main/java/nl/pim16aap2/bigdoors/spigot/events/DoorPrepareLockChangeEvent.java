package nl.pim16aap2.bigdoors.spigot.events;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.IDoorPrepareLockChangeEvent;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the event where a door will be (un)locked.
 *
 * @author Pim
 */
public class DoorPrepareLockChangeEvent extends DoorEvent implements IDoorPrepareLockChangeEvent
{
    private static final @NonNull HandlerList HANDLERS_LIST = new HandlerList();

    @Getter
    @Setter
    private boolean isCancelled = false;

    private final boolean newLockStatus;

    public DoorPrepareLockChangeEvent(final @NonNull AbstractDoorBase door,
                                      final @Nullable IPPlayer responsible,
                                      final boolean newLockStatus)
    {
        super(door, responsible);
        this.newLockStatus = newLockStatus;
    }

    @Override
    public @NonNull HandlerList getHandlers()
    {
        return HANDLERS_LIST;
    }

    public static @NonNull HandlerList getHandlerList()
    {
        return HANDLERS_LIST;
    }

    @Override
    public boolean newLockStatus()
    {
        return newLockStatus;
    }
}
