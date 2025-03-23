package nl.pim16aap2.animatedarchitecture.structures.garagedoor;

import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationUtil;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimator;
import nl.pim16aap2.animatedarchitecture.core.animation.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;

import java.util.function.BiFunction;

/**
 * Represents an {@link IAnimationComponent} for {@link GarageDoor} structure types.
 * <p>
 * This component moves the garage door with individual sections.
 */
@Flogger
@ToString(callSuper = true)
public final class SectionalGarageDoorAnimationComponent extends CounterWeightGarageDoorAnimationComponent
{
    private final double resultHeight;
    private final BiFunction<IAnimatedBlock, Double, RotatedPosition> getVector;
    private final double step;

    public SectionalGarageDoorAnimationComponent(AnimationRequestData data, MovementDirection movementDirection)
    {
        super(data, movementDirection);

        resultHeight = oldCuboid.getMax().y() + 1.0;

        final BiFunction<IAnimatedBlock, Double, RotatedPosition> getVectorTmp;
        switch (movementDirection)
        {
            case NORTH -> getVectorTmp = this::getVectorDownNorth;
            case EAST -> getVectorTmp = this::getVectorDownEast;
            case SOUTH -> getVectorTmp = this::getVectorDownSouth;
            case WEST -> getVectorTmp = this::getVectorDownWest;
            default -> throw new IllegalStateException(String.format(
                "Failed to open garage door '%s'. Reason: Invalid movement direction '%s'",
                snapshot.getUid(), movementDirection));
        }

        final Vector3Di dims = oldCuboid.getDimensions();
        final int blocksToMove;
        if (oldCuboid.getDimensions().y() > 1)
        {
            blocksToMove = dims.y();
            getVector = this::getVectorUp;
        }
        else
        {
            blocksToMove = dims.multiply(directionVec).absolute().sum();
            getVector = getVectorTmp;
        }

        final int animationDuration =
            AnimationUtil.getAnimationTicks(data.getAnimationTime(), data.getServerTickTime());
        step = (blocksToMove + 0.5f) / animationDuration;
    }

    private RotatedPosition getVectorUp(IAnimatedBlock animatedBlock, double stepSum)
    {
        final double currentHeight = Math.min(resultHeight, animatedBlock.getStartY() + stepSum);
        double xMod = 0;
        double yMod = stepSum;
        double zMod = 0;

        if (currentHeight >= oldCuboid.getMax().y())
        {
            final double horizontal = Math.max(0, stepSum - animatedBlock.getRadius() + 0.5);
            xMod = directionVec.x() * horizontal;
            yMod = Math.min(resultHeight - animatedBlock.getStartY(), stepSum);
            zMod = directionVec.z() * horizontal;
        }
        return new RotatedPosition(
            animatedBlock.getStartX() + xMod,
            animatedBlock.getStartY() + yMod,
            animatedBlock.getStartZ() + zMod);
    }

    private RotatedPosition getVectorDownNorth(IAnimatedBlock animatedBlock, double stepSum)
    {
        final double goalZ = rotationPoint.z();
        final double pivotZ = goalZ + 1.5;
        final double currentZ = Math.max(goalZ, animatedBlock.getStartZ() - stepSum);

        final double xMod = 0;
        double yMod = 0;
        double zMod = -stepSum;

        if (currentZ <= pivotZ)
        {
            yMod = -Math.max(0, stepSum - animatedBlock.getRadius() + 0.5);
            zMod = Math.max(goalZ - animatedBlock.getStartPosition().position().z() + 0.5, zMod);
        }

        return new RotatedPosition(
            animatedBlock.getStartX() + xMod,
            animatedBlock.getStartY() + yMod,
            animatedBlock.getStartZ() + zMod);
    }

    private RotatedPosition getVectorDownSouth(IAnimatedBlock animatedBlock, double stepSum)
    {
        final double goalZ = rotationPoint.z();
        final double pivotZ = goalZ - 1.5;
        final double currentZ = Math.min(goalZ, animatedBlock.getStartZ() + stepSum);

        final double xMod = 0;
        double yMod = 0;
        double zMod = stepSum;

        if (currentZ >= pivotZ)
        {
            yMod = -Math.max(0, stepSum - animatedBlock.getRadius() + 0.5);
            zMod = Math.min(goalZ - animatedBlock.getStartPosition().position().z() + 0.5, zMod);
        }
        return new RotatedPosition(
            animatedBlock.getStartX() + xMod,
            animatedBlock.getStartY() + yMod,
            animatedBlock.getStartZ() + zMod);
    }

    private RotatedPosition getVectorDownEast(IAnimatedBlock animatedBlock, double stepSum)
    {
        final double goalX = rotationPoint.x();
        final double pivotX = goalX - 1.5;
        final double currentX = Math.min(goalX, animatedBlock.getStartX() + stepSum);

        double xMod = stepSum;
        double yMod = 0;
        final double zMod = 0;

        if (currentX >= pivotX)
        {
            xMod = Math.min(goalX - animatedBlock.getStartPosition().position().x() + 0.5, xMod);
            yMod = -Math.max(0, stepSum - animatedBlock.getRadius() + 0.5);
        }
        return new RotatedPosition(
            animatedBlock.getStartX() + xMod,
            animatedBlock.getStartY() + yMod,
            animatedBlock.getStartZ() + zMod);
    }

    private RotatedPosition getVectorDownWest(IAnimatedBlock animatedBlock, double stepSum)
    {
        final double goalX = rotationPoint.x();
        final double pivotX = goalX + 1.5;
        final double currentX = Math.max(goalX, animatedBlock.getStartX() - stepSum);

        double xMod = -stepSum;
        double yMod = 0;
        final double zMod = 0;

        if (currentX <= pivotX)
        {
            xMod = Math.max(goalX - animatedBlock.getStartPosition().position().x() + 0.5, xMod);
            yMod = -Math.max(0, stepSum - animatedBlock.getRadius() + 0.5);
        }

        return new RotatedPosition(
            animatedBlock.getStartX() + xMod,
            animatedBlock.getStartY() + yMod,
            animatedBlock.getStartZ() + zMod);
    }

    @Override
    public void executeAnimationStep(IAnimator animator, Iterable<IAnimatedBlock> animatedBlocks, int ticks)
    {
        final double stepSum = step * ticks;

        for (final IAnimatedBlock animatedBlock : animatedBlocks)
            animator.applyMovement(animatedBlock, getVector.apply(animatedBlock, stepSum));
    }
}
