package nl.pim16aap2.bigdoors.structures.garagedoor;

import nl.pim16aap2.bigdoors.core.api.Color;
import nl.pim16aap2.bigdoors.core.api.GlowingBlockSpawner;
import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.core.moveblocks.AnimationRequestData;
import nl.pim16aap2.bigdoors.core.moveblocks.AnimationUtil;
import nl.pim16aap2.bigdoors.core.moveblocks.Animator;
import nl.pim16aap2.bigdoors.core.moveblocks.IAnimationComponent;
import nl.pim16aap2.bigdoors.core.moveblocks.IAnimator;
import nl.pim16aap2.bigdoors.core.structures.StructureSnapshot;
import nl.pim16aap2.bigdoors.core.util.BlockFace;
import nl.pim16aap2.bigdoors.core.util.Cuboid;
import nl.pim16aap2.bigdoors.core.util.MathUtil;
import nl.pim16aap2.bigdoors.core.util.MovementDirection;
import nl.pim16aap2.bigdoors.core.util.Util;
import nl.pim16aap2.bigdoors.core.util.functional.TriFunction;
import nl.pim16aap2.bigdoors.core.util.vector.IVector3D;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Di;

import java.time.Duration;
import java.util.function.BiFunction;

/**
 * Represents a {@link Animator} for {@link GarageDoor}s.
 *
 * @author Pim
 */
public final class GarageDoorAnimationComponent implements IAnimationComponent
{
    private final StructureSnapshot snapshot;
    private final TriFunction<Vector3Dd, Vector3Dd, Double, Vector3Dd> rotator;
    private final double resultHeight;
    private final Vector3Di directionVec;
    private final BiFunction<IAnimatedBlock, Double, Vector3Dd> getVector;
    private final Vector3Dd rotationCenter;
    private final boolean northSouth;
    private final double step;
    private final boolean wasVertical;
    private final Cuboid oldCuboid;
    private final double angle;
    private final double radiusToRotationCenter;
    private final double radiusMultiplier = 1.0;
    private final GlowingBlockSpawner glowingBlockBuilder;
    private final Cuboid mergedCuboid;
    private final IPlayer player;

