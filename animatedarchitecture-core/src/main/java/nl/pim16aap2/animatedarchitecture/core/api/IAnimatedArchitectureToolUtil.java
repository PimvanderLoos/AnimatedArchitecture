package nl.pim16aap2.animatedarchitecture.core.api;

import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;

/**
 * Represents a set of utility functions around the tool used by {@link ToolUser}s.
 *
 * @author Pim
 */
public interface IAnimatedArchitectureToolUtil
{
    /**
     * Gives a player a AnimatedArchitecture.
     *
     * @param player
     *     The player to give the tool to.
     * @param name
     *     The itemname of the tool.
     * @param lore
     *     The lore of the tool.
     */
    void giveToPlayer(IPlayer player, String name, String lore);

    /**
     * Removes any AnimatedArchitecture tools from a player's inventory.
     *
     * @param player
     *     The player whose inventory will be checked.
     */
    void removeTool(IPlayer player);

    /**
     * Checks if a player is currently holding a AnimatedArchitecture tool in their main hand.
     *
     * @param player
     *     The player to check.
     * @return True if they are currently holding a AnimatedArchitecture tool in their main hand.
     */
    boolean isPlayerHoldingTool(IPlayer player);
}
