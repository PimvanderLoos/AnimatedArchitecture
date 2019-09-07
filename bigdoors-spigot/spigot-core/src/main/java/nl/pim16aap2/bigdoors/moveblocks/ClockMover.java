package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.doors.HorizontalAxisAlignedBase;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.WorldTime;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Function;

public class ClockMover extends WindmillMover
{
    /**
     * Method to determine if a given {@link PBlockData} is part of the little hand or the big hand of a clock.
     * Represented as a {@link Function} becuase
     */
    @NotNull
    private final Function<PBlockData, Boolean> isLittleHand;
    /**
     * The start position of the clock.
     */
//    private static final float STARTPOINT = (float) Math.PI / 2;
    private static final float STARTPOINT = 0;
    /**
     * The step of 1 minute on a clock, or 1/60th of a circle in radians.
     */
    private static final float MINUTESTEP = (float) Math.PI / 30;
    /**
     * The step of 1 hours on a clock, or 1/12th of a circle in radians.
     */
    private static final float HOURSTEP = (float) Math.PI / 6;
    /**
     * The step of 1 minute between two full ours on a clock, or 1/720th of a circle in radians.
     */
    private static final float HOURSUBSTEP = (float) Math.PI / 360;
    /**
     * Override tickRate to be once per second. It's a clock, it doesn't move wildly (or shouldn't, at least) and
     * therefor doesn't need as frequent updating.
     */
    private static final int tickRate = 1; // TODO: Set this to 20 or something.

    public ClockMover(final @NotNull HorizontalAxisAlignedBase door, final @NotNull RotateDirection rotateDirection,
                      final @Nullable UUID playerUUID)
    {
        super(door, 0.0D, 0.0D, rotateDirection, playerUUID);
        isLittleHand = NS ? this::isLittleHandNS : this::isLittleHandEW;
    }

    /**
     * Checks is a given {@link PBlockData} is the little hand or the big hand.
     *
     * @return True if the block is part of the little hand.
     */
    private boolean isLittleHandNS(final @NotNull PBlockData block)
    {
        // If NS, the clock rotates along the z axis (north south), so the hands are distributed along the x axis.
        // The engine location determines what the front side of the clock is and the little hand is the front side.
        return block.getStartLocation().getBlockX() == door.getEngine().getBlockX();
    }

    /**
     * Checks is a given {@link PBlockData} is the little hand or the big hand.
     *
     * @return True if the block is part of the little hand.
     */
    private boolean isLittleHandEW(final @NotNull PBlockData block)
    {
        // If NS, the clock rotates along the z axis (north south), so the hands are distributed along the x axis.
        // The engine location determines what the front side of the clock is and the little hand is the front side.
        return block.getStartLocation().getBlockZ() == door.getEngine().getBlockZ();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void animateEntities()
    {
        new BukkitRunnable()
        {
            long counter = 0;
            int endCount = (int) (20 / tickRate * time) * 400;
            // Add a half a second or the smallest number of ticks closest to it to the timer
            // to make sure the animation doesn't jump at the end.
            int totalTicks = endCount + Math.max(1, 10 / tickRate);
            final double eps = 2 * Double.MIN_VALUE;
            final World world = door.getWorld();
            boolean moveSmallHand = false;
            long ticks = 0;
            boolean replace = false;

            @Override
            public void run()
            {
                ++counter;

                // Every 12000 ticks or so (when running at 20TPS), the blocks will despawn.
                // So this will preventively respawn them every 8000 ticks (buffer for low TPS servers, though I'm not
                // sure if that's actually needed).
                ticks += tickRate;
                if (ticks > 8000)
                {
                    replace = true;
                    ticks = 0;
                }
                else
                    replace = false;

                WorldTime worldTime = SpigotUtil.getTime(world);
                double hourAngle = ClockMover.hoursToAngle(worldTime.getHours(), worldTime.getMinutes());
                double minuteAngle = ClockMover.minutesToAngle(worldTime.getMinutes());

                moveSmallHand = counter % 10 == 0;

                if (isAborted.get() || counter > totalTicks)
                {
                    for (PBlockData block : savedBlocks)
                        block.getFBlock().setVelocity(new Vector3Dd(0D, 0D, 0D));
                    Bukkit.getScheduler().callSyncMethod(plugin, () ->
                    {
                        putBlocks(false);
                        return null;
                    });
                    cancel();
                }
                else
                {
                    if (replace)
                        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> respawnBlocks(), 0);
                    for (PBlockData block : savedBlocks)
                    {
                        if (Math.abs(block.getRadius()) > eps)
                        {
                            // Move the little hand at a lower interval than the big hand.
                            boolean littleHand = isLittleHand.apply(block);
                            if (!moveSmallHand && littleHand)
                                continue;

                            double timeAngle = littleHand ? hourAngle : minuteAngle;
                            Vector3Dd vec = getVector.apply(block, timeAngle)
                                                     .subtract(block.getFBlock().getPosition());
                            vec.multiply(0.101);
                            block.getFBlock().setVelocity(vec);
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 14, tickRate);
    }

    /**
     * Converts a time in minutes (60 per circle) to an angle in radians, with 0 minutes pointing up, and 30 minutes
     * pointing down.
     *
     * @param minutes The time in minutes since the last full hour.
     * @return The angle.
     */
    private static float minutesToAngle(final int minutes)
    {
        return (float) Util.clampAngleRad(STARTPOINT - minutes * MINUTESTEP);
    }

    /**
     * Converts a time in hours (12 per circle) to an angle in radians, with 0, 12 hours pointing up, and 6, 18 hours
     * pointing down.
     *
     * @param hours   The time in hours.
     * @param minutes The time in minutes since the last full hour.
     * @return The angle.
     */
    private static float hoursToAngle(final int hours, final int minutes)
    {
        return (float) Util.clampAngleRad(STARTPOINT - hours * HOURSTEP - minutes * HOURSUBSTEP);
    }
}
