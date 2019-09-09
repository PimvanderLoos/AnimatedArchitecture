package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.api.PSound;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.TimerTask;

public class VerticalMover extends BlockMover
{
    private int tickRate;

    public VerticalMover(final double time, final @NotNull AbstractDoorBase door, final boolean skipAnimation,
                         final int blocksToMove, final double multiplier, final @Nullable IPPlayer player,
                         final @NotNull Vector3Di finalMin, final @NotNull Vector3Di finalMax)
    {
        super(door, time, skipAnimation, PBlockFace.UP, RotateDirection.NONE, blocksToMove, player, finalMin,
              finalMax);

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

        // If the non-default exceeds the max-speed or isn't set, calculate default
        // speed.
        if (time == 0.0 || speed > maxSpeed)
        {
            speed = blocksToMove < 0 ? 1.7 : 0.8 * pcMult;
            speed = speed > maxSpeed ? maxSpeed : speed;
            this.time = Math.abs(blocksToMove) / speed;
        }

        tickRate = Util.tickRateFromSpeed(speed);

        super.constructFBlocks();
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
        BigDoors.get().getPlatform().newPExecutor().runAsyncRepeated(new TimerTask()
        {
            double counter = 0;
            int endCount = (int) (20 / tickRate * time);
            double step = ((double) getBlocksMoved()) / (endCount);
            double stepSum = 0;
            int totalTicks = (int) (endCount * 1.1);
            long startTime = System.nanoTime();
            long lastTime;
            long currentTime = System.nanoTime();
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

                if (counter > totalTicks || firstBlockData == null ||
                    isAborted.get())
                {
                    playSound(PSound.THUD, 2f, 0.15f);
                    for (PBlockData block : savedBlocks)
                        block.getFBlock().setVelocity(new Vector3Dd(0D, 0D, 0D));

                    BigDoors.get().getPlatform().newPExecutor().runSync(() -> putBlocks(false));
                    cancel();
                }
                else
                {
//                    // This isn't used currently, but the idea is to spawn solid blocks where this door is / is going to be.
//                    // A cheap way to create fake solid blocks. Should really be part of the blocks themselves, but
//                    // this was just to see how viable it is. Leaving it here for future reference.
//                    BigDoors.get().getPlatform().newPExecutor().runSync(
//                        () ->
//                        {
//                            int fullBlocksMoved = (int) Math.round(stepSum + Math.max(0.5, 2 * step));
//                            Vector3Di newMin = new Vector3Di(door.getMinimum().getX(),
//                                                             door.getMinimum().getY() + fullBlocksMoved,
//                                                             door.getMinimum().getZ());
//                            Vector3Di newMax = new Vector3Di(door.getMaximum().getX(),
//                                                             door.getMaximum().getY() + fullBlocksMoved,
//                                                             door.getMaximum().getZ());
//                        updateSolidBlocks(newMin, newMax);
//                        });

                    Vector3Dd pos = firstBlockData.getStartPosition();
                    pos.add(0, stepSum, 0);
                    Vector3Dd vec = pos.subtract(firstBlockData.getFBlock().getPosition());
                    vec.multiply(0.101);

                    for (PBlockData mbd : savedBlocks)
                        mbd.getFBlock().setVelocity(vec);
                }
            }
        }, 14, tickRate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IPLocation getNewLocation(double radius, double xAxis, double yAxis, double zAxis)
    {
        return locationFactory.create(world, xAxis, yAxis + blocksMoved, zAxis);
    }
}
