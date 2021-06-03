package nl.pim16aap2.bigdoors.doors.garagedoor;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPLocationConst;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.api.PSound;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.CuboidConst;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PSoundDescription;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.jetbrains.annotations.NotNull;

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
    private final @NotNull Vector3DiConst directionVec;
    private BiFunction<PBlockData, Double, Vector3Dd> getVector;
    private int xLen, yLen, zLen;
    private boolean NS = false;
    protected int blocksToMove;

    private double step;

    public GarageDoorMover(final @NotNull GarageDoor door, final double time, final double multiplier,
                           final boolean skipAnimation, final @NotNull RotateDirection rotateDirection,
                           final @NotNull IPPlayer player, final @NotNull CuboidConst newCuboid,
                           final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType)
        throws Exception
    {
        super(door, time, skipAnimation, rotateDirection, player, newCuboid, cause, actionType);

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
                BigDoors.get().getPLogger().dumpStackTrace("Failed to open garage door \"" + getDoorUID()
                                                               + "\". Reason: Invalid rotateDirection \"" +
                                                               rotateDirection.toString() + "\"");
                return;
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
            blocksToMove = Math.abs((xLen + 1) * directionVec.getX()
                                        + (yLen + 1) * directionVec.getY()
                                        + (zLen + 1) * directionVec.getZ());
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

    private @NotNull Vector3Dd getVectorUp(final @NotNull PBlockData block, final double stepSum)
    {
        final double currentHeight = Math.min(resultHeight, block.getStartY() + stepSum);
        double xMod = 0;
        double yMod = stepSum;
        double zMod = 0;

        if (currentHeight >= door.getMaximum().getY())
        {
            final double horizontal = Math.max(0, stepSum - block.getRadius() - 0.5);
            xMod = directionVec.getX() * horizontal;
            yMod = Math.min(resultHeight - block.getStartY(), stepSum);
            zMod = directionVec.getZ() * horizontal;
        }
        return new Vector3Dd(block.getStartX() + xMod, block.getStartY() + yMod, block.getStartZ() + zMod);
    }

    private @NotNull Vector3Dd getVectorDownNorth(final @NotNull PBlockData block, final double stepSum)
    {
        final double goalZ = door.getEngine().getZ();
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

    private @NotNull Vector3Dd getVectorDownSouth(final @NotNull PBlockData block, final double stepSum)
    {
        final double goalZ = door.getEngine().getZ();
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

    private @NotNull Vector3Dd getVectorDownEast(final @NotNull PBlockData block, final double stepSum)
    {
        final double goalX = door.getEngine().getX();
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

    private @NotNull Vector3Dd getVectorDownWest(final @NotNull PBlockData block, final double stepSum)
    {
        final double goalX = door.getEngine().getX();
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
    protected @NotNull IPLocation getNewLocation(final double radius, final double xAxis, final double yAxis,
                                                 final double zAxis)
    {
        double newX, newY, newZ;

        if (!door.isOpen())
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

    @Override
    protected @NotNull Vector3Dd getFinalPosition(final @NotNull PBlockData block)
    {
        final @NotNull IPLocationConst startLocation = block.getStartLocation();
        final @NotNull IPLocationConst finalLoc = getNewLocation(block.getRadius(), startLocation.getX(),
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
            final float height = door.getMaximum().getY();
            return height - yAxis;
        }

        final int dX = Math.abs(xAxis - door.getEngine().getX());
        final int dZ = Math.abs(zAxis - door.getEngine().getZ());
        return Math.abs(dX * directionVec.getX() + dZ * directionVec.getZ());
    }
}
