package nl.pim16aap2.bigdoors.movable.revolvingdoor;

import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.movable.MovableSnapshot;
import nl.pim16aap2.bigdoors.moveblocks.AnimationUtil;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.moveblocks.IAnimationComponent;
import nl.pim16aap2.bigdoors.moveblocks.IAnimator;
import nl.pim16aap2.bigdoors.moveblocks.MovementRequestData;
import nl.pim16aap2.bigdoors.util.MathUtil;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import nl.pim16aap2.bigdoors.util.vector.IVector3D;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;

import java.util.function.BiFunction;

/**
 * Represents a {@link BlockMover} for {@link RevolvingDoor}s.
 *
 * @author Pim
 */
public class RevolvingDoorAnimationComponent implements IAnimationComponent
{
    private final BiFunction<IAnimatedBlock, Double, Vector3Dd> getGoalPos;
    private final MovableSnapshot snapshot;
    private final double step;
    private final double endStepSum;
    private final MovementDirection openDirection;

    public RevolvingDoorAnimationComponent(
        MovementRequestData data, MovementDirection movementDirection, int quarterCircles)
    {
        this.snapshot = data.getSnapshotOfMovable();
        this.openDirection = movementDirection;

        switch (movementDirection)
        {
            case CLOCKWISE -> getGoalPos = this::getGoalPosClockwise;
            case COUNTERCLOCKWISE -> getGoalPos = this::getGoalPosCounterClockwise;
            default -> throw new IllegalStateException(
                String.format("Failed to open movable '%d'. Reason: Invalid movement direction '%s'",
                              snapshot.getUid(), movementDirection.name()));
        }

        final double animationDuration =
            AnimationUtil.getAnimationTicks(data.getAnimationTime(), data.getServerTickTime());
        step = (MathUtil.HALF_PI * quarterCircles) / animationDuration * -1.0;
        endStepSum = animationDuration * step;
    }

    private Vector3Dd getGoalPosClockwise(double radius, double startAngle, double startY, double stepSum)
    {
        final double posX = 0.5 + snapshot.getRotationPoint().xD() - radius * Math.sin(startAngle + stepSum);
        final double posZ = 0.5 + snapshot.getRotationPoint().zD() - radius * Math.cos(startAngle + stepSum);
        return new Vector3Dd(posX, startY, posZ);
    }

    private Vector3Dd getGoalPosClockwise(IAnimatedBlock animatedBlock, double stepSum)
    {
        return getGoalPosClockwise(animatedBlock.getRadius(), animatedBlock.getStartAngle(),
                                   animatedBlock.getStartY(),
                                   stepSum);
    }

    private Vector3Dd getGoalPosCounterClockwise(double radius, double startAngle, double startY, double stepSum)
    {
        final double posX = 0.5 + snapshot.getRotationPoint().xD() - radius * Math.sin(startAngle - stepSum);
        final double posZ = 0.5 + snapshot.getRotationPoint().zD() - radius * Math.cos(startAngle - stepSum);
        return new Vector3Dd(posX, startY, posZ);
    }

    private Vector3Dd getGoalPosCounterClockwise(IAnimatedBlock animatedBlock, double stepSum)
    {
        return getGoalPosCounterClockwise(animatedBlock.getRadius(), animatedBlock.getStartAngle(),
                                          animatedBlock.getStartY(), stepSum);
    }

    @Override
    public Vector3Dd getFinalPosition(IVector3D startLocation, float radius)
    {
        // TODO: Redo all this, it's too hacky.
        final double startAngle = getStartAngle((int) startLocation.xD(),
                                                (int) startLocation.yD(),
                                                (int) startLocation.zD());

        if (openDirection == MovementDirection.CLOCKWISE)
            return getGoalPosClockwise(radius, startAngle, startLocation.yD(), endStepSum);
        return getGoalPosCounterClockwise(radius, startAngle, startLocation.yD(), endStepSum);
    }

    @Override
    public void executeAnimationStep(IAnimator animator, int ticks, int ticksRemaining)
    {
        final double stepSum = step * ticks;

        for (final IAnimatedBlock animatedBlock : animator.getAnimatedBlocks())
            animator.applyMovement(animatedBlock, getGoalPos.apply(animatedBlock, stepSum), ticksRemaining);
    }

    @Override
    public float getRadius(int xAxis, int yAxis, int zAxis)
    {
        final double deltaA = snapshot.getRotationPoint().xD() - xAxis;
        final double deltaB = snapshot.getRotationPoint().zD() - zAxis;
        return (float) Math.sqrt(Math.pow(deltaA, 2) + Math.pow(deltaB, 2));
    }

    @Override
    public float getStartAngle(int xAxis, int yAxis, int zAxis)
    {
        return (float) Math.atan2(snapshot.getRotationPoint().xD() - xAxis,
                                  snapshot.getRotationPoint().zD() - zAxis);
    }
}
