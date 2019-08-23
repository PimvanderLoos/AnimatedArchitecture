package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.doors.HorizontalAxisAlignedBase;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class WindmillMover extends BridgeMover
{
    public WindmillMover(final @NotNull HorizontalAxisAlignedBase door, final double time, final double multiplier,
                         final @NotNull RotateDirection rotateDirection, final @Nullable UUID playerUUID)
    {
        super(time, door, PBlockFace.NONE, rotateDirection, false, multiplier, playerUUID, door.getMinimum(),
              door.getMaximum());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Location getNewLocation(double radius, double xAxis, double yAxis, double zAxis)
    {
        return new Location(world, xAxis, yAxis, zAxis);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void animateEntities()
    {
        new BukkitRunnable()
        {
            boolean replace = false;
            double counter = 0;
            int endCount = (int) (20 / tickRate * time) * 4;
            double step = (Math.PI / 2) / endCount;
            // Add a half a second or the smallest number of ticks closest to it to the timer
            // to make sure the animation doesn't jump at the end.
            int totalTicks = endCount + Math.max(1, 10 / tickRate);
            int replaceCount = endCount / 2;
            long startTime = System.nanoTime();
            long lastTime;
            long currentTime = System.nanoTime();
            final double eps = 2 * Double.MIN_VALUE;

            @Override
            public void run()
            {
                ++counter;
                lastTime = currentTime;
                currentTime = System.nanoTime();
                startTime += currentTime - lastTime;
                replace = counter == replaceCount;
                double stepSum = step * Math.min(counter, endCount);

                if (isAborted.get() || counter > totalTicks)
                {
                    for (PBlockData block : savedBlocks)
                        block.getFBlock().setVelocity(new Vector(0D, 0D, 0D));
                    Bukkit.getScheduler().callSyncMethod(plugin, () ->
                    {
                        putBlocks(false);
                        return null;
                    });
                    cancel();
                }
                else
                {
                    for (PBlockData block : savedBlocks)
                    {
                        if (Math.abs(block.getRadius()) > eps)
                        {
                            Vector vec = getVector.apply(block, stepSum)
                                                  .subtract(block.getFBlock().getLocation().toVector());
                            vec.multiply(0.101);
                            block.getFBlock().setVelocity(vec);
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 14, tickRate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected float getRadius(final int xAxis, final int yAxis, final int zAxis)
    {
        // Get the current radius of a block between used axis (either x and y, or z and y).
        // When the engine is positioned along the NS axis, the X values does not change for this type.
        double deltaA = (door.getEngine().getY() - yAxis);
        double deltaB = !NS ? (door.getEngine().getX() - xAxis) : (door.getEngine().getZ() - zAxis);
        return (float) Math.sqrt(Math.pow(deltaA, 2) + Math.pow(deltaB, 2));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected float getStartAngle(final int xAxis, final int yAxis, final int zAxis)
    {
        // Get the angle between the used axes (either x and y, or z and y).
        // When the engine is positioned along the NS axis, the X values does not change for this type.
        float deltaA = !NS ? door.getEngine().getBlockX() - xAxis : door.getEngine().getBlockZ() - zAxis;
        float deltaB = door.getEngine().getBlockY() - yAxis;

//        float startAngle = (float) Util.clampAngleRad(Math.atan2(deltaA, deltaB));
//        Bukkit.broadcastMessage(
//            door.getWorld().getBlockAt(xAxis, yAxis, zAxis).getType().name() + ": start angle: " + startAngle);
//        return startAngle;

        return (float) Util.clampAngleRad(Math.atan2(deltaA, deltaB));
    }
}