    public GarageDoorAnimationComponent(AnimationRequestData data, MovementDirection movementDirection)
    {
        this.player = data.getResponsible();
        this.glowingBlockBuilder = data.getGlowingBlockSpawner();
        this.snapshot = data.getStructureSnapshot();
        this.oldCuboid = snapshot.getCuboid();
        this.wasVertical = this.oldCuboid.getDimensions().y() > 1;
        this.northSouth = movementDirection == MovementDirection.NORTH || movementDirection == MovementDirection.SOUTH;

        resultHeight = oldCuboid.getMax().y() + 1.0D;
        final int quarterCircles = 1;

        final BiFunction<IAnimatedBlock, Double, Vector3Dd> getVectorTmp;
        switch (movementDirection)
        {
            case NORTH ->
            {
                angle = quarterCircles * -MathUtil.HALF_PI;
                rotator = Vector3Dd::rotateAroundXAxis;
                directionVec = BlockFace.getDirection(BlockFace.NORTH);
                getVectorTmp = this::getVectorDownNorth;
            }
            case EAST ->
            {
                angle = quarterCircles * MathUtil.HALF_PI;
                rotator = Vector3Dd::rotateAroundZAxis;
                directionVec = BlockFace.getDirection(BlockFace.EAST);
                getVectorTmp = this::getVectorDownEast;
            }
            case SOUTH ->
            {
                angle = quarterCircles * MathUtil.HALF_PI;
                rotator = Vector3Dd::rotateAroundXAxis;
                directionVec = BlockFace.getDirection(BlockFace.SOUTH);
                getVectorTmp = this::getVectorDownSouth;
            }
            case WEST ->
            {
                angle = quarterCircles * -MathUtil.HALF_PI;
                rotator = Vector3Dd::rotateAroundZAxis;
                directionVec = BlockFace.getDirection(BlockFace.WEST);
                getVectorTmp = this::getVectorDownWest;
            }
            default -> throw new IllegalStateException("Failed to open garage door \"" + snapshot.getUid()
                                                           + "\". Reason: Invalid movement direction \"" +
                                                           movementDirection + "\"");
        }

        mergedCuboid = Cuboid.of(oldCuboid, data.getNewCuboid());
        this.radiusToRotationCenter =
            (northSouth ? mergedCuboid.getDimensions().z() : mergedCuboid.getDimensions().x()) / 2.0D;

        this.rotationCenter = mergedCuboid.getCenter().add(0, -0.5, 0.5);
        this.step = angle / AnimationUtil.getAnimationTicks(data.getAnimationTime(), data.getServerTickTime());


        final Vector3Di dims = oldCuboid.getDimensions();
        final int blocksToMove;
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


    private void debug()
    {
        for (final var pt : this.mergedCuboid.getCorners())
        {
            glowingBlockBuilder.builder()
                               .atPosition(new Vector3Dd(pt).add(0.5, 0, 0.5))
                               .forDuration(Duration.ofSeconds(1))
                               .forPlayer(player)
                               .inWorld(snapshot.getWorld())
                               .withColor(Color.AQUA)
                               .build();
        }
    }

    private Vector3Dd getGoalPos(double angle, double x, double y, double z)
    {
        return rotator.apply(new Vector3Dd(x, y, z), rotationCenter, angle);
    }

    private Vector3Dd getGoalPos(double angle, IAnimatedBlock animatedBlock)
    {
        return getGoalPos(angle, animatedBlock.getStartX(), animatedBlock.getStartY(), animatedBlock.getStartZ());
    }

    @Override
    public void executeAnimationStep(IAnimator animator, int ticks, int ticksRemaining)
    {
//        debug();
        final double stepSum = Util.clampAngleRad(step * ticks);

        for (final IAnimatedBlock animatedBlock : animator.getAnimatedBlocks())
            animator.applyMovement(animatedBlock, getGoalPos(stepSum, animatedBlock), ticksRemaining);
    }

//    @Override
//    public Vector3Dd getFinalPosition(IVector3D startLocation, float radius)
//    {
//        return getGoalPos(Util.clampAngleRad(angle), startLocation.xD(), startLocation.yD(), startLocation.zD());
//    }
//
//    @Override
//    public float getRadius(int xAxis, int yAxis, int zAxis)
//    {
//        return (float) radiusToRotationCenter;
//    }

    @Override
    public Vector3Dd getFinalPosition(IVector3D startLocation, float radius)
    {
        double newX;
        double newY;
        double newZ;

        final Vector3Di dims = oldCuboid.getDimensions();

        if (wasVertical)
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
            newY -= 1;

            newX += northSouth ? 0 : 0.5f;
            newZ += northSouth ? 0.5f : 0;
        }
        return new Vector3Dd(newX, newY, newZ);
    }

//    @Override
//    public void executeAnimationStep(IAnimator animator, int ticks, int ticksRemaining)
//    {
//        final double stepSum = step * ticks;
//
//        for (final IAnimatedBlock animatedBlock : animator.getAnimatedBlocks())
//            animator.applyMovement(animatedBlock, getVector.apply(animatedBlock, stepSum), ticksRemaining);
//    }

    @Override
    public float getRadius(int xAxis, int yAxis, int zAxis)
    {
        if (wasVertical)
        {
            final float height = oldCuboid.getMax().y();
            return height - yAxis;
        }

        final int dX = Math.abs(xAxis - snapshot.getRotationPoint().x());
        final int dZ = Math.abs(zAxis - snapshot.getRotationPoint().z());
        return Math.abs(dX * directionVec.x() + dZ * directionVec.z());
    }
}
