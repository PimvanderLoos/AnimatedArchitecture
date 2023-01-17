package nl.pim16aap2.bigdoors.movable.garagedoor;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionCause;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionType;
import nl.pim16aap2.bigdoors.movable.MovableSnapshot;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.IVector3D;
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
    private final double resultHeight;
    private final Vector3Di directionVec;
    private final BiFunction<IAnimatedBlock, Double, Vector3Dd> getVector;
    private final boolean northSouth;
    private final double step;
    private final boolean isOpen;

    public GarageDoorMover(
        Context context, GarageDoor movable, MovableSnapshot snapshot, double time, double multiplier,
        boolean skipAnimation, RotateDirection rotateDirection, IPPlayer player, Cuboid newCuboid,
        MovableActionCause cause, MovableActionType actionType)
        throws Exception
    {
        super(context, movable, snapshot, time, skipAnimation, rotateDirection, player, newCuboid, cause, actionType);

        isOpen = snapshot.isOpen();

        resultHeight = oldCuboid.getMax().y() + 1.0D;

        BiFunction<IAnimatedBlock, Double, Vector3Dd> getVectorTmp;
        switch (rotateDirection)
        {
            case NORTH:
                directionVec = PBlockFace.getDirection(PBlockFace.NORTH);
                getVectorTmp = this::getVectorDownNorth;
                northSouth = true;
                break;
            case EAST:
                directionVec = PBlockFace.getDirection(PBlockFace.EAST);
                getVectorTmp = this::getVectorDownEast;
                northSouth = false;
                break;
            case SOUTH:
                directionVec = PBlockFace.getDirection(PBlockFace.SOUTH);
                getVectorTmp = this::getVectorDownSouth;
                northSouth = true;
                break;
            case WEST:
                directionVec = PBlockFace.getDirection(PBlockFace.WEST);
                getVectorTmp = this::getVectorDownWest;
                northSouth = false;
                break;
            default:
                throw new IllegalStateException("Failed to open garage door \"" + getMovableUID()
                                                    + "\". Reason: Invalid rotateDirection \"" +
                                                    rotateDirection + "\"");
        }

        final Vector3Di dims = oldCuboid.getDimensions();
        int blocksToMove;
        if (!movable.isOpen())
        {
            blocksToMove = dims.y();
            getVector = this::getVectorUp;
        }
        else
        {
            blocksToMove = Math.abs(dims.x() * directionVec.x()
                                        + dims.y() * directionVec.y()
                                        + dims.z() * directionVec.z());
            getVector = getVectorTmp;
        }

        step = (blocksToMove + 0.5f) / super.animationDuration;
    }

    private Vector3Dd getVectorUp(IAnimatedBlock animatedBlock, double stepSum)
    {
        final double currentHeight = Math.min(resultHeight, animatedBlock.getStartY() + stepSum);
        double xMod = 0;
        double yMod = stepSum;
        double zMod = 0;

        if (currentHeight >= oldCuboid.getMax().y())
        {
            final double horizontal = Math.max(0, stepSum - animatedBlock.getRadius() - 0.5);
            xMod = directionVec.x() * horizontal;
            yMod = Math.min(resultHeight - animatedBlock.getStartY(), stepSum);
            zMod = directionVec.z() * horizontal;
        }
        return new Vector3Dd(animatedBlock.getStartX() + xMod, animatedBlock.getStartY() + yMod,
                             animatedBlock.getStartZ() + zMod);
    }

    private Vector3Dd getVectorDownNorth(IAnimatedBlock animatedBlock, double stepSum)
    {
        final double goalZ = snapshot.getRotationPoint().z();
        final double pivotZ = goalZ + 1.5;
        final double currentZ = Math.max(goalZ, animatedBlock.getStartZ() - stepSum);

        final double xMod = 0;
        double yMod = 0;
        double zMod = -stepSum;

        if (currentZ <= pivotZ)
        {
            yMod = -Math.max(0, stepSum - animatedBlock.getRadius() + 0.5);
            zMod = Math.max(goalZ - animatedBlock.getStartPosition().z() + 0.5, zMod);
        }

        return new Vector3Dd(animatedBlock.getStartX() + xMod, animatedBlock.getStartY() + yMod,
                             animatedBlock.getStartZ() + zMod);
    }

    private Vector3Dd getVectorDownSouth(IAnimatedBlock animatedBlock, double stepSum)
    {
        final double goalZ = snapshot.getRotationPoint().z();
        final double pivotZ = goalZ - 1.5;
        final double currentZ = Math.min(goalZ, animatedBlock.getStartZ() + stepSum);

        final double xMod = 0;
        double yMod = 0;
        double zMod = stepSum;

        if (currentZ >= pivotZ)
        {
            yMod = -Math.max(0, stepSum - animatedBlock.getRadius() + 0.5);
            zMod = Math.min(goalZ - animatedBlock.getStartPosition().z() + 0.5, zMod);
        }
        return new Vector3Dd(animatedBlock.getStartX() + xMod, animatedBlock.getStartY() + yMod,
                             animatedBlock.getStartZ() + zMod);
    }

    private Vector3Dd getVectorDownEast(IAnimatedBlock animatedBlock, double stepSum)
    {
        final double goalX = snapshot.getRotationPoint().x();
        final double pivotX = goalX - 1.5;
        final double currentX = Math.min(goalX, animatedBlock.getStartX() + stepSum);

        double xMod = stepSum;
        double yMod = 0;
        final double zMod = 0;

        if (currentX >= pivotX)
        {
            xMod = Math.min(goalX - animatedBlock.getStartPosition().x() + 0.5, xMod);
            yMod = -Math.max(0, stepSum - animatedBlock.getRadius() + 0.5);
        }
        return new Vector3Dd(animatedBlock.getStartX() + xMod, animatedBlock.getStartY() + yMod,
                             animatedBlock.getStartZ() + zMod);
    }

    private Vector3Dd getVectorDownWest(IAnimatedBlock animatedBlock, double stepSum)
    {
        final double goalX = snapshot.getRotationPoint().x();
        final double pivotX = goalX + 1.5;
        final double currentX = Math.max(goalX, animatedBlock.getStartX() - stepSum);

        double xMod = -stepSum;
        double yMod = 0;
        final double zMod = 0;

        if (currentX <= pivotX)
        {
            xMod = Math.max(goalX - animatedBlock.getStartPosition().x() + 0.5, xMod);
            yMod = -Math.max(0, stepSum - animatedBlock.getRadius() + 0.5);
        }

        return new Vector3Dd(animatedBlock.getStartX() + xMod, animatedBlock.getStartY() + yMod,
                             animatedBlock.getStartZ() + zMod);
    }

    @Override
    protected Vector3Dd getFinalPosition(IVector3D startLocation, float radius)
    {
        double newX;
        double newY;
        double newZ;

        final Vector3Di dims = oldCuboid.getDimensions();

        if (!isOpen)
        {
            newX = startLocation.xD() + (dims.y() - radius) * directionVec.x();
            newY = resultHeight;
            newZ = startLocation.zD() + (dims.y() - radius) * directionVec.z();
        }
        else
        {
            if (directionVec.x() == 0)
            {
                newX = startLocation.xD();
                newY = oldCuboid.getMax().y() - (dims.z() - radius);
                newZ = snapshot.getRotationPoint().z();
            }
            else
            {
                newX = snapshot.getRotationPoint().x();
                newY = oldCuboid.getMax().y() - (dims.x() - radius);
                newZ = startLocation.zD();
            }
            newY -= 2;

            newX += northSouth ? 0 : 0.5f;
            newZ += northSouth ? 0.5f : 0;
        }
        return new Vector3Dd(newX, newY, newZ);
    }

    @Override
    protected void executeAnimationStep(int ticks, int ticksRemaining)
    {
        final double stepSum = step * ticks;
        for (final IAnimatedBlock animatedBlock : getAnimatedBlocks())
            applyMovement(animatedBlock, getVector.apply(animatedBlock, stepSum), ticksRemaining);
    }

    @Override
    protected float getRadius(int xAxis, int yAxis, int zAxis)
    {
        if (!isOpen)
        {
            final float height = oldCuboid.getMax().y();
            return height - yAxis;
        }

        final int dX = Math.abs(xAxis - snapshot.getRotationPoint().x());
        final int dZ = Math.abs(zAxis - snapshot.getRotationPoint().z());
        return Math.abs(dX * directionVec.x() + dZ * directionVec.z());
    }
}
