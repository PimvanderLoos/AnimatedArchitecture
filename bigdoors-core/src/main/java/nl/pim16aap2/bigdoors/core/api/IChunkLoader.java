package nl.pim16aap2.bigdoors.core.api;

import nl.pim16aap2.bigdoors.core.util.Cuboid;
import nl.pim16aap2.bigdoors.core.util.vector.IVector3D;

/**
 * Tool that can be used to load chunks or check if they are already loaded.
 *
 * @author Pim
 */
public interface IChunkLoader
{
    /**
     * Checks if all chunks intersecting with a cuboid are loaded. For any unloaded chunks, an attempt to load them may
     * be performed, as determined by the load mode.
     *
     * @param world
     *     The world whose chunks to check.
     * @param cuboid
     *     The region to check. All chunks that are at least partially inside this cuboid will be checked.
     * @param chunkLoadMode
     *     The type of chunk loading to use if the chunk is not loaded.
     * @return The result of the action.
     */
    ChunkLoadResult checkChunks(IWorld world, Cuboid cuboid, ChunkLoadMode chunkLoadMode);

    /**
     * Checks if the chunk a position exists in is loaded.
     *
     * @param world
     *     The world whose chunks to check.
     * @param position
     *     The coordinates to check.
     * @param chunkLoadMode
     *     The type of chunk loading to use if the chunk is not loaded.
     * @return The result of the action.
     */
    ChunkLoadResult checkChunk(IWorld world, IVector3D position, ChunkLoadMode chunkLoadMode);

    /**
     * Represents the different modes of checking chunks.
     */
    enum ChunkLoadMode
    {
        /**
         * Verifies that chunks are loaded. If not, it will not attempt to load it and abort the process instead.
         */
        VERIFY_LOADED,

        /**
         * Attempts to load any potentially unloaded chunks.
         */
        ATTEMPT_LOAD,
    }

    /**
     * The result of attempting to load one or more chunks.
     */
    enum ChunkLoadResult
    {
        /**
         * All chunks are loaded.
         */
        PASS,

        /**
         * The process failed. For example, it could not load any chunks even though it had to.
         */
        FAIL,

        /**
         * The process successfully loaded 1 or more chunks.
         */
        REQUIRED_LOAD
    }
}
