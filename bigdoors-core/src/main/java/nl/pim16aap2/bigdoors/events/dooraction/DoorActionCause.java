package nl.pim16aap2.bigdoors.events.dooraction;

import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.managers.AutoCloseScheduler;

/**
 * Represents the different kind of causes that can be the reason of a {@link AbstractDoorBase} action.
 *
 * @author Pim
 */
public enum DoorActionCause
{
    /**
     * The action was initiated by a player.
     */
    PLAYER,

    /**
     * The action was initiated by a redstone signal.
     */
    REDSTONE,

    /**
     * The {@link AbstractDoorBase} was toggled from the console or by a command block.
     */
    SERVER,

    /**
     * The action was intiated by the auto close system. See {@link AutoCloseScheduler}.
     */
    AUTOCLOSE,

    /**
     * The action was initiated because this type is always moving (e.g. clocks, flags, windmills).
     */
    PERPETUALMOVEMENT,

    ;
}
