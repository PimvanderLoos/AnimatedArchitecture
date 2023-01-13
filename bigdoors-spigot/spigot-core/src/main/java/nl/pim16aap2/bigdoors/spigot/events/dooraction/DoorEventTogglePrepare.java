package nl.pim16aap2.bigdoors.spigot.events.dooraction;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.DoorSnapshot;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEventTogglePrepare;
import nl.pim16aap2.bigdoors.util.Cuboid;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of {@link IDoorEventTogglePrepare} for the Spigot platform.
 *
 * @author Pim
 */
@ToString
public class DoorEventTogglePrepare extends DoorToggleEvent implements IDoorEventTogglePrepare
{
    private static final HandlerList HANDLERS_LIST = new HandlerList();

    @Getter
    @Setter
    private boolean isCancelled = false;

    @Getter
    private final Cuboid newCuboid;

    /**
     * Constructs a door action event.
     *
     * @param doorSnapshot
     *     A snapshot of the door.
     * @param cause
     *     What caused the action.
     * @param actionType
     *     The type of action.
     * @param responsible
     *     Who is responsible for this door. This player may be online, but does not have to be.
     * @param time
     *     The number of seconds the door will take to open. Note that there are other factors that affect the total
     *     time as well.
     * @param animationSkipped
     *     If true, the door will skip the animation and open instantly.
     * @param newCuboid
     *     The {@link Cuboid} representing the area the door will take up after the toggle.
     */
    public DoorEventTogglePrepare(
        DoorSnapshot doorSnapshot, DoorActionCause cause, DoorActionType actionType, IPPlayer responsible, double time,
        boolean animationSkipped, Cuboid newCuboid)
    {
        super(doorSnapshot, cause, actionType, responsible, time, animationSkipped);
        this.newCuboid = newCuboid;
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
