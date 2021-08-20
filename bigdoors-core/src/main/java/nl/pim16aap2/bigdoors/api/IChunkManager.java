package nl.pim16aap2.bigdoors.api;

import nl.pim16aap2.bigdoors.util.vector.Vector2Di;

/**
 * Represents a class that manages chunks.
 *
 * @author Pim
 */
public interface IChunkManager
{
    /**
     * Checks if a chunk is loaded.
     *
     * @param world The world the chunk is in.
     * @param chunk The coordinates of the chunk.
     * @return True if the chunk is loaded.
     */
    boolean isLoaded(IPWorld world, Vector2Di chunk);

    /**
     * Attempts to load a chunk (if it is not already loaded).
     *
     * @param world The world the chunk is in.
     * @param chunk The coordinates of the chunk.
     * @return The result of the load attempt.
     */
    ChunkLoadResult load(IPWorld world, Vector2Di chunk);

    /**
     * Represents the result of an attempt to load a chunk.
     *
     * @author Pim
     */
    enum ChunkLoadResult
    {
        /**
         * The chunk was loaded successfully.
         */
        SUCCESS,

        /**
         * The attempt to load the chunk failed.
         */
        FAIL,

        /**
         * The chunk was already loaded.
         */
        ALREADYLOADED,
    }
}
