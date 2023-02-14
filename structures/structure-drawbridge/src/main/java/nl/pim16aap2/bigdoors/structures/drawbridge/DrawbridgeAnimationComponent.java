package nl.pim16aap2.bigdoors.structures.drawbridge;

import nl.pim16aap2.bigdoors.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.core.moveblocks.AnimationUtil;
import nl.pim16aap2.bigdoors.core.moveblocks.Animator;
import nl.pim16aap2.bigdoors.core.moveblocks.IAnimationComponent;
import nl.pim16aap2.bigdoors.core.moveblocks.IAnimator;
import nl.pim16aap2.bigdoors.core.moveblocks.StructureRequestData;
import nl.pim16aap2.bigdoors.core.structures.StructureSnapshot;
import nl.pim16aap2.bigdoors.core.util.MathUtil;
import nl.pim16aap2.bigdoors.core.util.MovementDirection;
import nl.pim16aap2.bigdoors.core.util.Util;
import nl.pim16aap2.bigdoors.core.util.functional.TriFunction;
import nl.pim16aap2.bigdoors.core.util.vector.IVector3D;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Dd;

/**
 * Represents a {@link Animator} for {@link Drawbridge}s.
 *
 * @author Pim
 */
public class DrawbridgeAnimationComponent implements IAnimationComponent
{
    private final Vector3Dd rotationCenter;
    protected final boolean northSouth;
    protected final TriFunction<Vector3Dd, Vector3Dd, Double, Vector3Dd> rotator;
    protected final StructureSnapshot snapshot;
    protected final int animationDuration;

    protected final double angle;
    private final int halfEndCount;
    private final double step;

    public DrawbridgeAnimationComponent(
        StructureRequestData data, MovementDirection movementDirection, boolean isNorthSouthAligned)
    {
        this.snapshot = data.getStructureSnapshot();

        northSouth = isNorthSouthAligned;
        rotationCenter = snapshot.getRotationPoint().toDouble().add(0.5, 0, 0.5);

        switch (movementDirection)
        {
            case NORTH ->
            {
                angle = -MathUtil.HALF_PI;
                rotator = Vector3Dd::rotateAroundXAxis;
            }
            case SOUTH ->
            {
                angle = MathUtil.HALF_PI;
                rotator = Vector3Dd::rotateAroundXAxis;
            }
            case EAST ->
            {
                angle = MathUtil.HALF_PI;
                rotator = Vector3Dd::rotateAroundZAxis;
            }
            case WEST ->
            {
                angle = -MathUtil.HALF_PI;
                rotator = Vector3Dd::rotateAroundZAxis;
            }
            default -> throw new IllegalArgumentException("Movement direction \"" + movementDirection.name() +
                                                              "\" is not valid for this type!");
        }

        animationDuration = AnimationUtil.getAnimationTicks(data.getAnimationTime(), data.getServerTickTime());
        step = angle / animationDuration;
        halfEndCount = animationDuration / 2;
    }

    protected Vector3Dd getGoalPos(double angle, double x, double y, double z)
    {
        return rotator.apply(new Vector3Dd(x, y, z), rotationCenter, angle);
    }

    protected Vector3Dd getGoalPos(double angle, IAnimatedBlock animatedBlock)
    {
        return getGoalPos(angle, animatedBlock.getStartX(), animatedBlock.getStartY(), animatedBlock.getStartZ());
    }

    @Override
    public Vector3Dd getFinalPosition(IVector3D startLocation, float radius)
    {
        return getGoalPos(angle, startLocation.xD(), startLocation.yD(), startLocation.zD());
    }

    @Override
    public void executeAnimationStep(IAnimator animator, int ticks, int ticksRemaining)
    {
        final double stepSum = step * ticks;
        final boolean replace = ticks == halfEndCount;

        if (replace)
            animator.respawnBlocks();

        for (final IAnimatedBlock animatedBlock : animator.getAnimatedBlocks())
            animator.applyMovement(animatedBlock, getGoalPos(stepSum, animatedBlock), ticksRemaining);
    }

    public static float getRadius(boolean northSouthAligned, IVector3D rotationPoint, int xAxis, int yAxis, int zAxis)
    {
        // Get the current radius of a block between used axis (either x and y, or z and y).
        // When the rotation point is positioned along the NS axis, the Z values does not change.
        final double deltaA = rotationPoint.yD() - yAxis;
        final double deltaB = northSouthAligned ? (rotationPoint.xD() - xAxis) : (rotationPoint.zD() - zAxis);
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
                              snapshot.getRotationPoint().x() - xAxis :
                              snapshot.getRotationPoint().z() - zAxis;
        final double deltaB = snapshot.getRotationPoint().y() - yAxis;
        return (float) Util.clampAngleRad(Math.atan2(deltaA, deltaB));
    }
}