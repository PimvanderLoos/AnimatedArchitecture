package nl.pim16aap2.bigdoors.spigot.events;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.events.IStructurePrepareLockChangeEvent;
import nl.pim16aap2.bigdoors.structures.AbstractStructure;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the event where a structure will be (un)locked.
 *
 * @author Pim
 */
@ToString
public class StructurePrepareLockChangeEvent extends StructureEvent implements IStructurePrepareLockChangeEvent
{
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    @Getter
    @Setter
    private boolean isCancelled = false;

    private final boolean newLockStatus;

    public StructurePrepareLockChangeEvent(
        AbstractStructure structure, @Nullable IPPlayer responsible, boolean newLockStatus)
    {
        super(structure, responsible);
        this.newLockStatus = newLockStatus;
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

    @Override
    public boolean newLockStatus()
    {
        return newLockStatus;
    }
}
