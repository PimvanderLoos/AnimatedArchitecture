package nl.pim16aap2.bigdoors.spigot.events.dooraction;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEventTogglePrepare;
import nl.pim16aap2.bigdoors.util.CuboidConst;
import org.bukkit.event.HandlerList;

/**
 * Implementation of {@link IDoorEventTogglePrepare} for the Spigot platform.
 *
 * @author Pim
 */
public class DoorEventTogglePrepare extends DoorEventToggleStart implements IDoorEventTogglePrepare
{
    @NonNull
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    private boolean isCancelled = false;

    /**
     * Constructs a door action event.
     *
     * @param door             The door.
     * @param cause            What caused the action.
     * @param actionType       The type of action.
     * @param responsible      Who is responsible for this door. This player may be online, but does not have to be.
     * @param time             The number of seconds the door will take to open. Note that there are other factors that
     *                         affect the total time as well.
     * @param animationSkipped If true, the door will skip the animation and open instantly.
     * @param newCuboid        The {@link CuboidConst} representing the area the door will take up after the toggle.
     */
    public DoorEventTogglePrepare(final @NonNull AbstractDoorBase door, final @NonNull DoorActionCause cause,
                                  final @NonNull DoorActionType actionType, final @NonNull IPPlayer responsible,
                                  final double time, final boolean animationSkipped,
                                  final @NonNull CuboidConst newCuboid)
    {
        super(door, cause, actionType, responsible, time, animationSkipped, newCuboid);
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
