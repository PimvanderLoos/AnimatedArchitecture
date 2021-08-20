package nl.pim16aap2.bigdoors.doors.windmill;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IHorizontalAxisAligned;
import nl.pim16aap2.bigdoors.doors.drawbridge.BridgeMover;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;

/**
 * Represents a {@link BlockMover} for {@link Windmill}s.
 *
 * @author Pim
 */
public class WindmillMover<T extends AbstractDoor & IHorizontalAxisAligned> extends BridgeMover<T>
{
    protected static final double EPS = 2 * Double.MIN_VALUE;

    private double step;

    public WindmillMover(T door, double time, double multiplier, RotateDirection rotateDirection, IPPlayer player,
                         DoorActionCause cause, DoorActionType actionType)
        throws Exception
    {
        super(time, door, rotateDirection, false, multiplier, player, door.getCuboid(), cause, actionType);
    }

    @Override
    protected void init()
    {
        super.endCount = (int) (20 * 20 * super.time);
        step = (Math.PI / 2.0) / (20.0f * super.time * 2.0f);
    }

    @Override
    protected IPLocation getNewLocation(double radius, double xAxis, double yAxis, double zAxis)
    {
        return locationFactory.create(world, xAxis, yAxis, zAxis);
    }

    @Override
    protected Vector3Dd getFinalPosition(PBlockData block)
    {
        return block.getStartPosition();
    }

    @Override
    protected void executeAnimationStep(int ticks)
    {
        final double stepSum = step * ticks;
        for (PBlockData block : savedBlocks)
            block.getFBlock().teleport(getGoalPos(stepSum, block));
    }

    @Override
    protected float getRadius(int xAxis, int yAxis, int zAxis)
    {
        // Get the current radius of a block between used axis (either x and y, or z and y).
        // When the engine is positioned along the NS axis, the X values does not change for this type.
        final double deltaA = (double) door.getEngine().y() - yAxis;
        final double deltaB = !NS ? (door.getEngine().x() - xAxis) : (door.getEngine().z() - zAxis);
        return (float) Math.sqrt(Math.pow(deltaA, 2) + Math.pow(deltaB, 2));
    }

    @Override
    protected float getStartAngle(int xAxis, int yAxis, int zAxis)
    {
        // Get the angle between the used axes (either x and y, or z and y).
        // When the engine is positioned along the NS axis, the X values does not change for this type.
        final double deltaA = !NS ? door.getEngine().x() - xAxis : door.getEngine().z() - zAxis;
        final double deltaB = (double) door.getEngine().y() - yAxis;

        return (float) Util.clampAngleRad(Math.atan2(deltaA, deltaB));
    }
}
