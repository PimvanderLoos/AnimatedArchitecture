package nl.pim16aap2.bigdoors.api;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents a glowing block used for highlights
 *
 * @author Pim
 */
public interface IGlowingBlockSpawner
{
    /**
     * Spawns a glowing block.
     *
     * @param playerUUID The player who will see the glowing block.
     * @param world      The world in which the glowing block will be spawned
     * @param time       How long the glowing block will be visible (in seconds).
     * @param x          The x-coordinate of the glowing block.
     * @param y          The y-coordinate of the glowing block.
     * @param z          The z-coordinate of the glowing block.
     */
    void spawnGlowinBlock(@NotNull final UUID playerUUID, @NotNull String world, final long time, double x, double y,
                          double z);
}
