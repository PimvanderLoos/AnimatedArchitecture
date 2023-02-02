package nl.pim16aap2.bigdoors.spigot.events;

import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.events.IStructureCreatedEvent;
import nl.pim16aap2.bigdoors.structures.AbstractStructure;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the event where a structure was created.
 *
 * @author Pim
 */
@ToString
public class StructureCreatedEvent extends StructureEvent implements IStructureCreatedEvent
{
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    public StructureCreatedEvent(AbstractStructure structure, @Nullable IPPlayer responsible)
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