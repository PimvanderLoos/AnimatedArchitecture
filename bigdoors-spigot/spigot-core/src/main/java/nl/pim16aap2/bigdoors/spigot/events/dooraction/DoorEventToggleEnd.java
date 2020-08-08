package nl.pim16aap2.bigdoors.spigot.events.dooraction;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEventToggleEnd;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of {@link IDoorEventToggleEnd} for the Spigot platform.
 *
 * @author Pim
 */
public class DoorEventToggleEnd extends DoorToggleEvent implements IDoorEventToggleEnd
{
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    /**
     * Constructs a door action event.
     *
     * @param door          The door.
     * @param cause         What caused the action.
     * @param actionType    The type of action.
     * @param responsible   Who is responsible for this door. This player may be online, but does not have to be.
     * @param time          The number of seconds the door will take to open. Note that there are other factors that
     *                      affect the total time as well.
     * @param skipAnimation If true, the door will skip the animation and open instantly.
     */
    public DoorEventToggleEnd(final @NotNull AbstractDoorBase door, final @NotNull DoorActionCause cause,
                              final @NotNull DoorActionType actionType, final @NotNull IPPlayer responsible,
                              final double time, final boolean skipAnimation)
    {
        super(door, cause, actionType, responsible, time, skipAnimation);
    }

    @Override
    @NotNull
    public HandlerList getHandlers()
    {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList()
    {
        return HANDLERS_LIST;
    }
}
