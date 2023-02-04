package nl.pim16aap2.bigdoors.structures.garagedoor;

import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.moveblocks.AnimationUtil;
import nl.pim16aap2.bigdoors.moveblocks.Animator;
import nl.pim16aap2.bigdoors.moveblocks.IAnimationComponent;
import nl.pim16aap2.bigdoors.moveblocks.IAnimator;
import nl.pim16aap2.bigdoors.moveblocks.StructureRequestData;
import nl.pim16aap2.bigdoors.structures.StructureSnapshot;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.vector.IVector3D;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

import java.util.function.BiFunction;

/**
 * Represents a {@link Animator} for {@link GarageDoor}s.
 *
 * @author Pim
 */
public final class GarageDoorAnimationComponent implements IAnimationComponent
{
    private final StructureSnapshot snapshot;
    private final double resultHeight;
    private final Vector3Di directionVec;
    private final BiFunction<IAnimatedBlock, Double, Vector3Dd> getVector;
    private final boolean northSouth;
    private final double step;
    private final boolean isOpen;
    private final Cuboid oldCuboid;

    public GarageDoorAnimationComponent(StructureRequestData data, MovementDirection movementDirection)
    {
        this.snapshot = data.getStructureSnapshot();
        this.oldCuboid = snapshot.getCuboid();
        isOpen = snapshot.isOpen();

        resultHeight = oldCuboid.getMax().y() + 1.0D;

        BiFunction<IAnimatedBlock, Double, Vector3Dd> getVectorTmp;
        switch (movementDirection)
        {
            case NORTH ->
            {
                directionVec = PBlockFace.getDirection(PBlockFace.NORTH);
                getVectorTmp = this::getVectorDownNorth;
                northSouth = true;
            }
            case EAST ->
            {
                directionVec = PBlockFace.getDirection(PBlockFace.EAST);
                getVectorTmp = this::getVectorDownEast;
                northSouth = false;
            }
            case SOUTH ->
            {
                directionVec = PBlockFace.getDirection(PBlockFace.SOUTH);
                getVectorTmp = this::getVectorDownSouth;
                northSouth = true;
            }
            case WEST ->
            {
                directionVec = PBlockFace.getDirection(PBlockFace.WEST);
                getVectorTmp = this::getVectorDownWest;
                northSouth = false;
            }
            default -> throw new IllegalStateException("Failed to open garage door \"" + snapshot.getUid()
                                                           + "\". Reason: Invalid movement direction \"" +
                                                           movementDirection + "\"");
        }

        final Vector3Di dims = oldCuboid.getDimensions();
        int blocksToMove;
        if (!snapshot.isOpen())
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

        final int animationDuration =
            AnimationUtil.getAnimationTicks(data.getAnimationTime(), data.getServerTickTime());
        step = (blocksToMove + 0.5f) / animationDuration;
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
    public Vector3Dd getFinalPosition(IVector3D startLocation, float radius)
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
    public void executeAnimationStep(IAnimator animator, int ticks, int ticksRemaining)
    {
        final double stepSum = step * ticks;
        for (final IAnimatedBlock animatedBlock : animator.getAnimatedBlocks())
            animator.applyMovement(animatedBlock, getVector.apply(animatedBlock, stepSum), ticksRemaining);
    }

    @Override
    public float getRadius(int xAxis, int yAxis, int zAxis)
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
