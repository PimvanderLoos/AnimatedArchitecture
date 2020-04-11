package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.api.PSound;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.GarageDoor;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.TimerTask;
import java.util.function.BiFunction;

/**
 * Represents a {@link BlockMover} for {@link GarageDoor}s.
 *
 * @author Pim
 */
public class GarageDoorMover extends BlockMover
{
    private static final double maxSpeed = 3;
    private static final double minSpeed = 0.1;
    private final double time;
    private final double resultHeight;
    private final Vector3Di directionVec;
    private int tickRate;
    private BiFunction<PBlockData, Double, Vector3Dd> getVector;
    private int xLen, yLen, zLen;
    private boolean NS = false;

    public GarageDoorMover(final @NotNull AbstractDoorBase door, final double time, final double multiplier,
                           final boolean skipAnimation, final @NotNull PBlockFace currentDirection,
                           final @NotNull RotateDirection rotateDirection, final @Nullable IPPlayer player,
                           final @NotNull Vector3Di finalMin, final @NotNull Vector3Di finalMax)
    {
        super(door, time, skipAnimation, currentDirection, rotateDirection, -1, player, finalMin, finalMax);
        this.time = time;

        double speed = 1 * multiplier;
        speed = speed > maxSpeed ? 3 : Math.max(speed, minSpeed);
        tickRate = Util.tickRateFromSpeed(speed);
        tickRate = 3;

        resultHeight = door.getMaximum().getY() + 1;

        BiFunction<PBlockData, Double, Vector3Dd> getVectorTmp;
        switch (rotateDirection)
        {
            case NORTH:
                directionVec = PBlockFace.getDirection(PBlockFace.NORTH);
                getVectorTmp = this::getVectorDownNorth;
                NS = true;
                break;
            case EAST:
                directionVec = PBlockFace.getDirection(PBlockFace.EAST);
                getVectorTmp = this::getVectorDownEast;
                break;
            case SOUTH:
                directionVec = PBlockFace.getDirection(PBlockFace.SOUTH);
                getVectorTmp = this::getVectorDownSouth;
                NS = true;
                break;
            case WEST:
                directionVec = PBlockFace.getDirection(PBlockFace.WEST);
                getVectorTmp = this::getVectorDownWest;
                break;
            default:
                directionVec = null;
                PLogger.get().dumpStackTrace("Failed to open garage door \"" + getDoorUID()
                                                 + "\". Reason: Invalid rotateDirection \"" +
                                                 rotateDirection.toString() + "\"");
                return;
        }

        xLen = xMax - xMin;
        yLen = yMax - yMin;
        zLen = zMax - zMin;

        if (currentDirection.equals(PBlockFace.UP))
        {
            super.blocksMoved = yLen + 1;
            getVector = this::getVectorUp;
        }
        else
        {
            super.blocksMoved = (xLen + 1) * directionVec.getX() + (yLen + 1) * directionVec.getY()
                + (zLen + 1) * directionVec.getZ();
            getVector = getVectorTmp;
        }

        super.constructFBlocks();
    }

    private Vector3Dd getVectorUp(final @NotNull PBlockData block, final double stepSum)
    {
        double currentHeight = Math.min(resultHeight, block.getStartY() + stepSum);
        double xMod = 0;
        double yMod = stepSum;
        double zMod = 0;

        if (currentHeight >= door.getMaximum().getY())
        {
            double horizontal = Math.max(0, stepSum - block.getRadius() - 0.5);
            xMod = directionVec.getX() * horizontal;
            yMod = Math.min(resultHeight - block.getStartY(), stepSum);
            zMod = directionVec.getZ() * horizontal;
        }
        return new Vector3Dd(block.getStartX() + xMod, block.getStartY() + yMod, block.getStartZ() + zMod);
    }

    private Vector3Dd getVectorDownNorth(final @NotNull PBlockData block, final double stepSum)
    {
        double goalZ = door.getEngine().getZ();
        double pivotZ = goalZ + 1.5;
        double currentZ = Math.max(goalZ, block.getStartZ() - stepSum);

        double xMod = 0;
        double yMod = 0;
        double zMod = -stepSum;

        if (currentZ <= pivotZ)
        {
            yMod = -Math.max(0, stepSum - block.getRadius() + 0.5);
            zMod = Math.max(goalZ - block.getStartLocation().getZ() + 0.5, zMod);
        }

        return new Vector3Dd(block.getStartX() + xMod, block.getStartY() + yMod, block.getStartZ() + zMod);
    }

    private Vector3Dd getVectorDownSouth(final @NotNull PBlockData block, final double stepSum)
    {
        double goalZ = door.getEngine().getZ();
        double pivotZ = goalZ - 1.5;
        double currentZ = Math.min(goalZ, block.getStartZ() + stepSum);

        double xMod = 0;
        double yMod = 0;
        double zMod = stepSum;

        if (currentZ >= pivotZ)
        {
            yMod = -Math.max(0, stepSum - block.getRadius() + 0.5);
            zMod = Math.min(goalZ - block.getStartLocation().getZ() + 0.5, zMod);
        }
        return new Vector3Dd(block.getStartX() + xMod, block.getStartY() + yMod, block.getStartZ() + zMod);
    }

