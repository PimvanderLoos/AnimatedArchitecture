package nl.pim16aap2.animatedarchitecture.spigot.core.managers;

import nl.pim16aap2.animatedarchitecture.core.api.IConfig;
import nl.pim16aap2.animatedarchitecture.core.api.IRedstoneManager;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
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

    private final IConfig config;

    @Inject
    public RedstoneManagerSpigot(IConfig config)
    {
        this.config = config;
    }

    @Override
    public RedstoneStatus isBlockPowered(IWorld world, Vector3Di position)
    {
        if (!config.isRedstoneEnabled())
            return RedstoneStatus.DISABLED;

        final World bukkitWorld = Util.requireNonNull(SpigotAdapter.getBukkitWorld(world), "bukkitWorld");
        final Block block = bukkitWorld.getBlockAt(position.x(), position.y(), position.z());

        for (final BlockFace blockFace : FACE_LIST)
            if (block.getRelative(blockFace).isBlockPowered())
                return RedstoneStatus.POWERED;
        return RedstoneStatus.UNPOWERED;
    }
}
