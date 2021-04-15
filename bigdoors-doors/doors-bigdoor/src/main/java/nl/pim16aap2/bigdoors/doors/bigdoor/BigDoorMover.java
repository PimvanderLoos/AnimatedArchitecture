package nl.pim16aap2.bigdoors.doors.bigdoor;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPLocationConst;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.api.PSound;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.CuboidConst;
import nl.pim16aap2.bigdoors.util.PSoundDescription;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3DdConst;

public class BigDoorMover extends BlockMover
{
    private final @NonNull Vector3DdConst rotationCenter;
    private int halfEndCount;
    private final double angle;
    private final double endSin;
    private final double endCos;
    private double step;

    public BigDoorMover(final @NonNull AbstractDoorBase door, final @NonNull RotateDirection rotDirection,
                        final double time, final boolean skipAnimation, final double multiplier,
                        final @NonNull IPPlayer player, final @NonNull CuboidConst newCuboid,
                        final @NonNull DoorActionCause cause, final @NonNull DoorActionType actionType)
        throws Exception
    {
        super(door, time, skipAnimation, rotDirection, player, newCuboid, cause, actionType);

        angle = rotDirection == RotateDirection.CLOCKWISE ? Math.PI / 2 :
                rotDirection == RotateDirection.COUNTERCLOCKWISE ? -Math.PI / 2 : 0.0D;

        if (angle == 0.0D)
            BigDoors.get().getPLogger()
                    .severe("Invalid open direction \"" + rotDirection.name() + "\" for door: " + getDoorUID());

        endCos = Math.cos(angle);
        endSin = Math.sin(angle);

        rotationCenter = new Vector3Dd(door.getEngine().getX() + 0.5, yMin, door.getEngine().getZ() + 0.5);

        final int xLen = Math.abs(door.getMaximum().getX() - door.getMinimum().getX());
        final int zLen = Math.abs(door.getMaximum().getZ() - door.getMinimum().getZ());
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
    protected @NonNull Vector3Dd getFinalPosition(final @NonNull PBlockData block)
    {
        final @NonNull Vector3DdConst startLocation = block.getStartPosition();
        final @NonNull IPLocationConst finalLoc = getNewLocation(block.getRadius(), startLocation.getX(),
                                                                 startLocation.getY(), startLocation.getZ());
        return new Vector3Dd(finalLoc.getBlockX() + 0.5, finalLoc.getBlockY(), finalLoc.getBlockZ() + 0.5);
    }

    @Override
    protected void executeAnimationStep(final int ticks)
    {
        if (ticks == halfEndCount)
            applyRotation();

        final double stepSum = step * ticks;
        final double cos = Math.cos(stepSum);
        final double sin = Math.sin(stepSum);

        for (final PBlockData block : savedBlocks)
            block.getFBlock().teleport(getGoalPos(cos, sin, block.getStartX(), block.getStartY(), block.getStartZ()));
    }


    private @NonNull Vector3Dd getGoalPos(final double angle, final double startX, final double startY,
                                          final double startZ)
    {
        return getGoalPos(Math.cos(angle), Math.sin(angle), startX, startY, startZ);
    }


    private @NonNull Vector3Dd getGoalPos(final double cos, final double sin, final double startX, final double startY,
                                          final double startZ)
    {
        double translatedX = startX - rotationCenter.getX();
        double translatedZ = startZ - rotationCenter.getZ();

        double changeX = translatedX * cos - translatedZ * sin;
        double changeZ = translatedX * sin + translatedZ * cos;

        return new Vector3Dd(rotationCenter.getX() + changeX, startY, rotationCenter.getZ() + changeZ);
    }

    @Override
    protected @NonNull IPLocation getNewLocation(final double radius, final double xAxis, final double yAxis,
                                                 final double zAxis)
    {
        return locationFactory.create(world, getGoalPos(angle, xAxis, yAxis, zAxis));
    }

    @Override
    protected float getRadius(final int xAxis, final int yAxis, final int zAxis)
    {
        final double deltaA = door.getEngine().getX() - xAxis;
        final double deltaB = door.getEngine().getZ() - zAxis;
        return (float) Math.sqrt(Math.pow(deltaA, 2) + Math.pow(deltaB, 2));
    }

    @Override
    protected float getStartAngle(final int xAxis, final int yAxis, final int zAxis)
    {
        return (float) Math.atan2(door.getEngine().getX() - xAxis, door.getEngine().getZ() - zAxis);
    }
}
