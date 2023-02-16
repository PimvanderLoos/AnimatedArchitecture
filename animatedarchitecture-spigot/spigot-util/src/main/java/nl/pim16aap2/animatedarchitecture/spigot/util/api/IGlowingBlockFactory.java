package nl.pim16aap2.animatedarchitecture.spigot.util.api;

import nl.pim16aap2.animatedarchitecture.core.api.Color;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.util.IGlowingBlock;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.Optional;

public interface IGlowingBlockFactory
{
    /**
     * Spawns a glowing block.
     *
     * @param player
     *     The player who will see the glowing block.
     * @param world
     *     The world in which the glowing block will be spawned. (de)spawned.
     * @param rotatedPosition
     *     The position and rotation of the glowing block.
     * @param teams
     *     The teams to use for colors.
     * @return The {@link IGlowingBlock} that was spawned.
     */
    Optional<IGlowingBlock> createGlowingBlock(
        Player player, World world, Color pColor, RotatedPosition rotatedPosition, Map<Color, Team> teams);
}
