package nl.pim16aap2.bigdoors.doors.bigdoor;

import lombok.extern.flogger.Flogger;
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
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;

import java.util.logging.Level;

@Flogger
public class BigDoorMover extends BlockMover
{
    private final Vector3Dd rotationCenter;
    private int halfEndCount;
    private final double angle;
    private double step;

    public BigDoorMover(Context context, AbstractDoor door, RotateDirection rotDirection, double time,
                        boolean skipAnimation, double multiplier, IPPlayer player, Cuboid newCuboid,
                        DoorActionCause cause, DoorActionType actionType)
        throws Exception
    {
        super(context, door, time, skipAnimation, rotDirection, player, newCuboid, cause, actionType);

        angle = rotDirection == RotateDirection.CLOCKWISE ? Math.PI / 2 :
                rotDirection == RotateDirection.COUNTERCLOCKWISE ? -Math.PI / 2 : 0.0D;

        if (angle == 0.0D)
            log.at(Level.SEVERE).log("Invalid open direction '%s' for door: %d", rotDirection.name(), getDoorUID());

        rotationCenter = new Vector3Dd(door.getRotationPoint().x() + 0.5, yMin, door.getRotationPoint().z() + 0.5);

        final int xLen = Math.abs(door.getMaximum().x() - door.getMinimum().x());
        final int zLen = Math.abs(door.getMaximum().z() - door.getMinimum().z());
        final int doorLength = Math.max(xLen, zLen) + 1;
        final double[] vars = Util.calculateTimeAndTickRate(doorLength, time, multiplier, 3.7);
        super.time = vars[0];

        init();
        super.startAnimation();
    }

    /**
     * Used for initializing variables such as {@link #endCount} and {@link #soundActive}.
     */
    protected void init()
    {
        super.endCount = (int) (20 * super.time) + 1;
        step = angle / super.endCount;
        halfEndCount = super.endCount / 2;
        super.soundActive = new PSoundDescription(PSound.DRAGGING, 0.8f, 0.7f);
        super.soundFinish = new PSoundDescription(PSound.THUD, 0.2f, 0.15f);
    }

    @Override
    protected Vector3Dd getFinalPosition(PBlockData block)
    {
        final Vector3Dd startLocation = block.getStartPosition();
        final IPLocation finalLoc = getNewLocation(block.getRadius(), startLocation.x(),
                                                   startLocation.y(), startLocation.z());
        return new Vector3Dd(finalLoc.getBlockX() + 0.5, finalLoc.getBlockY(), finalLoc.getBlockZ() + 0.5);
    }

    @Override
    protected void executeAnimationStep(int ticks)
    {
        if (ticks == halfEndCount)
            applyRotation();

        final double stepSum = step * ticks;
        final double cos = Math.cos(stepSum);
        final double sin = Math.sin(stepSum);

        for (final PBlockData block : savedBlocks)
            block.getFBlock().teleport(getGoalPos(cos, sin, block.getStartX(), block.getStartY(), block.getStartZ()));
    }


    private Vector3Dd getGoalPos(double angle, double startX, double startY, double startZ)
    {
        return getGoalPos(Math.cos(angle), Math.sin(angle), startX, startY, startZ);
    }


    private Vector3Dd getGoalPos(double cos, double sin, double startX, double startY, double startZ)
    {
        final double translatedX = startX - rotationCenter.x();
        final double translatedZ = startZ - rotationCenter.z();

        final double changeX = translatedX * cos - translatedZ * sin;
        final double changeZ = translatedX * sin + translatedZ * cos;

        return new Vector3Dd(rotationCenter.x() + changeX, startY, rotationCenter.z() + changeZ);
    }

    @Override
    protected IPLocation getNewLocation(double radius, double xAxis, double yAxis, double zAxis)
    {
        return locationFactory.create(world, getGoalPos(angle, xAxis, yAxis, zAxis));
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
