package nl.pim16aap2.bigdoors.spigot.events;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.events.IStructurePrepareAddOwnerEvent;
import nl.pim16aap2.bigdoors.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.structures.StructureOwner;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the event where a new owner is added to a structure.
 *
 * @author Pim
 */
@ToString
public class StructurePrepareAddOwnerEvent extends StructureEvent implements IStructurePrepareAddOwnerEvent
{
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    @Getter
    @Setter
    private boolean isCancelled = false;

    @Getter
    private final StructureOwner newStructureOwner;

    public StructurePrepareAddOwnerEvent(
        AbstractStructure structure, @Nullable IPPlayer responsible, StructureOwner newStructureOwner)
    {
        super(structure, responsible);
        this.newStructureOwner = newStructureOwner;
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
