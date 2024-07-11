package nl.pim16aap2.animatedarchitecture.core.util;

import lombok.experimental.UtilityClass;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector2Di;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;

/**
 * Utility class for operations related to locations (and chunks).
 */
@Flogger
@UtilityClass
public final class LocationUtil
{
    /**
     * Gets the ID of the chunk associated with a position.
     *
     * @param position
     *     The position from which to retrieve the chunk.
     * @return The ID of the chunk.
     */
    public static long getChunkId(Vector3Di position)
    {
        return getChunkId(getChunkCoords(position));
    }

    /**
     * Gets the ID of the chunk from its coordinates.
     *
     * @param chunkCoords
     *     The coordinates of the chunk.
     * @return The ID of the chunk.
     */
    public static long getChunkId(Vector2Di chunkCoords)
    {
        return getChunkId(chunkCoords.x(), chunkCoords.y());
    }

    /**
     * Gets the ID of the chunk from its coordinates.
     * <p>
     * The upper 32 bits store the x-coordinate of the chunk, and the lower 32 bits store the z coordinate.
     *
     * @param chunkX
     *     The x-coordinate of the chunk.
     * @param chunkZ
     *     The z-coordinate of the chunk.
     * @return The ID of the chunk.
     */
    public static long getChunkId(int chunkX, int chunkZ)
    {
        return (((long) chunkX) << 32) | (chunkZ & 0xffffffffL);
    }

    /**
     * Retrieves the chunk coordinates from the chunk's ID. See {@link #getChunkId(Vector2Di)}.
     *
     * @param chunkId
     *     The ID of the chunk.
     * @return The x/z coordinates of the chunk.
     */
    public static Vector2Di getChunkFromId(long chunkId)
    {
        final int chunkX = (int) (chunkId >> 32);
        final int chunkZ = (int) chunkId;
        return new Vector2Di(chunkX, chunkZ);
    }

    /**
     * Gets the chunk coordinates of a position.
     *
     * @param position
     *     The position.
     * @return The chunk coordinates.
     */
    public static Vector2Di getChunkCoords(Vector3Di position)
    {
        return new Vector2Di(position.x() >> 4, position.z() >> 4);
    }

    /**
     * Gets the 'simple' hash of a location in chunk-space. 'simple' here refers to the fact that the world of this
     * location will not be taken into account.
     *
     * @param x
     *     The x-coordinate of the location.
     * @param y
     *     The z-coordinate of the location.
     * @param z
     *     The z-coordinate of the location.
     * @return The simple hash of the location in chunk-space.
     */
    public static int simpleChunkSpaceLocationHash(int x, int y, int z)
    {
        final int chunkSpaceX = x % 16;
        final int chunkSpaceZ = z % 16;
        return (y << 8) + (chunkSpaceX << 4) + chunkSpaceZ;
    }
}
