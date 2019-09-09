package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.api.PSound;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
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

    public GarageDoorMover(final @NotNull AbstractDoorBase door, final double time, final double multiplier,
                           final boolean skipAnimation, final @NotNull PBlockFace currentDirection,
                           final @NotNull RotateDirection rotateDirection, final @Nullable IPPlayer player,
                           final @NotNull Vector3Di finalMin, final @NotNull Vector3Di finalMax)
    {
        super(door, 30, skipAnimation, currentDirection, rotateDirection, -1, player, finalMin, finalMax);
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
                break;
            case EAST:
                directionVec = PBlockFace.getDirection(PBlockFace.EAST);
                getVectorTmp = this::getVectorDownEast;
                break;
            case SOUTH:
                directionVec = PBlockFace.getDirection(PBlockFace.SOUTH);
                getVectorTmp = this::getVectorDownSouth;
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
        double goalZ = door.getEngine().getZ() - 0.5;
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
        double goalZ = door.getEngine().getZ() - 0.5;
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
        double goalX = door.getEngine().getX() - 0.5;
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

        double goalX = door.getEngine().getX() - 0.5;
        double pivotX = goalX + 1.5;
        double currentX = Math.max(goalX, block.getStartX() - stepSum);

        double xMod = -stepSum;
        double yMod = 0;
        double zMod = 0;
        boolean test = false;

        if (currentX <= pivotX)
        {
            test = true;

            xMod = Math.max(goalX - block.getStartLocation().getX() + 0.5, xMod);
            yMod = -Math.max(0, stepSum - block.getRadius() + 0.5);
        }

//        SpigotUtil.broadcastMessage(String.format(
//            "%.5b: goalX: %.2f, pivotX: %.2f, currentX: %.2f, stepSum: %.2f, "
//                + "yMod: %.2f, xMod: %.2f, radius: %.2f, startX: %.2f, BTM: %.1f, test: %.2f",
//            test, goalX, pivotX, currentX, stepSum, yMod, xMod, block.getRadius(),
//            block.getStartX(), getBlocksMoved(), (goalX - block.getStartLocation().getX() + 0.5)));

        return new Vector3Dd(block.getStartX() + xMod, block.getStartY() + yMod, block.getStartZ() + zMod);
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


//        Location newLoc = new Location(world, newX, newY, newZ);
//        newLoc.getBlock().setType(Material.LAPIS_BLOCK);
//        SpigotUtil.broadcastMessage("newLoc: " + SpigotUtil.locIntToString(newLoc));
//        return new Location(world, xAxis, yAxis, zAxis);
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
        BigDoors.get().getPlatform().newPExecutor().runAsyncRepeated(new TimerTask()
        {
            double counter = 0;
            int endCount = (int) (20 / tickRate * time);
            int totalTicks = (int) (endCount * 1.1);
            long startTime = System.nanoTime();
            long lastTime;
            long currentTime = System.nanoTime();
            double step = getBlocksMoved() / endCount;
            double stepSum = 0;

            @Override
            public void run()
            {
                ++counter;
                lastTime = currentTime;
                currentTime = System.nanoTime();
                startTime += currentTime - lastTime;

                if (counter > totalTicks || isAborted.get())
                {
                    playSound(PSound.THUD, 0.2f, 0.15f);
                    for (PBlockData block : savedBlocks)
                        block.getFBlock().setVelocity(new Vector3Dd(0D, 0D, 0D));

                    BigDoors.get().getPlatform().newPExecutor().runSync(() -> putBlocks(false));
                    cancel();
                }
                else
                {
                    stepSum = counter * step;
                    for (PBlockData block : savedBlocks)
                    {
                        Vector3Dd vec = getVector.apply(block, counter).subtract(block.getFBlock().getPosition());
                        block.getFBlock().setVelocity(vec.multiply(0.101));
                    }
                }
            }
        }, 14, tickRate);
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
