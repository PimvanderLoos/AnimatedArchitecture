package nl.pim16aap2.bigdoors.doors.revolvingdoor;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PSound;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
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
    private final BiFunction<IAnimatedBlock, Double, Vector3Dd> getGoalPos;
    private final RotateDirection rotateDirection;

    /**
     * The number of quarter circles to turn.
     */
    private final int quarterCircles;

    private double step = 0;
    private double endStepSum = 0;

    @SuppressWarnings("unused")
    public RevolvingDoorMover(
        Context context, AbstractDoor door, double time, double multiplier, RotateDirection rotateDirection,
        IPPlayer player, int quarterCircles, DoorActionCause cause, Cuboid newCuboid, DoorActionType actionType)
        throws Exception
    {
        super(context, door, 30, false, RotateDirection.NONE, player, newCuboid, cause, actionType);

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
        super.endCount = (int) (20.0 * super.time * quarterCircles);
        step = (Math.PI / 2.0 * quarterCircles) / super.endCount * -1.0;
        endStepSum = super.endCount * step;
        super.soundActive = new PSoundDescription(PSound.DRAGGING, 0.8f, 0.7f);
        super.soundFinish = new PSoundDescription(PSound.THUD, 0.2f, 0.15f);
    }

    private Vector3Dd getGoalPosClockwise(double radius, double startAngle, double startY, double stepSum)
    {
        final double posX = 0.5 + door.getRotationPoint().x() - radius * Math.sin(startAngle + stepSum);
        final double posZ = 0.5 + door.getRotationPoint().z() - radius * Math.cos(startAngle + stepSum);
        return new Vector3Dd(posX, startY, posZ);
    }

    private Vector3Dd getGoalPosClockwise(IAnimatedBlock animatedBlock, double stepSum)
    {
        return getGoalPosClockwise(animatedBlock.getRadius(), animatedBlock.getStartAngle(),
                                   animatedBlock.getStartY(),
                                   stepSum);
    }

    private Vector3Dd getGoalPosCounterClockwise(double radius, double startAngle, double startY, double stepSum)
    {
        final double posX = 0.5 + door.getRotationPoint().x() - radius * Math.sin(startAngle - stepSum);
        final double posZ = 0.5 + door.getRotationPoint().z() - radius * Math.cos(startAngle - stepSum);
        return new Vector3Dd(posX, startY, posZ);
    }

    private Vector3Dd getGoalPosCounterClockwise(IAnimatedBlock animatedBlock, double stepSum)
    {
        return getGoalPosCounterClockwise(animatedBlock.getRadius(), animatedBlock.getStartAngle(),
                                          animatedBlock.getStartY(), stepSum);
    }

    @Override
    protected IPLocation getNewLocation(double radius, double xAxis, double yAxis, double zAxis)
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
    protected Vector3Dd getFinalPosition(IAnimatedBlock animatedBlock)
    {
        final Vector3Dd startLocation = animatedBlock.getStartPosition();
        final IPLocation finalLoc = getNewLocation(animatedBlock.getRadius(), startLocation.x(),
                                                   startLocation.y(), startLocation.z());
        return new Vector3Dd(finalLoc.getBlockX() + 0.5, finalLoc.getBlockY(), finalLoc.getBlockZ() + 0.5);
    }

    @Override
    protected void executeAnimationStep(int ticks)
    {
        final double stepSum = step * ticks;

        for (final IAnimatedBlock animatedBlock : animatedBlocks)
            movementMethod.apply(animatedBlock, getGoalPos.apply(animatedBlock, stepSum));
    }

    @Override
    protected float getRadius(int xAxis, int yAxis, int zAxis)
    {
        final double deltaA = (double) door.getRotationPoint().x() - xAxis;
        final double deltaB = (double) door.getRotationPoint().z() - zAxis;
        return (float) Math.sqrt(Math.pow(deltaA, 2) + Math.pow(deltaB, 2));
    }

    @Override
    protected float getStartAngle(int xAxis, int yAxis, int zAxis)
    {
        return (float) Math.atan2((double) door.getRotationPoint().x() - xAxis,
                                  (double) door.getRotationPoint().z() - zAxis);
    }
}
