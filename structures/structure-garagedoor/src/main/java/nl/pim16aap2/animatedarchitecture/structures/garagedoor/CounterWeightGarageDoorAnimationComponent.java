package nl.pim16aap2.animatedarchitecture.structures.garagedoor;

import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationUtil;
import nl.pim16aap2.animatedarchitecture.core.animation.Animator;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimator;
import nl.pim16aap2.animatedarchitecture.core.animation.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.util.BlockFace;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.functional.TriFunction;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;

/**
 * Represents a {@link Animator} for {@link GarageDoor}s.
 */
public class CounterWeightGarageDoorAnimationComponent implements IAnimationComponent
{
    protected final StructureSnapshot snapshot;
    protected final Vector3Di rotationPoint;
    protected final boolean northSouth;
    protected final Cuboid oldCuboid;
    protected final Cuboid mergedCuboid;
    protected final double mergedCuboidRadius;
    protected final Cuboid newCuboid;
    protected final IPlayer player;
    protected final TriFunction<Vector3Dd, IVector3D, Double, Vector3Dd> rotator;
    protected final BlockFace animationDirectionFace;
    protected final Vector3Di directionVec;
    protected final Vector3Dd rotationCenter;
    protected final boolean wasVertical;
    protected final double angle;

    private final double step;

    public CounterWeightGarageDoorAnimationComponent(AnimationRequestData data, MovementDirection movementDirection)
    {
        this.player = data.getResponsible();
        this.snapshot = data.getStructureSnapshot();
        this.oldCuboid = snapshot.getCuboid();
        this.wasVertical = this.oldCuboid.getDimensions().y() > 1;
        this.northSouth = movementDirection == MovementDirection.NORTH || movementDirection == MovementDirection.SOUTH;
        this.rotationPoint = this.snapshot.getRequiredPropertyValue(Property.ROTATION_POINT);

        final int quarterCircles = 1; // Placeholder
        switch (movementDirection)
        {
            case NORTH ->
            {
                angle = quarterCircles * -MathUtil.HALF_PI;
                rotator = Vector3Dd::rotateAroundXAxis;
                animationDirectionFace = BlockFace.NORTH;
            }
            case EAST ->
            {
                angle = quarterCircles * MathUtil.HALF_PI;
                rotator = Vector3Dd::rotateAroundZAxis;
                animationDirectionFace = BlockFace.EAST;
            }
            case SOUTH ->
            {
                angle = quarterCircles * MathUtil.HALF_PI;
                rotator = Vector3Dd::rotateAroundXAxis;
                animationDirectionFace = BlockFace.SOUTH;
            }
            case WEST ->
            {
                angle = quarterCircles * -MathUtil.HALF_PI;
                rotator = Vector3Dd::rotateAroundZAxis;
                animationDirectionFace = BlockFace.WEST;
            }
            default -> throw new IllegalStateException(String.format(
                "Failed to open garage door '%d'. Reason: Invalid movement direction '%s'",
                snapshot.getUid(),
                movementDirection
            ));
        }
        directionVec = BlockFace.getDirection(this.animationDirectionFace);

        this.mergedCuboid = getMergedCuboid(oldCuboid, directionVec, wasVertical);
        this.mergedCuboidRadius = mergedCuboid.getDimensions().multiply(directionVec.absolute()).max() / 2.0D;
        this.newCuboid = data.getNewCuboid();

        this.rotationCenter = mergedCuboid.getCenter();
        final int animationSteps = AnimationUtil.getAnimationTicks(data.getAnimationTime(), data.getServerTickTime());
        this.step = angle / quarterCircles / animationSteps;
    }

    private static Cuboid getMergedCuboid(Cuboid oldCuboid, Vector3Di directionVec, boolean wasVertical)
    {
        if (wasVertical)
        {
            final int height = oldCuboid.getDimensions().y();
            final Vector3Di add = directionVec.multiply(height + 1, 0, height + 1);
            final int growX = directionVec.x() == 0 ? 1 : 0;
            final int growZ = directionVec.z() == 0 ? 1 : 0;
            return oldCuboid.grow(growX, 1, growZ).add(add.x(), add.y(), add.z());
        }
        else
        {
            final int height = oldCuboid.getDimensions().multiply(directionVec.absolute()).max();
            return oldCuboid.grow(1, 0, 1).add(0, -height - 1, 0);
        }
    }

    protected RotatedPosition getGoalPos(double angle, IAnimatedBlock animatedBlock)
    {
        return new RotatedPosition(rotator.apply(animatedBlock.getStartPosition().position(), rotationCenter, angle));
    }

    @Override
    public void executeAnimationStep(IAnimator animator, Iterable<IAnimatedBlock> animatedBlocks, int ticks)
    {
        final double stepSum = MathUtil.clampAngleRad(step * ticks);

        for (final IAnimatedBlock animatedBlock : animatedBlocks)
            animator.applyMovement(animatedBlock, getGoalPos(stepSum, animatedBlock));
    }

    @Override
    public RotatedPosition getFinalPosition(int xAxis, int yAxis, int zAxis)
    {
        return new RotatedPosition(rotator.apply(new Vector3Dd(xAxis, yAxis, zAxis), rotationCenter, angle));
    }

    @Override
    public float getRadius(int xAxis, int yAxis, int zAxis)
    {
        if (wasVertical)
        {
            final float height = oldCuboid.getMax().y();
            return 1 + height - yAxis;
        }

        final int dX = Math.abs(xAxis - rotationPoint.x());
        final int dZ = Math.abs(zAxis - rotationPoint.z());
        return Math.abs(dX * directionVec.x() + dZ * directionVec.z());
    }
}
