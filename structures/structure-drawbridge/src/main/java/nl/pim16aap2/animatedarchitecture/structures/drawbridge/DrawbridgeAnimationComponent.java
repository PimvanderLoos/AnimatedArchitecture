package nl.pim16aap2.animatedarchitecture.structures.drawbridge;

import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationUtil;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimator;
import nl.pim16aap2.animatedarchitecture.core.animation.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockData;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.functional.TriFunction;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Represents an {@link IAnimationComponent} for {@link Drawbridge} structure types.
 */
@ToString
public class DrawbridgeAnimationComponent implements IAnimationComponent
{
    private final Vector3Dd rotationCenter;
    private final Vector3Di rotationPoint;
    private final boolean northSouth;
    private final TriFunction<Vector3Dd, Vector3Dd, Double, Vector3Dd> rotator;
    private final double resultAngle;
    private final double step;
    private final MovementDirection movementDirection;
    private final int effectiveQuarterCircles;

    public DrawbridgeAnimationComponent(
        AnimationRequestData data,
        MovementDirection movementDirection,
        boolean isNorthSouthAligned,
        int quarterCircles)
    {
        final var snapshot = data.getStructureSnapshot();

        this.northSouth = isNorthSouthAligned;
        this.rotationPoint = snapshot.getRequiredPropertyValue(Property.ROTATION_POINT);
        this.rotationCenter = rotationPoint.toDouble();
        this.movementDirection = movementDirection;
        this.effectiveQuarterCircles = quarterCircles % 4;

        final double quarterCircleAngle;
        switch (movementDirection)
        {
            case NORTH ->
            {
                quarterCircleAngle = -MathUtil.HALF_PI;
                rotator = Vector3Dd::rotateAroundXAxis;
            }
            case SOUTH ->
            {
                quarterCircleAngle = MathUtil.HALF_PI;
                rotator = Vector3Dd::rotateAroundXAxis;
            }
            case EAST ->
            {
                quarterCircleAngle = MathUtil.HALF_PI;
                rotator = Vector3Dd::rotateAroundZAxis;
            }
            case WEST ->
            {
                quarterCircleAngle = -MathUtil.HALF_PI;
                rotator = Vector3Dd::rotateAroundZAxis;
            }
            default -> throw new IllegalArgumentException(
                "Movement direction '" + movementDirection.name() + "' is not valid for this type!");
        }

        this.resultAngle = effectiveQuarterCircles * quarterCircleAngle;

        final int animationStepCount =
            AnimationUtil.getAnimationTicks(data.getAnimationTime(), data.getServerTickTime());

        this.step = quarterCircles * quarterCircleAngle / animationStepCount;
    }

    @Override
    public RotatedPosition getStartPosition(int xAxis, int yAxis, int zAxis)
    {
        return getGoalPos(null, 0, xAxis, yAxis, zAxis);
    }

    protected Vector3Dd getGoalRotation(double angle)
    {
        double pitch = 0;
        double roll = 0;
        if (northSouth)
            pitch = Math.toDegrees(angle + MathUtil.HALF_PI);
        else
            roll = -Math.toDegrees(angle + MathUtil.HALF_PI);

        return new Vector3Dd(roll, pitch, 0);
    }

    protected RotatedPosition getGoalPos(@Nullable Vector3Dd localRotation, double angle, double x, double y, double z)
    {
        final Vector3Dd goalPos = rotator.apply(new Vector3Dd(x, y, z), rotationCenter, angle);
        final Vector3Dd goalRot = localRotation != null ? localRotation : getGoalRotation(angle);
        return new RotatedPosition(goalPos, goalRot);
    }

    protected RotatedPosition getGoalPos(@Nullable Vector3Dd localRotation, double angle, IAnimatedBlock animatedBlock)
    {
        return getGoalPos(
            localRotation,
            angle,
            animatedBlock.getStartX(),
            animatedBlock.getStartY(),
            animatedBlock.getStartZ()
        );
    }

    @Override
    public RotatedPosition getFinalPosition(int xAxis, int yAxis, int zAxis)
    {
        return getGoalPos(null, MathUtil.clampAngleRad(resultAngle), xAxis, yAxis, zAxis);
    }

    @Override
    public void executeAnimationStep(IAnimator animator, Iterable<IAnimatedBlock> animatedBlocks, int ticks)
    {
        final double angle = MathUtil.clampAngleRad(step * ticks);
        final Vector3Dd localRotation = getGoalRotation(angle);

        for (final IAnimatedBlock animatedBlock : animatedBlocks)
            animator.applyMovement(animatedBlock, getGoalPos(localRotation, angle, animatedBlock));
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
        return getRadius(northSouth, rotationPoint, xAxis, yAxis, zAxis);
    }

    @Override
    public @Nullable Consumer<IAnimatedBlockData> getBlockDataRotator()
    {
        if (effectiveQuarterCircles == 0)
            return null;
        return blockData -> blockData.rotateBlock(movementDirection, effectiveQuarterCircles);
    }
}
