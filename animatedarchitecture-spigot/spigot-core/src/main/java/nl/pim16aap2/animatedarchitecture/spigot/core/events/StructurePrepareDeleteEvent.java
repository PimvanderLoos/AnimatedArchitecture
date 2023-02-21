package nl.pim16aap2.animatedarchitecture.spigot.core.events;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.events.IStructurePrepareDeleteEvent;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the event where a structure was deleted.
 *
 * @author Pim
 */
@ToString
public class StructurePrepareDeleteEvent extends StructureEvent implements IStructurePrepareDeleteEvent
{
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    @Getter
    @Setter
    private boolean isCancelled = false;

    public StructurePrepareDeleteEvent(AbstractStructure structure, @Nullable IPlayer responsible)
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
