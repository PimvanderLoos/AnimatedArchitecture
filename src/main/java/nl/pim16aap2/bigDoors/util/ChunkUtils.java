/**
 *
 */
package nl.pim16aap2.bigDoors.util;

import org.bukkit.Location;

/**
 *
 * @author Pim
 */
public final class ChunkUtils
{
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
}