    private Vector3Dd getVectorDownEast(final @NotNull PBlockData block, final double stepSum)
    {
        double goalX = door.getEngine().getX();
        double pivotX = goalX - 1.5;
        double currentX = Math.min(goalX, block.getStartX() + stepSum);

        double xMod = stepSum;
        double yMod = 0;
        double zMod = 0;

        if (currentX >= pivotX)
        {
            xMod = Math.min(goalX - block.getStartLocation().getX() + 0.5, xMod);
            yMod = -Math.max(0, stepSum - block.getRadius() + 0.5);
        }
        return new Vector3Dd(block.getStartX() + xMod, block.getStartY() + yMod, block.getStartZ() + zMod);
    }

    private Vector3Dd getVectorDownWest(final @NotNull PBlockData block, final double stepSum)
    {
        double goalX = door.getEngine().getX();
        double pivotX = goalX + 1.5;
        double currentX = Math.max(goalX, block.getStartX() - stepSum);

        double xMod = -stepSum;
        double yMod = 0;
        double zMod = 0;

        if (currentX <= pivotX)
        {
            xMod = Math.max(goalX - block.getStartLocation().getX() + 0.5, xMod);
            yMod = -Math.max(0, stepSum - block.getRadius() + 0.5);
        }

        return new Vector3Dd(block.getStartX() + xMod, block.getStartY() + yMod, block.getStartZ() + zMod);
    }

    /**
     * Gets the vector to the final location for a {@link PBlockData}.
     *
     * @param block The {@link PBlockData}.
     * @return The vector towards the final location for a {@link PBlockData}.
     */
    private Vector3Dd getFinalVector(final @NotNull PBlockData block)
    {
        final @NotNull IPLocation startLocation = block.getStartLocation();
        final @NotNull IPLocation finalLoc = getNewLocation(block.getRadius(), startLocation.getX(),
                                                            startLocation.getY(), startLocation.getZ());
        double addX = 0;
        double addZ = 0;
        if (door.isOpen()) // The offset isn't needed when going up.
        {
            addX = NS ? 0 : 0.5f;
            addZ = NS ? 0.5f : 0;
        }
        return new Vector3Dd(finalLoc.getX() + addX, finalLoc.getY(), finalLoc.getZ() + addZ);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IPLocation getNewLocation(final double radius, final double xAxis, final double yAxis, final double zAxis)
    {
        double newX, newY, newZ;

        if (currentDirection.equals(PBlockFace.UP))
        {
            newX = xAxis + (1 + yLen - radius) * directionVec.getX();
            newY = resultHeight;
            newZ = zAxis + (1 + yLen - radius) * directionVec.getZ();
        }
        else
        {
            if (directionVec.getX() == 0)
            {
                newX = xAxis;
                newY = door.getMaximum().getY() - (zLen - radius);
                newZ = door.getEngine().getZ();
            }
            else
            {
                newX = door.getEngine().getX();
                newY = door.getMaximum().getY() - (xLen - radius);
                newZ = zAxis;
            }
            newY -= 2;
        }
        return locationFactory.create(world, newX, newY, newZ);
    }

    private double getBlocksMoved()
    {
        return Math.abs(blocksMoved);
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
            final int endCount = (int) (20.0f / tickRate * time);
            final int totalTicks = endCount + (int) Math.ceil(16.0f / tickRate);
            long startTime = System.nanoTime();
            long lastTime;
            long currentTime = System.nanoTime();
            final double step = getBlocksMoved() / ((float) endCount);
            double stepSum = 0;

            @Override
            public void run()
            {
                ++counter;
                lastTime = currentTime;
                currentTime = System.nanoTime();
                startTime += currentTime - lastTime;

                if (counter > totalTicks)
                {
                    playSound(PSound.THUD, 0.2f, 0.15f);
                    for (PBlockData block : savedBlocks)
                        block.getFBlock().setVelocity(new Vector3Dd(0D, 0D, 0D));

                    final @NotNull IPExecutor<Object> executor = BigDoors.get().getPlatform().newPExecutor();
                    executor.runSync(() -> putBlocks(false));
                    executor.cancel(this, moverTaskID);
                }
                else if (counter > endCount)
                {
                    for (final PBlockData block : savedBlocks)
                    {
                        Vector3Dd vec = getFinalVector(block).subtract(block.getFBlock().getPosition());
                        block.getFBlock().setVelocity(vec.multiply(0.101));
                    }
                }
                else
                {
                    stepSum = counter * step;
                    for (final PBlockData block : savedBlocks)
                    {
                        Vector3Dd vec = getVector.apply(block, stepSum).subtract(block.getFBlock().getPosition());
                        block.getFBlock().setVelocity(vec.multiply(0.101));
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
        if (currentDirection.equals(PBlockFace.UP))
        {
            float height = door.getMaximum().getY();
            return height - yAxis;
        }

        int dX = Math.abs(xAxis - door.getEngine().getX());
        int dZ = Math.abs(zAxis - door.getEngine().getZ());
        return Math.abs(dX * directionVec.getX() + dZ * directionVec.getZ());
    }
}
