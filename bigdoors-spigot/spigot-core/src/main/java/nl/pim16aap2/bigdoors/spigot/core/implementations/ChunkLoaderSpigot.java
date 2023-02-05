package nl.pim16aap2.bigdoors.spigot.core.implementations;

import nl.pim16aap2.bigdoors.core.api.IChunkLoader;
import nl.pim16aap2.bigdoors.core.api.IPWorld;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.core.util.Cuboid;
import nl.pim16aap2.bigdoors.core.util.Util;
import nl.pim16aap2.bigdoors.core.util.vector.IVector3D;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Di;
import org.bukkit.World;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation of {@link IChunkLoader} for the Spigot platform.
 */
@Singleton
public class ChunkLoaderSpigot implements IChunkLoader
{
    private final IChunkLoadFunction[] chunkLoadFunctions;

    @Inject
    public ChunkLoaderSpigot()
    {
        chunkLoadFunctions = new IChunkLoadFunction[]{this::verifyLoaded, this::attemptLoad};
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
        final IChunkLoadFunction modeFun = chunkLoadFunctions[chunkLoadMode.ordinal()];

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

    @Override
    public ChunkLoadResult checkChunk(IPWorld iWorld, IVector3D position, ChunkLoadMode chunkLoadMode)
    {
        final IChunkLoadFunction modeFun = chunkLoadFunctions[chunkLoadMode.ordinal()];
        final World world = Util.requireNonNull(SpigotAdapter.getBukkitWorld(iWorld), "Bukkit World");

        final int chunkX = ((int) Math.round(position.xD())) >> 4;
        final int chunkZ = ((int) Math.round(position.zD())) >> 4;

        return modeFun.apply(world, chunkX, chunkZ);
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

    @FunctionalInterface
    private interface IChunkLoadFunction
    {
        ChunkLoadResult apply(World world, int chunkX, int chunkZ);
    }
}
