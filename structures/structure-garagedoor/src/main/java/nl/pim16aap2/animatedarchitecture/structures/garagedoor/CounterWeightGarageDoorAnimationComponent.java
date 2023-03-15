package nl.pim16aap2.animatedarchitecture.structures.garagedoor;

import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.AnimationUtil;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.Animator;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.IAnimator;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;
import nl.pim16aap2.animatedarchitecture.core.util.BlockFace;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.functional.TriFunction;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;

/**
 * Represents a {@link Animator} for {@link GarageDoor}s.
 *
 * @author Pim
 */
public class CounterWeightGarageDoorAnimationComponent implements IAnimationComponent
{
    protected final StructureSnapshot snapshot;
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
            default -> throw new IllegalStateException("Failed to open garage door \"" + snapshot.getUid()
                                                           + "\". Reason: Invalid movement direction \"" +
                                                           movementDirection + "\"");
        }
        directionVec = BlockFace.getDirection(this.animationDirectionFace);

        this.mergedCuboid = getMergedCuboid(oldCuboid, directionVec, wasVertical);
        this.mergedCuboidRadius = mergedCuboid.getDimensions().multiply(directionVec.absolute()).getMax() / 2.0D;
        this.newCuboid = data.getNewCuboid();

        this.rotationCenter = mergedCuboid.getCenter().floor().add(0.5, 0, 0.5);
        this.step =
            angle / quarterCircles / AnimationUtil.getAnimationTicks(data.getAnimationTime(), data.getServerTickTime());
    }

    @Override
    public Vector3Dd getRotationPoint()
    {
        return rotationCenter;
    }

    private Cuboid getMergedCuboid(Cuboid oldCuboid, Vector3Di directionVec, boolean wasVertical)
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
            final int height = oldCuboid.getDimensions().multiply(directionVec.absolute()).getMax();
            return oldCuboid.grow(1, 0, 1).add(0, -height - 1, 0);
        }
    }

    protected Vector3Dd getGoalPos(double angle, IAnimatedBlock animatedBlock)
    {
        return rotator.apply(animatedBlock.getStartPosition().position(), rotationCenter, angle);
    }

    @Override
    public void executeAnimationStep(IAnimator animator, int ticks, int ticksRemaining)
    {
        final double stepSum = Util.clampAngleRad(step * ticks);

        for (final IAnimatedBlock animatedBlock : animator.getAnimatedBlocks())
            animator.applyMovement(animatedBlock, getGoalPos(stepSum, animatedBlock), ticksRemaining);
    }

    @Override
    public Vector3Dd getFinalPosition(IVector3D startLocation, float radius)
    {
        return rotator.apply(Vector3Dd.of(startLocation), rotationCenter, angle);
    }

    @Override
    public float getRadius(int xAxis, int yAxis, int zAxis)
    {
        if (wasVertical)
        {
            final float height = oldCuboid.getMax().y();
            return 1 + height - yAxis;
        }

        final int dX = Math.abs(xAxis - snapshot.getRotationPoint().x());
        final int dZ = Math.abs(zAxis - snapshot.getRotationPoint().z());
        return Math.abs(dX * directionVec.x() + dZ * directionVec.z());
    }
}
