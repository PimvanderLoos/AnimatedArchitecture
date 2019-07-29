package nl.pim16aap2.bigdoors.events.dooraction;

/**
 * Represents the different kind of causes that can be the reason of a {@link DoorBase} action.
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
     * The {@link DoorBase} was toggled from the console or by a command block.
     */
    SERVER,

    ;
}
