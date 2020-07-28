/**
 *
 */
package nl.pim16aap2.bigDoors.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.Location;
import org.bukkit.World;

/**
 *
 * @author Pim
 */
public final class ChunkUtils
{
    private static final Method isChunkGeneratedMethod;
    static
    {
        Method isChunkGeneratedMethodTmp = null;
        try
        {
            isChunkGeneratedMethodTmp = World.class.getMethod("isChunkGenerated​", int.class, int.class);
        }
        catch (NoSuchMethodException e)
        {
            // ignore
        }
        isChunkGeneratedMethod = isChunkGeneratedMethodTmp;
    }

    private ChunkUtils()
    {
        // STAY OUT!
        throw new IllegalAccessError();
    }

    public static Pair<Vector2D, Vector2D> getChunkRangeBetweenCoords(final Location locA, final Location locB)
    {
        return getChunkRangeBetweenSortedCoords(locA, locB, locA, locB);
    }

    public static Pair<Vector2D, Vector2D> getChunkRangeBetweenSortedCoords(final Location minA, final Location minB,
                                                                            final Location maxA, final Location maxB)
    {
        int minX = Math.min(minA.getBlockX(), minB.getBlockX());
        int minZ = Math.min(minA.getBlockZ(), minB.getBlockZ());

        int maxX = Math.max(maxA.getBlockX(), maxB.getBlockX());
        int maxZ = Math.max(maxA.getBlockZ(), maxB.getBlockZ());

        // Convert coords to chunk-space
        minX = minX >> 4;
        minZ = minZ >> 4;
        maxX = maxX >> 4;
        maxZ = maxZ >> 4;

        return new Pair<>(new Vector2D(minX, minZ), new Vector2D(maxX, maxZ));
    }

    public static Pair<Vector2D, Vector2D> getChunkRangeBetweenCoords(final Location locA, final Location locB,
                                                                      final Location locC, final Location locD)
    {
        int minX = Math.min(Math.min(locA.getBlockX(), locB.getBlockX()), Math.min(locC.getBlockX(), locD.getBlockX()));
        int minZ = Math.min(Math.min(locA.getBlockZ(), locB.getBlockZ()), Math.min(locC.getBlockZ(), locD.getBlockZ()));

        int maxX = Math.max(Math.max(locA.getBlockX(), locB.getBlockX()), Math.max(locC.getBlockX(), locD.getBlockX()));
        int maxZ = Math.max(Math.max(locA.getBlockZ(), locB.getBlockZ()), Math.max(locC.getBlockZ(), locD.getBlockZ()));

        // Convert coords to chunk-space
        minX = minX >> 4;
        minZ = minZ >> 4;
        maxX = maxX >> 4;
        maxZ = maxZ >> 4;

        return new Pair<>(new Vector2D(minX, minZ), new Vector2D(maxX, maxZ));
    }

    public static ChunkLoadResult checkChunks(final World world, final Pair<Vector2D, Vector2D> chunkRange, final ChunkLoadMode mode)
    {
        TriFunction<World, Integer, Integer, ChunkLoadResult> modeFun;
        switch (mode)
        {
        case VERIFY_LOADED:
            modeFun = ChunkUtils::verifyLoaded;
            break;
        case ATTEMPT_LOAD:
            modeFun = ChunkUtils::attemptLoad;
            break;
        default:
            throw new UnsupportedOperationException();
        }

        boolean requiredLoad = false;
        for (int x = chunkRange.first.getX(); x <= chunkRange.second.getX(); ++x)
            for (int z = chunkRange.first.getY(); z <= chunkRange.second.getY(); ++z)
            {
                final ChunkLoadResult result = modeFun.apply(world, x, z);
                if (result == ChunkLoadResult.REQUIRED_LOAD)
                    requiredLoad = true;
                else if (result == ChunkLoadResult.FAIL)
                    return ChunkLoadResult.FAIL;
            }
        return requiredLoad ? ChunkLoadResult.REQUIRED_LOAD : ChunkLoadResult.PASS;
    }

    /**
     * This will check if the chunk has been generated on versions of Spigot that
     * have "World#isChunkGenerated(int, int)".
     *
     * @param chunkX The x-coordinate of the chunk (in chunk-space).
     * @param chunkZ The z-coordinate of the chunk (in chunk-space).
     * @return False if the chunk was definitely not generated, otherwise true
     *         (including when errors occur).
     */
    private static boolean isChunkGenerated(final World world, final Integer chunkX, final Integer chunkZ)
    {
        if (isChunkGeneratedMethod == null)
            return true;

        try
        {
            return (boolean) isChunkGeneratedMethod.invoke(world, chunkX, chunkZ);
        }
        catch (InvocationTargetException | IllegalAccessException e)
        {
            e.printStackTrace();
            return true;
        }
    }

    private static ChunkLoadResult attemptLoad(World world, final Integer chunkX, final Integer chunkZ)
    {
        if (verifyLoaded(world, chunkX, chunkZ) == ChunkLoadResult.PASS)
            return ChunkLoadResult.PASS;

        if (!isChunkGenerated(world, chunkX, chunkZ))
            return ChunkLoadResult.FAIL;

        world.getChunkAt(chunkX, chunkZ);
        return ChunkLoadResult.REQUIRED_LOAD;
    }

    private static ChunkLoadResult verifyLoaded(World world, final Integer chunkX, final Integer chunkZ)
    {
        return world.isChunkLoaded(chunkX, chunkZ) ? ChunkLoadResult.PASS : ChunkLoadResult.FAIL;
    }

    public enum ChunkLoadMode
    {
        /**
         * Verifies that chunks are loaded. If not, it will not attempt to load it and
         * abort the process instead.
         */
        VERIFY_LOADED,

        /**
         * Attempts to load any potentially unloaded chunks.
         */
        ATTEMPT_LOAD,
    }

    public enum ChunkLoadResult
    {
        /**
         * All chunks are loaded.
         */
        PASS,

        /**
         * The process failed. For example, it could not load any chunks even though it
         * had to.
         */
        FAIL,

        /**
         * The process successfully loaded 1 or more chunks.
         */
        REQUIRED_LOAD
    }
}
