package nl.pim16aap2.bigdoors.movable.drawbridge;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionCause;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionType;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableBase;
import nl.pim16aap2.bigdoors.movable.MovableSnapshot;
import nl.pim16aap2.bigdoors.movable.movablearchetypes.IHorizontalAxisAligned;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.MathUtil;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.functional.TriFunction;
import nl.pim16aap2.bigdoors.util.vector.IVector3D;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;

/**
 * Represents a {@link BlockMover} for {@link Drawbridge}s.
 *
 * @author Pim
 */
public class BridgeMover<T extends AbstractMovable & IHorizontalAxisAligned> extends BlockMover
{
    private final Vector3Dd rotationCenter;
    protected final boolean northSouth;
    protected final TriFunction<Vector3Dd, Vector3Dd, Double, Vector3Dd> rotator;

    private final int halfEndCount;
    private final double step;
    protected final double angle;

    /**
     * Constructs a {@link BlockMover}.
     *
     * @param movable
     *     The {@link MovableBase}.
     * @param time
     *     The amount of time (in seconds) the movable will try to toggle itself in.
     * @param skipAnimation
     *     If the movable should be opened instantly (i.e. skip animation) or not.
     * @param rotateDirection
     *     The direction the {@link MovableBase} will move.
     * @param multiplier
     *     The speed multiplier.
     * @param player
     *     The player who opened this movable.
     */
    public BridgeMover(
        Context context, T movable, MovableSnapshot snapshot, double time, RotateDirection rotateDirection,
        boolean skipAnimation, double multiplier, IPPlayer player, Cuboid newCuboid, MovableActionCause cause,
        MovableActionType actionType)
        throws Exception
    {
        super(context, movable, snapshot, time, skipAnimation, rotateDirection, player, newCuboid, cause,
              actionType);

        northSouth = movable.isNorthSouthAligned();
        rotationCenter = snapshot.getRotationPoint().toDouble().add(0.5, 0, 0.5);

        switch (rotateDirection)
        {
            case NORTH:
                angle = -MathUtil.HALF_PI;
                rotator = Vector3Dd::rotateAroundXAxis;
                break;
            case SOUTH:
                angle = MathUtil.HALF_PI;
                rotator = Vector3Dd::rotateAroundXAxis;
                break;
            case EAST:
                angle = MathUtil.HALF_PI;
                rotator = Vector3Dd::rotateAroundZAxis;
                break;
            case WEST:
                angle = -MathUtil.HALF_PI;
                rotator = Vector3Dd::rotateAroundZAxis;
                break;
            default:
                throw new IllegalArgumentException("RotateDirection \"" + rotateDirection.name() +
                                                       " is not valid for this type!");
        }

        step = angle / super.animationDuration;
        halfEndCount = super.animationDuration / 2;
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
    protected Vector3Dd getFinalPosition(IVector3D startLocation, float radius)
    {
        return getGoalPos(angle, startLocation.xD(), startLocation.yD(), startLocation.zD());
    }

    @Override
    protected void executeAnimationStep(int ticks, int ticksRemaining)
    {
        final double stepSum = step * ticks;
        final boolean replace = ticks == halfEndCount;

        if (replace)
            this.respawnBlocks();

        for (final IAnimatedBlock animatedBlock : getAnimatedBlocks())
            applyMovement(animatedBlock, getGoalPos(stepSum, animatedBlock), ticksRemaining);
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
    protected float getRadius(int xAxis, int yAxis, int zAxis)
    {
        return getRadius(northSouth, snapshot.getRotationPoint(), xAxis, yAxis, zAxis);
    }

    @Override
    protected float getStartAngle(int xAxis, int yAxis, int zAxis)
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
