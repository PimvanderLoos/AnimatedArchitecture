package nl.pim16aap2.bigdoors.spigot.events;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.events.IStructurePrepareCreateEvent;
import nl.pim16aap2.bigdoors.structures.AbstractStructure;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the event where a structure will be created.
 *
 * @author Pim
 */
@ToString
public class StructurePrepareCreateEvent extends StructureEvent implements IStructurePrepareCreateEvent
{
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    @Getter
    @Setter
    private boolean isCancelled = false;

    public StructurePrepareCreateEvent(AbstractStructure structure, @Nullable IPPlayer responsible)
    {
        super(structure, responsible);
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
