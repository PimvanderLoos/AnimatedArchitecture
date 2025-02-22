package nl.pim16aap2.animatedarchitecture.spigot.core.events;

import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.events.IStructureEventToggleEnd;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionCause;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionType;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of {@link IStructureEventToggleEnd} for the Spigot platform.
 */
@ToString
public class StructureEventToggleEnd extends StructureToggleEvent implements IStructureEventToggleEnd
{
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    @Getter
    private final Structure structure;

    /**
     * Constructs a structure action event.
     *
     * @param structure
     *     The structure.
     * @param cause
     *     What caused the action.
     * @param actionType
     *     The type of action.
     * @param responsible
     *     Who is responsible for this structure. This player may be online, but does not have to be.
     * @param time
     *     The number of seconds the structure will take to open. Note that there are other factors that affect the
     *     total time as well.
     * @param skipAnimation
     *     If true, the structure will skip the animation and open instantly.
     */
    public StructureEventToggleEnd(
        Structure structure,
        StructureSnapshot snapshot,
        StructureActionCause cause,
        StructureActionType actionType,
        IPlayer responsible,
        double time,
        boolean skipAnimation)
    {
        super(snapshot, cause, actionType, responsible, time, skipAnimation);
        this.structure = structure;
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
