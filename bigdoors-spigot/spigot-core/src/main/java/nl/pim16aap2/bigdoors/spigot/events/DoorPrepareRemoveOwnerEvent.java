package nl.pim16aap2.bigdoors.spigot.events;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.events.IDoorPrepareRemoveOwnerEvent;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the event where an owner is removed from a door.
 *
 * @author Pim
 */
@ToString
public class DoorPrepareRemoveOwnerEvent extends DoorEvent implements IDoorPrepareRemoveOwnerEvent
{
    private static final @NotNull HandlerList HANDLERS_LIST;

    static
    {
        HANDLERS_LIST = new HandlerList();
    }

    @Getter
    @Setter
    private boolean isCancelled = false;

    @Getter
    private final @NotNull DoorOwner removedDoorOwner;

    public DoorPrepareRemoveOwnerEvent(final @NotNull AbstractDoor door,
                                       final @Nullable IPPlayer responsible,
                                       final @NotNull DoorOwner removedDoorOwner)
    {
        super(door, responsible);
        this.removedDoorOwner = removedDoorOwner;
    }

    @Override
    public @NotNull HandlerList getHandlers()
    {
        return HANDLERS_LIST;
    }

    public static @NotNull HandlerList getHandlerList()
    {
        return HANDLERS_LIST;
    }
}
