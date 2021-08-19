package nl.pim16aap2.bigdoors.doors.revolvingdoor;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.api.PSound;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.PSoundDescription;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;

import java.util.function.BiFunction;

/**
 * Represents a {@link BlockMover} for {@link RevolvingDoor}s.
 *
 * @author Pim
 */
public class RevolvingDoorMover extends BlockMover
{
    private final BiFunction<PBlockData, Double, Vector3Dd> getGoalPos;
    private final RotateDirection rotateDirection;

    /**
     * The number of quarter circles to turn.
     */
    private final int quarterCircles;

    private double step = 0;
    private double endStepSum = 0;

    public RevolvingDoorMover(final AbstractDoor door, final double time, final double multiplier,
                              final RotateDirection rotateDirection, final IPPlayer player,
                              final int quarterCircles, final DoorActionCause cause,
                              final Cuboid newCuboid, final DoorActionType actionType)
        throws Exception
    {
        super(door, 30, false, RotateDirection.NONE, player, newCuboid, cause, actionType);

        this.quarterCircles = quarterCircles;

        this.time = time;
        this.rotateDirection = rotateDirection;

        switch (rotateDirection)
        {
            case CLOCKWISE:
                getGoalPos = this::getGoalPosClockwise;
                break;
            case COUNTERCLOCKWISE:
                getGoalPos = this::getGoalPosCounterClockwise;
                break;
            default:
                throw new IllegalStateException("Failed to open door \"" + getDoorUID()
                                                    + "\". Reason: Invalid rotateDirection \"" +
                                                    rotateDirection + "\"");
        }

        init();
        super.startAnimation();
    }

    /**
     * Used for initializing variables such as {@link #endCount} and {@link #soundActive}.
     */
    protected void init()
    {
        super.endCount = (int) (20.0 * super.time * ((double) quarterCircles));
        step = (Math.PI / 2.0 * ((double) quarterCircles)) / ((double) super.endCount) * -1.0;
        endStepSum = super.endCount * step;
        super.soundActive = new PSoundDescription(PSound.DRAGGING, 0.8f, 0.7f);
        super.soundFinish = new PSoundDescription(PSound.THUD, 0.2f, 0.15f);
    }

    private Vector3Dd getGoalPosClockwise(final double radius, final double startAngle, final double startY,
                                          final double stepSum)
    {
        final double posX = 0.5 + door.getEngine().x() - radius * Math.sin(startAngle + stepSum);
        final double posZ = 0.5 + door.getEngine().z() - radius * Math.cos(startAngle + stepSum);
        return new Vector3Dd(posX, startY, posZ);
    }

    private Vector3Dd getGoalPosClockwise(final PBlockData block, final double stepSum)
    {
        return getGoalPosClockwise(block.getRadius(), block.getStartAngle(), block.getStartY(), stepSum);
    }

    private Vector3Dd getGoalPosCounterClockwise(final double radius, final double startAngle,
                                                 final double startY,
                                                 final double stepSum)
    {
        final double posX = 0.5 + door.getEngine().x() - radius * Math.sin(startAngle - stepSum);
        final double posZ = 0.5 + door.getEngine().z() - radius * Math.cos(startAngle - stepSum);
        return new Vector3Dd(posX, startY, posZ);
    }

    private Vector3Dd getGoalPosCounterClockwise(final PBlockData block, final double stepSum)
    {
        return getGoalPosCounterClockwise(block.getRadius(), block.getStartAngle(), block.getStartY(), stepSum);
    }

    @Override
    protected IPLocation getNewLocation(final double radius, final double xAxis, final double yAxis,
                                        final double zAxis)
    {
        // TODO: Redo all this, it's too hacky.
        final double startAngle = getStartAngle((int) xAxis, (int) yAxis, (int) zAxis);
        Vector3Dd newPos;
        if (rotateDirection == RotateDirection.CLOCKWISE)
            newPos = getGoalPosClockwise(radius, startAngle, yAxis, endStepSum);
        else
            newPos = getGoalPosCounterClockwise(radius, startAngle, yAxis, endStepSum);
        return locationFactory.create(world, newPos.x(), newPos.y(), newPos.z());
    }

    @Override
    protected Vector3Dd getFinalPosition(final PBlockData block)
    {
        final Vector3Dd startLocation = block.getStartPosition();
        final IPLocation finalLoc = getNewLocation(block.getRadius(), startLocation.x(),
                                                   startLocation.y(), startLocation.z());
        return new Vector3Dd(finalLoc.getBlockX() + 0.5, finalLoc.getBlockY(), finalLoc.getBlockZ() + 0.5);
    }

    @Override
    protected void executeAnimationStep(int ticks)
    {
        final double stepSum = step * ticks;

        for (final PBlockData block : savedBlocks)
            block.getFBlock().teleport(getGoalPos.apply(block, stepSum));
    }

    @Override
    protected float getRadius(final int xAxis, final int yAxis, final int zAxis)
    {
        final double deltaA = (double) door.getEngine().x() - xAxis;
        final double deltaB = (double) door.getEngine().z() - zAxis;
        return (float) Math.sqrt(Math.pow(deltaA, 2) + Math.pow(deltaB, 2));
    }

    @Override
    protected float getStartAngle(final int xAxis, final int yAxis, final int zAxis)
    {
        return (float) Math.atan2((double) door.getEngine().x() - xAxis, (double) door.getEngine().z() - zAxis);
    }
}
