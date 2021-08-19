package nl.pim16aap2.bigdoors.doors.garagedoor;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.api.PSound;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PSoundDescription;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

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
    private final double resultHeight;
    private final Vector3Di directionVec;
    private BiFunction<PBlockData, Double, Vector3Dd> getVector;
    private int xLen, yLen, zLen;
    private boolean NS = false;
    protected int blocksToMove;

    private double step;

    public GarageDoorMover(final GarageDoor door, final double time, final double multiplier,
                           final boolean skipAnimation, final RotateDirection rotateDirection,
                           final IPPlayer player, final Cuboid newCuboid,
                           final DoorActionCause cause, final DoorActionType actionType)
        throws Exception
    {
        super(door, time, skipAnimation, rotateDirection, player, newCuboid, cause, actionType);

        resultHeight = door.getMaximum().y() + 1;

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
                throw new IllegalStateException("Failed to open garage door \"" + getDoorUID()
                                                    + "\". Reason: Invalid rotateDirection \"" +
                                                    rotateDirection + "\"");
        }

        xLen = xMax - xMin;
        yLen = yMax - yMin;
        zLen = zMax - zMin;

        if (!door.isOpen())
        {
            blocksToMove = yLen + 1;
            getVector = this::getVectorUp;
        }
        else
        {
            blocksToMove = Math.abs((xLen + 1) * directionVec.x()
                                        + (yLen + 1) * directionVec.y()
                                        + (zLen + 1) * directionVec.z());
            getVector = getVectorTmp;
        }

        init();
        super.startAnimation();
    }

    /**
     * Used for initializing variables such as {@link #endCount} and {@link #soundActive}.
     */
    protected void init()
    {
        super.endCount = (int) (20 * super.time);
        step = (blocksToMove + 0.5) / ((float) super.endCount);
        super.soundActive = new PSoundDescription(PSound.DRAWBRIDGE_RATTLING, 0.8f, 0.7f);
        super.soundFinish = new PSoundDescription(PSound.THUD, 0.2f, 0.15f);
    }

    private Vector3Dd getVectorUp(final PBlockData block, final double stepSum)
    {
        final double currentHeight = Math.min(resultHeight, block.getStartY() + stepSum);
        double xMod = 0;
        double yMod = stepSum;
        double zMod = 0;

        if (currentHeight >= door.getMaximum().y())
        {
            final double horizontal = Math.max(0, stepSum - block.getRadius() - 0.5);
            xMod = directionVec.x() * horizontal;
            yMod = Math.min(resultHeight - block.getStartY(), stepSum);
            zMod = directionVec.z() * horizontal;
        }
        return new Vector3Dd(block.getStartX() + xMod, block.getStartY() + yMod, block.getStartZ() + zMod);
    }

    private Vector3Dd getVectorDownNorth(final PBlockData block, final double stepSum)
    {
        final double goalZ = door.getEngine().z();
        final double pivotZ = goalZ + 1.5;
        final double currentZ = Math.max(goalZ, block.getStartZ() - stepSum);

        final double xMod = 0;
        double yMod = 0;
        double zMod = -stepSum;

        if (currentZ <= pivotZ)
        {
            yMod = -Math.max(0, stepSum - block.getRadius() + 0.5);
            zMod = Math.max(goalZ - block.getStartLocation().getZ() + 0.5, zMod);
        }

        return new Vector3Dd(block.getStartX() + xMod, block.getStartY() + yMod, block.getStartZ() + zMod);
    }

    private Vector3Dd getVectorDownSouth(final PBlockData block, final double stepSum)
    {
        final double goalZ = door.getEngine().z();
        final double pivotZ = goalZ - 1.5;
        final double currentZ = Math.min(goalZ, block.getStartZ() + stepSum);

        final double xMod = 0;
        double yMod = 0;
        double zMod = stepSum;

        if (currentZ >= pivotZ)
        {
            yMod = -Math.max(0, stepSum - block.getRadius() + 0.5);
            zMod = Math.min(goalZ - block.getStartLocation().getZ() + 0.5, zMod);
        }
        return new Vector3Dd(block.getStartX() + xMod, block.getStartY() + yMod, block.getStartZ() + zMod);
    }

    private Vector3Dd getVectorDownEast(final PBlockData block, final double stepSum)
    {
        final double goalX = door.getEngine().x();
        final double pivotX = goalX - 1.5;
        final double currentX = Math.min(goalX, block.getStartX() + stepSum);

        double xMod = stepSum;
        double yMod = 0;
        final double zMod = 0;

        if (currentX >= pivotX)
        {
            xMod = Math.min(goalX - block.getStartLocation().getX() + 0.5, xMod);
            yMod = -Math.max(0, stepSum - block.getRadius() + 0.5);
        }
        return new Vector3Dd(block.getStartX() + xMod, block.getStartY() + yMod, block.getStartZ() + zMod);
    }

    private Vector3Dd getVectorDownWest(final PBlockData block, final double stepSum)
    {
        final double goalX = door.getEngine().x();
        final double pivotX = goalX + 1.5;
        final double currentX = Math.max(goalX, block.getStartX() - stepSum);

        double xMod = -stepSum;
        double yMod = 0;
        final double zMod = 0;

        if (currentX <= pivotX)
        {
            xMod = Math.max(goalX - block.getStartLocation().getX() + 0.5, xMod);
            yMod = -Math.max(0, stepSum - block.getRadius() + 0.5);
        }

        return new Vector3Dd(block.getStartX() + xMod, block.getStartY() + yMod, block.getStartZ() + zMod);
    }

    @Override
    protected IPLocation getNewLocation(final double radius, final double xAxis, final double yAxis,
                                        final double zAxis)
    {
        double newX, newY, newZ;

        if (!door.isOpen())
        {
            newX = xAxis + (1 + yLen - radius) * directionVec.x();
            newY = resultHeight;
            newZ = zAxis + (1 + yLen - radius) * directionVec.z();
        }
        else
        {
            if (directionVec.x() == 0)
            {
                newX = xAxis;
                newY = door.getMaximum().y() - (zLen - radius);
                newZ = door.getEngine().z();
            }
            else
            {
                newX = door.getEngine().x();
                newY = door.getMaximum().y() - (xLen - radius);
                newZ = zAxis;
            }
            newY -= 2;
        }
        return locationFactory.create(world, newX, newY, newZ);
    }

    @Override
    protected Vector3Dd getFinalPosition(final PBlockData block)
    {
        final IPLocation startLocation = block.getStartLocation();
        final IPLocation finalLoc = getNewLocation(block.getRadius(), startLocation.getX(),
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

    @Override
    protected void executeAnimationStep(final int ticks)
    {
        final double stepSum = step * ticks;
        for (final PBlockData block : savedBlocks)
            block.getFBlock().teleport(getVector.apply(block, stepSum));
    }

    @Override
    protected float getRadius(final int xAxis, final int yAxis, final int zAxis)
    {
        if (!door.isOpen())
        {
            final float height = door.getMaximum().y();
            return height - yAxis;
        }

        final int dX = Math.abs(xAxis - door.getEngine().x());
        final int dZ = Math.abs(zAxis - door.getEngine().z());
        return Math.abs(dX * directionVec.x() + dZ * directionVec.z());
    }
}
