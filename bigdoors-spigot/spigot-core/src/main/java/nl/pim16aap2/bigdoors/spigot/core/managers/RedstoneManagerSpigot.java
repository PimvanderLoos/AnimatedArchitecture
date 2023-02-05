package nl.pim16aap2.bigdoors.spigot.core.managers;

import nl.pim16aap2.bigdoors.core.api.IConfigLoader;
import nl.pim16aap2.bigdoors.core.api.IPWorld;
import nl.pim16aap2.bigdoors.core.api.IRedstoneManager;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.core.util.Util;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Di;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

/**
 * Represents an implementation of {@link IRedstoneManager} for the Spigot platform.
 *
 * @author Pim
 */
@Singleton
public final class RedstoneManagerSpigot implements IRedstoneManager
{
    private static final List<BlockFace> FACE_LIST =
        List.of(BlockFace.SELF,
                BlockFace.UP, BlockFace.DOWN,
                BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST);

    private final IConfigLoader configLoader;

    @Inject
    public RedstoneManagerSpigot(IConfigLoader configLoader)
    {
        this.configLoader = configLoader;
    }

    @Override
    public RedstoneStatus isBlockPowered(IPWorld world, Vector3Di position)
    {
        if (!configLoader.isRedstoneEnabled())
            return RedstoneStatus.DISABLED;

        final World bukkitWorld = Util.requireNonNull(SpigotAdapter.getBukkitWorld(world), "bukkitWorld");
        final Block block = bukkitWorld.getBlockAt(position.x(), position.y(), position.z());

        for (final BlockFace blockFace : FACE_LIST)
            if (block.getRelative(blockFace).isBlockPowered())
                return RedstoneStatus.POWERED;
        return RedstoneStatus.UNPOWERED;
    }
}
