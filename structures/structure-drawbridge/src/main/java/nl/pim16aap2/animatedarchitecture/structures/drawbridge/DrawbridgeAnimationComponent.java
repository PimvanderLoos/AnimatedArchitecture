package nl.pim16aap2.animatedarchitecture.structures.drawbridge;

import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationUtil;
import nl.pim16aap2.animatedarchitecture.core.animation.Animator;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimator;
import nl.pim16aap2.animatedarchitecture.core.animation.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockData;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.functional.TriFunction;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

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
    private final MovementDirection movementDirection;
    private final int quarterCircles;

    public DrawbridgeAnimationComponent(
        AnimationRequestData data, MovementDirection movementDirection, boolean isNorthSouthAligned, int quarterCircles)
    {
        this.quarterCircles = quarterCircles;
        this.snapshot = data.getStructureSnapshot();
        this.northSouth = isNorthSouthAligned;
        this.rotationCenter = snapshot.getRotationPoint().toDouble();
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
    }

    @Override
    public RotatedPosition getStartPosition(int xAxis, int yAxis, int zAxis)
    {
        return getGoalPos(0, xAxis, yAxis, zAxis);
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
    public RotatedPosition getFinalPosition(int xAxis, int yAxis, int zAxis)
    {
        return getGoalPos(Util.clampAngleRad(angle), xAxis, yAxis, zAxis);
    }

    @Override
    public void executeAnimationStep(IAnimator animator, Iterable<IAnimatedBlock> animatedBlocks, int ticks)
    {
        final double stepSum = Util.clampAngleRad(step * ticks);
        for (final IAnimatedBlock animatedBlock : animatedBlocks)
            animator.applyMovement(animatedBlock, getGoalPos(stepSum, animatedBlock));
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
    public @Nullable Consumer<IAnimatedBlockData> getBlockDataRotator()
    {
        return blockData -> blockData.rotateBlock(movementDirection, quarterCircles);
    }
}
