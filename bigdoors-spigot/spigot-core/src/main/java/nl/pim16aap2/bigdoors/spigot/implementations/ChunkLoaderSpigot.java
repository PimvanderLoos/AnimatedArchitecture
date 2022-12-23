package nl.pim16aap2.bigdoors.spigot.implementations;

import nl.pim16aap2.bigdoors.api.IChunkLoader;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.functional.TriFunction;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.bukkit.World;

import javax.inject.Inject;

/**
 * Implementation of {@link IChunkLoader} for the Spigot platform.
 */
public class ChunkLoaderSpigot implements IChunkLoader
{
    @Inject
    public ChunkLoaderSpigot()
    {
    }

    public Cuboid getChunkCuboid(Cuboid cuboid)
    {
        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();
        return new Cuboid(new Vector3Di(min.x() >> 4, min.y(), min.z() >> 4),
                          new Vector3Di(max.x() >> 4, max.y(), max.z() >> 4));
    }

    @Override
    public ChunkLoadResult checkChunks(IPWorld iWorld, Cuboid cuboid, ChunkLoadMode chunkLoadMode)
    {
        final TriFunction<World, Integer, Integer, ChunkLoadResult> modeFun = switch (chunkLoadMode)
            {
                case VERIFY_LOADED -> this::verifyLoaded;
                case ATTEMPT_LOAD -> this::attemptLoad;
            };

        final World world = Util.requireNonNull(SpigotAdapter.getBukkitWorld(iWorld), "Bukkit World");
        final Cuboid chunkCuboid = getChunkCuboid(cuboid);

        boolean requiredLoad = false;
        for (int x = chunkCuboid.getMin().x(); x <= chunkCuboid.getMax().x(); ++x)
            for (int z = chunkCuboid.getMin().z(); z <= chunkCuboid.getMax().z(); ++z)
            {
                final ChunkLoadResult result = modeFun.apply(world, x, z);
                if (result == ChunkLoadResult.REQUIRED_LOAD)
                    requiredLoad = true;
                else if (result == ChunkLoadResult.FAIL)
                    return ChunkLoadResult.FAIL;
            }
        return requiredLoad ? ChunkLoadResult.REQUIRED_LOAD : ChunkLoadResult.PASS;
    }

    private ChunkLoadResult attemptLoad(World world, final Integer chunkX, final Integer chunkZ)
    {
        if (verifyLoaded(world, chunkX, chunkZ) == ChunkLoadResult.PASS)
            return ChunkLoadResult.PASS;

        if (!world.isChunkGenerated(chunkX, chunkZ))
            return ChunkLoadResult.FAIL;

        world.getChunkAt(chunkX, chunkZ);
        return ChunkLoadResult.REQUIRED_LOAD;
    }

    private ChunkLoadResult verifyLoaded(World world, final Integer chunkX, final Integer chunkZ)
    {
        return world.isChunkLoaded(chunkX, chunkZ) ? ChunkLoadResult.PASS : ChunkLoadResult.FAIL;
    }
}
