package nl.pim16aap2.bigdoors.events.movableaction;

import nl.pim16aap2.bigdoors.movable.AbstractMovable;

/**
 * Represents the different kind of causes that can be the reason of a {@link AbstractMovable} action.
 *
 * @author Pim
 */
public enum MovableActionCause
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
     * The {@link AbstractMovable} was toggled from the console or by a command block.
     */
    SERVER,

    /**
     * The {@link AbstractMovable} was toggled by another plugin.
     */
    PLUGIN,

    /**
     * The action was initiated because this type is always moving (e.g. clocks, flags, windmills).
     */
    PERPETUAL_MOVEMENT,

}
