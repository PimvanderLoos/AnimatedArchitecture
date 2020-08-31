package nl.pim16aap2.bigdoors.spigot.util.api;

import nl.pim16aap2.bigdoors.util.IGlowingBlock;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface IGlowingBlockFactory
{
    /**
     * Spawns a glowing block.
     *
     * @param player The player who will see the glowing block.
     * @param world  The world in which the glowing block will be spawned
     * @return The {@link IGlowingBlock} that was spawned.
     */
    @NotNull IGlowingBlock createGlowingBlock(final @NotNull Player player, final @NotNull World world);
}
