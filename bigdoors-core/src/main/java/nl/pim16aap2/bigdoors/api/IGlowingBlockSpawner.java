package nl.pim16aap2.bigdoors.api;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.util.IGlowingBlock;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
     * @param player   The player who will see the glowing block.
     * @param world    The world in which the glowing block will be spawned
     * @param time     How long the glowing block will be visible.
     * @param timeUnit The unit of the <code>time</code>. Note that one tick is 50ms, so that is the absolute lowest
     *                 value possible! Any values less than that will mean nothing is spawned.
     * @param x        The x-coordinate of the glowing block. An offset of 0.5 is applied to make it align by default.
     * @param y        The y-coordinate of the glowing block.
     * @param z        The z-coordinate of the glowing block. An offset of 0.5 is applied to make it align by default.
     * @param color    The color of the outline.
     * @return The {@link IGlowingBlock} that was spawned.
     */
    @NonNull Optional<IGlowingBlock> spawnGlowingBlock(@NonNull IPPlayer player, @NonNull IPWorld world,
                                                       int time, @NonNull TimeUnit timeUnit, double x,
                                                       double y, double z, @NonNull PColor color);

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
    default @NonNull Optional<IGlowingBlock> spawnGlowingBlock(@NonNull IPPlayer player,
                                                               @NonNull IPWorld world, int time,
                                                               double x, double y, double z,
                                                               @NonNull PColor color)
    {
        return spawnGlowingBlock(player, world, time, TimeUnit.SECONDS, x, y, z, color);
    }

    /**
     * Spawns a glowing block.
     *
     * @param player   The player who will see the glowing block.
     * @param world    The world in which the glowing block will be spawned
     * @param time     How long the glowing block will be visible.
     * @param timeUnit The unit of the <code>time</code>. Note that one tick is 50ms, so that is the absolute lowest
     *                 value possible! Any values less than that will mean nothing is spawned.
     * @param x        The x-coordinate of the glowing block. An offset of 0.5 is applied to make it align by default.
     * @param y        The y-coordinate of the glowing block.
     * @param z        The z-coordinate of the glowing block. An offset of 0.5 is applied to make it align by default.
     * @return The {@link IGlowingBlock} that was spawned.
     */
    default @NonNull Optional<IGlowingBlock> spawnGlowingBlock(@NonNull IPPlayer player,
                                                               @NonNull IPWorld world, int time,
                                                               @NonNull TimeUnit timeUnit, double x,
                                                               double y, double z)
    {
        return spawnGlowingBlock(player, world, time, timeUnit, x, y, z, PColor.WHITE);
    }

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
    default @NonNull Optional<IGlowingBlock> spawnGlowingBlock(@NonNull IPPlayer player,
                                                               @NonNull IPWorld world, int time,
                                                               double x, double y, double z)
    {
        return spawnGlowingBlock(player, world, time, x, y, z, PColor.WHITE);
    }

    /**
     * Spawns a glowing block.
     *
     * @param player   The player who will see the glowing block.
     * @param time     How long the glowing block will be visible (in seconds).
     * @param timeUnit The unit of the <code>time</code>. Note that one tick is 50ms, so that is the absolute lowest
     *                 value possible! Any values less than that will mean nothing is spawned.
     * @param location The location where the glowing block will be spawned.
     * @return The {@link IGlowingBlock} that was spawned.
     */
    default @NonNull Optional<IGlowingBlock> spawnGlowingBlock(@NonNull IPPlayer player, int time,
                                                               @NonNull TimeUnit timeUnit,
                                                               @NonNull IPLocationConst location)
    {
        return spawnGlowingBlock(player, location.getWorld(), time, timeUnit, location.getX(), location.getY(),
                                 location.getZ());
    }

    /**
     * Spawns a glowing block.
     *
     * @param player   The player who will see the glowing block.
     * @param time     How long the glowing block will be visible (in seconds).
     * @param location The location where the glowing block will be spawned.
     * @return The {@link IGlowingBlock} that was spawned.
     */
    default @NonNull Optional<IGlowingBlock> spawnGlowingBlock(@NonNull IPPlayer player, int time,
                                                               @NonNull IPLocationConst location)
    {
        return spawnGlowingBlock(player, location.getWorld(), time, location.getX(), location.getY(), location.getZ());
    }

    /**
     * Spawns a glowing block.
     *
     * @param player   The player who will see the glowing block.
     * @param location The location where the glowing block will be spawned.
     * @param time     How long the glowing block will be visible (in seconds).
     * @param timeUnit The unit of the <code>time</code>. Note that one tick is 50ms, so that is the absolute lowest
     *                 value possible! Any values less than that will mean nothing is spawned.
     * @param color    The color of the outline.
     * @return The {@link IGlowingBlock} that was spawned.
     */
    default @NonNull Optional<IGlowingBlock> spawnGlowingBlock(@NonNull IPPlayer player, int time,
                                                               @NonNull TimeUnit timeUnit,
                                                               @NonNull IPLocationConst location,
                                                               @NonNull PColor color)
    {
        return spawnGlowingBlock(player, location.getWorld(), time, timeUnit, location.getX(), location.getY(),
                                 location.getZ(), color);
    }

    /**
     * Spawns a glowing block.
     *
     * @param player   The player who will see the glowing block.
     * @param location The location where the glowing block will be spawned.
     * @param time     How long the glowing block will be visible (in seconds).
     * @param color    The color of the outline.
     * @return The {@link IGlowingBlock} that was spawned.
     */
    default @NonNull Optional<IGlowingBlock> spawnGlowingBlock(@NonNull IPPlayer player, int time,
                                                               @NonNull IPLocationConst location,
                                                               @NonNull PColor color)
    {
        return spawnGlowingBlock(player, location.getWorld(), time, location.getX(), location.getY(), location.getZ(),
                                 color);
    }
}
