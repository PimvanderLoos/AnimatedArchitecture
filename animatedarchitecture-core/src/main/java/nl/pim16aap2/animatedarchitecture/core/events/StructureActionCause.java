package nl.pim16aap2.animatedarchitecture.core.events;

import nl.pim16aap2.animatedarchitecture.core.structures.Structure;

/**
 * Represents the different kind of causes that can be the reason of a {@link Structure} action.
 */
public enum StructureActionCause
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
     * The {@link Structure} was toggled from the console or by a command block.
     */
    SERVER,

    /**
     * The {@link Structure} was toggled by another plugin.
     */
    PLUGIN,

    /**
     * The action was initiated because this type is always moving (e.g. clocks, flags, windmills).
     */
    PERPETUAL_MOVEMENT,

}
