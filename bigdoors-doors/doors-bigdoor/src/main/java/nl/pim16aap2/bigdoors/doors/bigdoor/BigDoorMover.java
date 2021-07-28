package nl.pim16aap2.bigdoors.doors.bigdoor;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.api.PSound;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.PSoundDescription;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import org.jetbrains.annotations.NotNull;

public class BigDoorMover extends BlockMover
{
    private final @NotNull Vector3Dd rotationCenter;
    private int halfEndCount;
    private final double angle;
    private final double endSin;
    private final double endCos;
    private double step;

    public BigDoorMover(final @NotNull AbstractDoorBase door, final @NotNull RotateDirection rotDirection,
                        final double time, final boolean skipAnimation, final double multiplier,
                        final @NotNull IPPlayer player, final @NotNull Cuboid newCuboid,
                        final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType)
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

        rotationCenter = new Vector3Dd(door.getEngine().x() + 0.5, yMin, door.getEngine().z() + 0.5);

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
    protected @NotNull Vector3Dd getFinalPosition(final @NotNull PBlockData block)
    {
        final @NotNull Vector3Dd startLocation = block.getStartPosition();
        final @NotNull IPLocation finalLoc = getNewLocation(block.getRadius(), startLocation.x(),
                                                            startLocation.y(), startLocation.z());
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


    private @NotNull Vector3Dd getGoalPos(final double angle, final double startX, final double startY,
                                          final double startZ)
    {
        return getGoalPos(Math.cos(angle), Math.sin(angle), startX, startY, startZ);
    }


    private @NotNull Vector3Dd getGoalPos(final double cos, final double sin, final double startX, final double startY,
                                          final double startZ)
    {
        double translatedX = startX - rotationCenter.x();
        double translatedZ = startZ - rotationCenter.z();

        double changeX = translatedX * cos - translatedZ * sin;
        double changeZ = translatedX * sin + translatedZ * cos;

        return new Vector3Dd(rotationCenter.x() + changeX, startY, rotationCenter.z() + changeZ);
    }

    @Override
    protected @NotNull IPLocation getNewLocation(final double radius, final double xAxis, final double yAxis,
                                                 final double zAxis)
    {
        return locationFactory.create(world, getGoalPos(angle, xAxis, yAxis, zAxis));
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
