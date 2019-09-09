package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.api.PSound;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.moveblocks.getnewlocation.GNLHorizontalRotEast;
import nl.pim16aap2.bigdoors.moveblocks.getnewlocation.GNLHorizontalRotNorth;
import nl.pim16aap2.bigdoors.moveblocks.getnewlocation.GNLHorizontalRotSouth;
import nl.pim16aap2.bigdoors.moveblocks.getnewlocation.GNLHorizontalRotWest;
import nl.pim16aap2.bigdoors.moveblocks.getnewlocation.IGetNewLocation;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.TimerTask;

public class CylindricalMover extends BlockMover
{
    private final int tickRate;
    private final int stepMultiplier;
    private final Vector3Di turningPoint;
    private final IGetNewLocation gnl;
    private double endStepSum;
    private double multiplier;
    private double startStepSum;

    public CylindricalMover(final @NotNull RotateDirection rotDirection, final double time,
                            final @NotNull PBlockFace currentDirection, final @NotNull AbstractDoorBase door,
                            final boolean skipAnimation, final double multiplier, @Nullable final IPPlayer player,
                            final @NotNull Vector3Di finalMin, final @NotNull Vector3Di finalMax)
    {
        super(door, time, skipAnimation, currentDirection, rotDirection, -1, player, finalMin, finalMax);

        turningPoint = door.getEngine();
        stepMultiplier = rotDirection == RotateDirection.CLOCKWISE ? -1 : 1;

        int xLen = Math.abs(door.getMaximum().getX() - door.getMinimum().getX());
        int zLen = Math.abs(door.getMaximum().getZ() - door.getMinimum().getZ());
        int doorLength = Math.max(xLen, zLen) + 1;
        double vars[] = Util.calculateTimeAndTickRate(doorLength, time, multiplier, 3.7);
        this.time = vars[0];
        tickRate = (int) vars[1];
        this.multiplier = vars[2];

        switch (currentDirection)
        {
            case NORTH:
                gnl = new GNLHorizontalRotNorth(world, xMin, xMax, zMin, zMax, rotDirection);
                startStepSum = Math.PI;
                endStepSum = rotDirection == RotateDirection.CLOCKWISE ? Math.PI / 2 : 3 * Math.PI / 2;
                break;
            case EAST:
                gnl = new GNLHorizontalRotEast(world, xMin, xMax, zMin, zMax, rotDirection);
                startStepSum = Math.PI / 2;
                endStepSum = rotDirection == RotateDirection.CLOCKWISE ? 0 : Math.PI;
                break;
            case SOUTH:
                gnl = new GNLHorizontalRotSouth(world, xMin, xMax, zMin, zMax, rotDirection);
                startStepSum = 0;
                endStepSum = rotDirection == RotateDirection.CLOCKWISE ? 3 * Math.PI / 2 : Math.PI / 2;
                break;
            case WEST:
                gnl = new GNLHorizontalRotWest(world, xMin, xMax, zMin, zMax, rotDirection);
                startStepSum = 3 * Math.PI / 2;
                endStepSum = rotDirection == RotateDirection.CLOCKWISE ? Math.PI : 0;
                break;
            default:
                PLogger.get()
                       .dumpStackTrace(
                           "Invalid currentDirection for cylindrical mover: " + currentDirection.toString());
                gnl = null;
                break;
        }

        super.constructFBlocks();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void animateEntities()
    {
        BigDoors.get().getPlatform().newPExecutor().runAsyncRepeated(new TimerTask()
        {
            IPLocation center = locationFactory.create(world, turningPoint.getX() + 0.5, yMin,
                                                       turningPoint.getZ() + 0.5);
            boolean replace = false;
            double counter = 0;
            int endCount = (int) (20 / tickRate * time);
            double step = (Math.PI / 2) / endCount * stepMultiplier;
            double stepSum = startStepSum;
            int totalTicks = (int) (endCount * multiplier);
            int replaceCount = endCount / 2;
            Long startTime = null; // Initialize on the first run, for better accuracy.
            long lastTime;
            long currentTime = System.nanoTime();

            @Override
            public void run()
            {
                if (startTime == null)
                    startTime = System.nanoTime();
                ++counter;

                if (counter == 0 || (counter < endCount - 27 / tickRate && counter % (5 * tickRate / 4) == 0))
                    playSound(PSound.DRAGGING, 0.5f, 0.6f);

                lastTime = currentTime;
                currentTime = System.nanoTime();
                startTime += currentTime - lastTime;

                if (counter < endCount - 1)
                    stepSum = startStepSum + step * counter;
                else
                    stepSum = endStepSum;

                replace = (counter == replaceCount);

                if (counter > totalTicks || isAborted.get())
                {
                    playSound(PSound.CLOSING_VAULT_DOOR, 0.2f, 1f);

                    for (PBlockData savedBlock : savedBlocks)
                        savedBlock.getFBlock().setVelocity(new Vector3Dd(0D, 0D, 0D));
                    BigDoors.get().getPlatform().newPExecutor().runSync(() -> putBlocks(false));
                    cancel();
                }
                else
                {
                    // It is not possible to edit falling block blockdata (client won't update it),
                    // so delete the current fBlock and replace it by one that's been rotated.
                    // Also, this stuff needs to be done on the main thread.
                    if (replace)
                        BigDoors.get().getPlatform().newPExecutor().runSync(() -> respawnBlocks());

                    double sin = Math.sin(stepSum);
                    double cos = Math.cos(stepSum);

                    for (PBlockData block : savedBlocks)
                    {
                        double radius = block.getRadius();
                        int yPos = block.getStartLocation().getBlockY();

                        if (radius != 0)
                        {
                            double addX = radius * sin;
                            double addZ = radius * cos;

                            Vector3Dd position = new Vector3Dd(center.getX() + addX, yPos, center.getZ() + addZ);
                            Vector3Dd vec = position.subtract(block.getFBlock().getPosition());
                            block.getFBlock().setVelocity(vec.multiply(0.101));
                        }
                    }
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
        return gnl.getNewLocation(radius, xAxis, yAxis, zAxis);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected float getRadius(int xAxis, int yAxis, int zAxis)
    {
        // Get the radius of this pillar.
        return Math.max(Math.abs(xAxis - turningPoint.getX()), Math.abs(zAxis - turningPoint.getZ()));
    }
}
