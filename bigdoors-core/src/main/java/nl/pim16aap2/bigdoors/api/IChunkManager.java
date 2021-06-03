package nl.pim16aap2.bigdoors.api;

import nl.pim16aap2.bigdoors.util.vector.Vector2DiConst;
import org.jetbrains.annotations.NotNull;

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
    boolean isLoaded(@NotNull IPWorld world, @NotNull Vector2DiConst chunk);

    /**
     * Attempts to load a chunk (if it is not already loaded).
     *
     * @param world The world the chunk is in.
     * @param chunk The coordinates of the chunk.
     * @return The result of the load attempt.
     */
    @NotNull ChunkLoadResult load(@NotNull IPWorld world, @NotNull Vector2DiConst chunk);

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
