package nl.pim16aap2.bigdoors.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     * @param player The player who will see the glowing block.
     * @param world  The world in which the glowing block will be spawned
     * @param time   How long the glowing block will be visible (in seconds).
     * @param x      The x-coordinate of the glowing block. An offset of 0.5 is applied to make it align by default.
     * @param y      The y-coordinate of the glowing block.
     * @param z      The z-coordinate of the glowing block. An offset of 0.5 is applied to make it align by default.
     * @return The {@link IGlowingBlock} that was spawned.
     */
    @Nullable
    IGlowingBlock spawnGlowinBlock(final @NotNull IPPlayer player, @NotNull UUID world, final int time, final double x,
                                   final double y, final double z);

    /**
     * Spawns a glowing block.
     *
     * @param player The player who will see the glowing block.
     * @param world  The world in which the glowing block will be spawned
     * @param time   How long the glowing block will be visible (in seconds).
     * @param x      The x-coordinate of the glowing block. An offset of 0.5 is applied to make it align by default.
     * @param y      The y-coordinate of the glowing block.
     * @param z      The z-coordinate of the glowing block. An offset of 0.5 is applied to make it align by default.
     * @param color  The color of the outline.
     * @return The {@link IGlowingBlock} that was spawned.
     */
    @Nullable
    IGlowingBlock spawnGlowinBlock(final @NotNull IPPlayer player, @NotNull UUID world, final int time, final double x,
                                   final double y, final double z, final @NotNull PColor color);
}
