package nl.pim16aap2.bigdoors.spigot.events.dooraction;

import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.events.movableaction.IMovableEventToggleEnd;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionCause;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionType;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableSnapshot;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of {@link IMovableEventToggleEnd} for the Spigot platform.
 *
 * @author Pim
 */
@ToString
public class MovableEventToggleEnd extends MovableToggleEvent implements IMovableEventToggleEnd
{
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    @Getter
    private final AbstractMovable movable;

    /**
     * Constructs a movable action event.
     *
     * @param movable
     *     The movable.
     * @param cause
     *     What caused the action.
     * @param actionType
     *     The type of action.
     * @param responsible
     *     Who is responsible for this movable. This player may be online, but does not have to be.
     * @param time
     *     The number of seconds the movable will take to open. Note that there are other factors that affect the total
     *     time as well.
     * @param skipAnimation
     *     If true, the movable will skip the animation and open instantly.
     */
    public MovableEventToggleEnd(
        AbstractMovable movable, MovableSnapshot snapshot, MovableActionCause cause, MovableActionType actionType,
        IPPlayer responsible, double time, boolean skipAnimation)
    {
        super(snapshot, cause, actionType, responsible, time, skipAnimation);
        this.movable = movable;
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
