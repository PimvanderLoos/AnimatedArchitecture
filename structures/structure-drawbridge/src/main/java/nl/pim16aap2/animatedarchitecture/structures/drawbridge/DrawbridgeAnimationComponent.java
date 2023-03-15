package nl.pim16aap2.animatedarchitecture.structures.drawbridge;

import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.AnimationUtil;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.Animator;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.IAnimator;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.functional.TriFunction;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;

/**
 * Represents a {@link Animator} for {@link Drawbridge}s.
 *
 * @author Pim
 */
public class DrawbridgeAnimationComponent implements IAnimationComponent
{
    private final Vector3Dd rotationCenter;
    private final boolean northSouth;
    private final TriFunction<Vector3Dd, Vector3Dd, Double, Vector3Dd> rotator;
    private final StructureSnapshot snapshot;

    private final double angle;
    private final double step;
    private final int rotateCount;
    private final int rotateCountOffset;
    private final MovementDirection movementDirection;

    public DrawbridgeAnimationComponent(
        AnimationRequestData data, MovementDirection movementDirection, boolean isNorthSouthAligned, int quarterCircles)
    {
        this.snapshot = data.getStructureSnapshot();
        this.northSouth = isNorthSouthAligned;
        this.rotationCenter = snapshot.getRotationPoint().toDouble().add(0.5, 0, 0.5);
        this.movementDirection = movementDirection;

        switch (movementDirection)
        {
            case NORTH ->
            {
                angle = quarterCircles * -MathUtil.HALF_PI;
                rotator = Vector3Dd::rotateAroundXAxis;
            }
            case SOUTH ->
            {
                angle = quarterCircles * MathUtil.HALF_PI;
                rotator = Vector3Dd::rotateAroundXAxis;
            }
            case EAST ->
            {
                angle = quarterCircles * MathUtil.HALF_PI;
                rotator = Vector3Dd::rotateAroundZAxis;
            }
            case WEST ->
            {
                angle = quarterCircles * -MathUtil.HALF_PI;
                rotator = Vector3Dd::rotateAroundZAxis;
            }
            default -> throw new IllegalArgumentException("Movement direction \"" + movementDirection.name() +
                                                              "\" is not valid for this type!");
        }

        final int animationDuration =
            AnimationUtil.getAnimationTicks(data.getAnimationTime(), data.getServerTickTime());

        this.step = angle / animationDuration;
        this.rotateCount = animationDuration / quarterCircles;
        this.rotateCountOffset = this.rotateCount / 2;
    }

    @Override
    public Vector3Dd getRotationPoint()
    {
        return rotationCenter;
    }

    @Override
    public RotatedPosition getStartPosition(int xAxis, int yAxis, int zAxis)
    {
        return getGoalPos(0, xAxis + 0.5, yAxis, zAxis + 0.5);
    }

    protected Vector3Dd getGoalRotation(Vector3Dd goalPos)
    {
        final Vector3Dd vec = rotationCenter.subtract(goalPos);
        double pitch = 0;
        double roll = 0;
        if (northSouth)
            pitch = Math.toDegrees(-Math.atan2(vec.y(), vec.z()) + MathUtil.HALF_PI);
        else
            roll = -Math.toDegrees(-Math.atan2(vec.y(), vec.x()) + MathUtil.HALF_PI);

        return new Vector3Dd(roll, pitch, 0);
    }

    protected RotatedPosition getGoalPos(double angle, double x, double y, double z)
    {
        final Vector3Dd goalPos = rotator.apply(new Vector3Dd(x, y, z), rotationCenter, angle);
        final Vector3Dd goalRot = getGoalRotation(goalPos);
        return new RotatedPosition(goalPos, goalRot);
    }

    protected RotatedPosition getGoalPos(double angle, IAnimatedBlock animatedBlock)
    {
        return getGoalPos(angle, animatedBlock.getStartX(), animatedBlock.getStartY(), animatedBlock.getStartZ());
    }

    @Override
    public Vector3Dd getFinalPosition(IVector3D startLocation, float radius)
    {
        return getFinalPosition0(startLocation, radius).position();
    }

    @Override
    public RotatedPosition getFinalPosition0(IVector3D startLocation, float radius)
    {
        return getGoalPos(
            Util.clampAngleRad(angle), startLocation.xD() + 0.5, startLocation.yD(), startLocation.zD() + 0.5);
    }

    @Override
    public void executeAnimationStep(IAnimator animator, int ticks, int ticksRemaining)
    {
        final double stepSum = Util.clampAngleRad(step * ticks);

        if ((ticks - rotateCountOffset) % rotateCount == 0)
            animator.applyRotation(this.movementDirection);

        for (final IAnimatedBlock animatedBlock : animator.getAnimatedBlocks())
            animator.applyMovement(animatedBlock, getGoalPos(stepSum, animatedBlock), ticksRemaining);
    }

    public static float getRadius(boolean northSouthAligned, IVector3D rotationPoint, int xAxis, int yAxis, int zAxis)
    {
        // Get the current radius of a block between used axis (either x and y, or z and y).
        // When the rotation point is positioned along the NS axis, the Z values does not change.
        final double deltaA = rotationPoint.yD() - yAxis;
        final double deltaB = northSouthAligned ? (rotationPoint.zD() - zAxis) : (rotationPoint.xD() - xAxis);
        return (float) Math.sqrt(Math.pow(deltaA, 2) + Math.pow(deltaB, 2));
    }

    @Override
    public float getRadius(int xAxis, int yAxis, int zAxis)
    {
        return getRadius(northSouth, snapshot.getRotationPoint(), xAxis, yAxis, zAxis);
    }

    @Override
    public float getStartAngle(int xAxis, int yAxis, int zAxis)
    {
        // Get the angle between the used axes (either x and y, or z and y).
        // When the rotation point is positioned along the NS axis, the Z values does not change.
        final double deltaA = northSouth ?
                              snapshot.getRotationPoint().z() - zAxis :
                              snapshot.getRotationPoint().x() - xAxis;
        final double deltaB = snapshot.getRotationPoint().y() - yAxis;
        return (float) Util.clampAngleRad(Math.atan2(deltaA, deltaB));
    }
}
