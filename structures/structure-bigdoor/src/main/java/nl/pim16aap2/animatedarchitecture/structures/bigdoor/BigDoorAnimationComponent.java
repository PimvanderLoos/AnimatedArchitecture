package nl.pim16aap2.animatedarchitecture.structures.bigdoor;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.AnimationUtil;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.IAnimator;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;

@Flogger
public class BigDoorAnimationComponent implements IAnimationComponent
{
    private final MovementDirection movementDirection;
    private final StructureSnapshot snapshot;
    private final Vector3Dd rotationCenter;
    private final int rotateCount;
    private final int rotateCountOffset;
    private final double angle;
    private final double step;

    public BigDoorAnimationComponent(AnimationRequestData data, MovementDirection movementDirection, int quarterCircles)
    {
        this.snapshot = data.getStructureSnapshot();
        this.movementDirection = movementDirection;

        angle = movementDirection == MovementDirection.CLOCKWISE ? quarterCircles * MathUtil.HALF_PI :
                movementDirection == MovementDirection.COUNTERCLOCKWISE ? quarterCircles * -MathUtil.HALF_PI :
                0.0D;

        if (angle == 0.0D)
            log.atSevere()
               .log("Invalid open direction '%s' for structure: %d", movementDirection.name(), snapshot.getUid());

        rotationCenter = new Vector3Dd(
            snapshot.getRotationPoint().x() + 0.5,
            snapshot.getCuboid().getMin().y(),
            snapshot.getRotationPoint().z() + 0.5);

        final int animationDuration =
            AnimationUtil.getAnimationTicks(data.getAnimationTime(), data.getServerTickTime());

        this.step = this.angle / animationDuration;
        this.rotateCount = animationDuration / quarterCircles;
        this.rotateCountOffset = this.rotateCount / 2;
    }

    @Override
    public Vector3Dd getFinalPosition(IVector3D startLocation, float radius)
    {
        return
            getGoalPos(Util.clampAngleRad(angle), startLocation.xD(), startLocation.yD(), startLocation.zD())
                .position();
    }

    @Override
    public void executeAnimationStep(IAnimator animator, int ticks, int ticksRemaining)
    {
        if ((ticks - rotateCountOffset) % rotateCount == 0)
            animator.applyRotation(movementDirection);

        final double stepSum = Util.clampAngleRad(step * ticks);
        final double cos = Math.cos(stepSum);
        final double sin = Math.sin(stepSum);

        for (final IAnimatedBlock animatedBlock : animator.getAnimatedBlocks())
            animator.applyMovement(animatedBlock, getGoalPos(animatedBlock, cos, sin), ticksRemaining);
    }

    private RotatedPosition getGoalPos(double cos, double sin, double startX, double startY, double startZ)
    {
        final double translatedX = startX - rotationCenter.x();
        final double translatedZ = startZ - rotationCenter.z();

        final double changeX = translatedX * cos - translatedZ * sin;
        final double changeZ = translatedX * sin + translatedZ * cos;

        final Vector3Dd goalPos = new Vector3Dd(rotationCenter.x() + changeX, startY, rotationCenter.z() + changeZ);
        final Vector3Dd goalRotation = getGoalRotation(goalPos);

        return new RotatedPosition(goalPos, goalRotation);
    }

    private Vector3Dd getGoalRotation(Vector3Dd goalPos)
    {
        final double yaw =
            -Math.atan2(rotationCenter.x() - goalPos.x(), rotationCenter.z() - goalPos.z()) + MathUtil.HALF_PI;
        return new Vector3Dd(0, 0, Math.toDegrees(yaw));
    }

    private RotatedPosition getGoalPos(double angle, double startX, double startY, double startZ)
    {
        return getGoalPos(Math.cos(angle), Math.sin(angle), startX, startY, startZ);
    }

    private RotatedPosition getGoalPos(IAnimatedBlock animatedBlock, double cos, double sin)
    {
        return getGoalPos(cos, sin, animatedBlock.getStartX(), animatedBlock.getStartY(), animatedBlock.getStartZ());
    }

    static float getRadius(IVector3D rotationPoint, int xAxis, int zAxis)
    {
        final double deltaA = rotationPoint.xD() - xAxis;
        final double deltaB = rotationPoint.zD() - zAxis;
        return (float) Math.sqrt(Math.pow(deltaA, 2) + Math.pow(deltaB, 2));
    }

    @Override
    public float getRadius(int xAxis, int yAxis, int zAxis)
    {
        return getRadius(snapshot.getRotationPoint(), xAxis, zAxis);
    }

    @Override
    public float getStartAngle(int xAxis, int yAxis, int zAxis)
    {
        return (float) Math.atan2(snapshot.getRotationPoint().xD() - xAxis, snapshot.getRotationPoint().zD() - zAxis);
    }
}
