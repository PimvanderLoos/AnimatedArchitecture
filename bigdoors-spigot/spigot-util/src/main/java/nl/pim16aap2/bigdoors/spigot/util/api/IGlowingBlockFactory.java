package nl.pim16aap2.bigdoors.spigot.util.api;

import nl.pim16aap2.bigdoors.api.IGlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.restartable.IRestartableHolder;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.util.IGlowingBlock;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Optional;

public interface IGlowingBlockFactory
{
    /**
     * Spawns a glowing block.
     *
     * @param player
     *     The player who will see the glowing block.
     * @param world
     *     The world in which the glowing block will be spawned.
     * @param restartableHolder
     *     The {@link IRestartableHolder} where the resulting {@link IGlowingBlock} will be (de)registered when it is
     *     (de)spawned.
     * @return The {@link IGlowingBlock} that was spawned.
     */
    Optional<IGlowingBlock> createGlowingBlock(Player player, World world, IRestartableHolder restartableHolder,
                                               IPLogger logger, IGlowingBlockSpawner glowingBlockSpawner,
                                               IPExecutor executor);
}
