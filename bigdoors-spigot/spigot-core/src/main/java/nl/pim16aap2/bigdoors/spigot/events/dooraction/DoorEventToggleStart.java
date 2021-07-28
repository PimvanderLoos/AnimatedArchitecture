package nl.pim16aap2.bigdoors.spigot.events.dooraction;

import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEventToggleStart;
import nl.pim16aap2.bigdoors.util.Cuboid;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of {@link IDoorEventToggleStart} for the Spigot platform.
 *
 * @author Pim
 */
@ToString
public class DoorEventToggleStart extends DoorToggleEvent implements IDoorEventToggleStart
{
    private static final @NotNull HandlerList HANDLERS_LIST = new HandlerList();

    @Getter
    private final @NotNull Cuboid newCuboid;

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
     * @param newCuboid     The {@link Cuboid} representing the area the door will take up after the toggle.
     */
    public DoorEventToggleStart(final @NotNull AbstractDoorBase door, final @NotNull DoorActionCause cause,
                                final @NotNull DoorActionType actionType, final @NotNull IPPlayer responsible,
                                final double time, final boolean skipAnimation, final @NotNull Cuboid newCuboid)
    {
        super(door, cause, actionType, responsible, time, skipAnimation);
        this.newCuboid = newCuboid;
    }

    @Override
    public @NotNull HandlerList getHandlers()
    {
        return HANDLERS_LIST;
    }

    public static @NotNull HandlerList getHandlerList()
    {
        return HANDLERS_LIST;
    }
}
