package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.api.PSound;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.SlidingDoor;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.TimerTask;

/**
 * Represents a {@link BlockMover} for {@link SlidingDoor}.
 *
 * @author Pim
 */
public class SlidingMover extends BlockMover
{
    private boolean NS;
    private int tickRate;
    private int moveX, moveZ;

    public SlidingMover(final double time, final @NotNull AbstractDoorBase door, final boolean skipAnimation,
                        final int blocksToMove, final @NotNull RotateDirection openDirection, final double multiplier,
                        final @Nullable IPPlayer player, final @NotNull Vector3Di finalMin,
                        final @NotNull Vector3Di finalMax)
    {
        super(door, time, skipAnimation, PBlockFace.UP, openDirection, blocksToMove, player, finalMin, finalMax);

        NS = openDirection.equals(RotateDirection.NORTH) || openDirection.equals(RotateDirection.SOUTH);

        moveX = NS ? 0 : blocksToMove;
        moveZ = NS ? blocksToMove : 0;

        double speed = 1;
        double pcMult = multiplier;
        pcMult = pcMult == 0.0 ? 1.0 : pcMult;
        int maxSpeed = 6;

        // If the time isn't default, calculate speed.
        if (time != 0.0)
        {
            speed = Math.abs(blocksToMove) / time;
            this.time = time;
        }

        // If the non-default exceeds the max-speed or isn't set, calculate default speed.
        if (time == 0.0 || speed > maxSpeed)
        {
            speed = 1.4 * pcMult;
            speed = speed > maxSpeed ? maxSpeed : speed;
            this.time = Math.abs(blocksToMove) / speed;
        }

        tickRate = Util.tickRateFromSpeed(speed);

        super.constructFBlocks();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IPLocation getNewLocation(double radius, double xAxis, double yAxis, double zAxis)
    {
        return locationFactory.create(world, xAxis + moveX, yAxis, zAxis + moveZ);
    }

    private int getBlocksMoved()
    {
        return super.blocksMoved;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void animateEntities()
    {
        super.moverTask = new TimerTask()
        {
            double counter = 0;
            int endCount = (int) (20 / tickRate * time);
            double step = ((double) getBlocksMoved()) / ((double) endCount);
            double stepSum = 0;
            int totalTicks = (int) (endCount * 1.1);
            long startTime = System.nanoTime();
            long lastTime;
            long currentTime = System.nanoTime();
            // TODO: Check if this is still needed. It shouldn't be, as null-entries aren't allowed anymore.
            PBlockData firstBlockData = savedBlocks.stream().filter(block -> block.getFBlock() != null).findFirst()
                                                   .orElse(null);

            @Override
            public void run()
            {
                ++counter;
                if (counter == 0 || (counter < endCount - 27 / tickRate && counter % (5 * tickRate / 4) == 0))
                    playSound(PSound.DRAGGING, 0.5f, 0.6f);

                lastTime = currentTime;
                currentTime = System.nanoTime();
                startTime += currentTime - lastTime;

                if (counter < endCount - 1)
                    stepSum = step * counter;
                else
                    stepSum = getBlocksMoved();

                if (counter > totalTicks ||
                    firstBlockData == null)
                {
                    playSound(PSound.THUD, 2f, 0.15f);
                    for (PBlockData savedBlock : savedBlocks)
                        savedBlock.getFBlock().setVelocity(new Vector3Dd(0D, 0D, 0D));

                    final @NotNull IPExecutor<Object> executor = BigDoors.get().getPlatform().newPExecutor();
                    executor.runSync(() -> putBlocks(false));
                    executor.cancel(this, moverTaskID);
                }
                else
                {
                    Vector3Dd pos = firstBlockData.getStartPosition();

                    if (NS)
                        pos.setZ(pos.getZ() + stepSum);
                    else
                        pos.setX(pos.getX() + stepSum);

                    if (firstBlockData.getStartLocation().getY() != yMin)
                        pos.setY(pos.getY() - .010001);
                    Vector3Dd vec = pos.subtract(firstBlockData.getFBlock().getPosition());
                    vec.multiply(0.101);

                    for (PBlockData block : savedBlocks)
                        block.getFBlock().setVelocity(vec);
                }
            }
        };
        moverTaskID = BigDoors.get().getPlatform().newPExecutor().runAsyncRepeated(moverTask, 14, tickRate);
    }
}
