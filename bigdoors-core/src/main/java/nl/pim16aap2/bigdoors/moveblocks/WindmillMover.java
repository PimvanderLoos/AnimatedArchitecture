package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.doors.HorizontalAxisAlignedBase;
import nl.pim16aap2.bigdoors.doors.Windmill;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.TimerTask;

/**
 * Represents a {@link BlockMover} for {@link Windmill}s.
 *
 * @author Pim
 */
public class WindmillMover extends BridgeMover
{
    public WindmillMover(final @NotNull HorizontalAxisAlignedBase door, final double time, final double multiplier,
                         final @NotNull RotateDirection rotateDirection, final @Nullable IPPlayer player)
    {
        super(time, door, PBlockFace.NONE, rotateDirection, false, multiplier, player, door.getMinimum(),
              door.getMaximum());
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    protected IPLocation getNewLocation(double radius, double xAxis, double yAxis, double zAxis)
    {
        return locationFactory.create(world, xAxis, yAxis, zAxis);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void animateEntities()
    {
        super.moverTask = new TimerTask()
        {
            boolean replace = false;
            double counter = 0;
            int endCount = (int) (20 / tickRate * time) * 20;
            double step = (Math.PI / 2) / ((int) (20 / tickRate * time) * 2);
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

                if (counter > totalTicks)
                {
                    for (PBlockData block : savedBlocks)
                        block.getFBlock().setVelocity(new Vector3Dd(0D, 0D, 0D));

                    final @NotNull IPExecutor<Object> executor = BigDoors.get().getPlatform().newPExecutor();
                    executor.runSync(() -> putBlocks(false));
                    executor.cancel(this, moverTaskID);
                }
                else
                {
                    double stepSum = step * Math.min(counter, endCount);
                    for (PBlockData block : savedBlocks)
                    {
                        // TODO: Store separate list to avoid checking this constantly.
                        if (Math.abs(block.getRadius()) > eps)
                        {
                            Vector3Dd vec = getVector.apply(block, stepSum).subtract(block.getFBlock().getPosition());
                            block.getFBlock().setVelocity(vec.multiply(0.101));
                        }
                    }
                }
            }
        };
        moverTaskID = BigDoors.get().getPlatform().newPExecutor().runAsyncRepeated(moverTask, 14, tickRate);
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
        float deltaA = !NS ? door.getEngine().getX() - xAxis : door.getEngine().getZ() - zAxis;
        float deltaB = door.getEngine().getY() - yAxis;

        return (float) Util.clampAngleRad(Math.atan2(deltaA, deltaB));
    }
}
