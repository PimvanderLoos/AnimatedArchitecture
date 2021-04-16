package nl.pim16aap2.bigdoors.spigot.events;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.IDoorPrepareCreateEvent;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the event where a door will be created.
 *
 * @author Pim
 */
@ToString
public class DoorPrepareCreateEvent extends DoorEvent implements IDoorPrepareCreateEvent
{
    private static final @NonNull HandlerList HANDLERS_LIST = new HandlerList();

    @Getter
    @Setter
    private boolean isCancelled = false;

    public DoorPrepareCreateEvent(final @NonNull AbstractDoorBase door,
                                  final @Nullable IPPlayer responsible)
    {
        super(door, responsible);
    }

    @Override
    public @NonNull AbstractDoorBase getDoor()
    {
        return super.getDoor();
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
