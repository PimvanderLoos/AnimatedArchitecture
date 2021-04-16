package nl.pim16aap2.bigdoors.spigot.events;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.IDoorPrepareRemoveOwnerEvent;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the event where an owner is removed from a door.
 *
 * @author Pim
 */
public class DoorPrepareRemoveOwnerEvent extends DoorEvent implements IDoorPrepareRemoveOwnerEvent
{
    private static final @NonNull HandlerList HANDLERS_LIST = new HandlerList();

    @Getter
    @Setter
    private boolean isCancelled = false;

    @Getter
    private final @NonNull DoorOwner removedDoorOwner;

    public DoorPrepareRemoveOwnerEvent(final @NonNull AbstractDoorBase door,
                                       final @Nullable IPPlayer responsible,
                                       final @NonNull DoorOwner removedDoorOwner)
    {
        super(door, responsible);
        this.removedDoorOwner = removedDoorOwner;
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
}
