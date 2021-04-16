package nl.pim16aap2.bigdoors.spigot.events;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.IDoorPrepareAddOwnerEvent;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the event where a new owner is added to a door.
 *
 * @author Pim
 */
@ToString
public class DoorPrepareAddOwnerEvent extends DoorEvent implements IDoorPrepareAddOwnerEvent
{
    private static final @NonNull HandlerList HANDLERS_LIST = new HandlerList();

    @Getter
    @Setter
    private boolean isCancelled = false;

    @Getter
    private final @NonNull DoorOwner newDoorOwner;

    public DoorPrepareAddOwnerEvent(final @NonNull AbstractDoorBase door,
                                    final @Nullable IPPlayer responsible,
                                    final @NonNull DoorOwner newDoorOwner)
    {
        super(door, responsible);
        this.newDoorOwner = newDoorOwner;
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
