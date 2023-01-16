package nl.pim16aap2.bigdoors.doors.windmill;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorSnapshot;
import nl.pim16aap2.bigdoors.doors.doorarchetypes.IHorizontalAxisAligned;
import nl.pim16aap2.bigdoors.doors.drawbridge.BridgeMover;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.MathUtil;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.IVector3D;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;

/**
 * Represents a {@link BlockMover} for {@link Windmill}s.
 *
 * @author Pim
 */
public class WindmillMover<T extends AbstractDoor & IHorizontalAxisAligned> extends BridgeMover<T>
{
    protected static final double EPS = 2 * Double.MIN_VALUE;

    private final double step;

    public WindmillMover(
        Context context, T door, DoorSnapshot doorSnapshot, double time, double multiplier,
        RotateDirection rotateDirection, IPPlayer player, DoorActionCause cause, DoorActionType actionType)
        throws Exception
    {
        super(context, door, doorSnapshot, time, rotateDirection, false, multiplier, player, door.getCuboid(), cause,
              actionType);
        super.perpetualMovement = true;

        step = MathUtil.HALF_PI / (20.0f * super.time * 2.0f);
    }

    @Override
    protected Vector3Dd getFinalPosition(IVector3D startLocation, float radius)
    {
        return Vector3Dd.of(startLocation);
    }

    @Override
    protected void executeAnimationStep(int ticks, int ticksRemaining)
    {
        final double stepSum = step * ticks;
        for (final IAnimatedBlock animatedBlock : getAnimatedBlocks())
            applyMovement(animatedBlock, getGoalPos(stepSum, animatedBlock), ticksRemaining);
    }

    @Override
    protected float getRadius(int xAxis, int yAxis, int zAxis)
    {
        // Get the current radius of a block between used axis (either x and y, or z and y).
        // When the rotation point is positioned along the NS axis, the X values does not change for this type.
        final double deltaA = doorSnapshot.getRotationPoint().yD() - yAxis;
        final double deltaB =
            northSouth ? (doorSnapshot.getRotationPoint().z() - zAxis) : (doorSnapshot.getRotationPoint().x() - xAxis);
        return (float) Math.sqrt(Math.pow(deltaA, 2) + Math.pow(deltaB, 2));
    }

    @Override
    protected float getStartAngle(int xAxis, int yAxis, int zAxis)
    {
        // Get the angle between the used axes (either x and y, or z and y).
        // When the rotation point is positioned along the NS axis, the X values does not change for this type.
        final double deltaA =
            northSouth ? doorSnapshot.getRotationPoint().z() - zAxis : doorSnapshot.getRotationPoint().x() - xAxis;
        final double deltaB = doorSnapshot.getRotationPoint().yD() - yAxis;

        return (float) Util.clampAngleRad(Math.atan2(deltaA, deltaB));
    }
}
