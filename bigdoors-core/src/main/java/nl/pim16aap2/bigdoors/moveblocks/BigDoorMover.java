package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.api.PSound;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.getnewlocation.GNLHorizontalRotEast;
import nl.pim16aap2.bigdoors.moveblocks.getnewlocation.GNLHorizontalRotNorth;
import nl.pim16aap2.bigdoors.moveblocks.getnewlocation.GNLHorizontalRotSouth;
import nl.pim16aap2.bigdoors.moveblocks.getnewlocation.GNLHorizontalRotWest;
import nl.pim16aap2.bigdoors.moveblocks.getnewlocation.IGetNewLocation;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.PSoundDescription;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;

public class BigDoorMover extends BlockMover
{
    private final int stepMultiplier;
    private final Vector3Di turningPoint;
    private final IGetNewLocation gnl;
    private double endStepSum;
    private double multiplier;
    private double startStepSum;

    private int halfEndCount;
    private double step;

    private Vector3Dd rotationCenter;

    public BigDoorMover(final @NotNull RotateDirection rotDirection, final double time,
                        final @NotNull PBlockFace currentDirection, final @NotNull AbstractDoorBase door,
                        final boolean skipAnimation, final double multiplier, @NotNull final IPPlayer player,
                        final @NotNull Vector3Di finalMin, final @NotNull Vector3Di finalMax,
                        final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType)
    {
        super(door, time, skipAnimation, currentDirection, rotDirection, -1, player, finalMin, finalMax, cause,
              actionType);

        turningPoint = door.getEngine();
        rotationCenter = new Vector3Dd(turningPoint.getX() + 0.5, yMin, turningPoint.getZ() + 0.5);
        stepMultiplier = rotDirection == RotateDirection.CLOCKWISE ? -1 : 1;

        final int xLen = Math.abs(door.getMaximum().getX() - door.getMinimum().getX());
        final int zLen = Math.abs(door.getMaximum().getZ() - door.getMinimum().getZ());
        final int doorLength = Math.max(xLen, zLen) + 1;
        final double[] vars = Util.calculateTimeAndTickRate(doorLength, time, multiplier, 3.7);
        super.time = vars[0];
        super.tickRate = (int) vars[1];
        this.multiplier = vars[2];

        switch (currentDirection)
        {
            case NORTH:
                gnl = new GNLHorizontalRotNorth(world, xMin, xMax, zMin, zMax, rotDirection);
                startStepSum = Math.PI;
                endStepSum = rotDirection == RotateDirection.CLOCKWISE ? Math.PI / 2 : 3 * Math.PI / 2;
                break;
            case EAST:
                gnl = new GNLHorizontalRotEast(world, xMin, xMax, zMin, zMax, rotDirection);
                startStepSum = Math.PI / 2;
                endStepSum = rotDirection == RotateDirection.CLOCKWISE ? 0 : Math.PI;
                break;
            case SOUTH:
                gnl = new GNLHorizontalRotSouth(world, xMin, xMax, zMin, zMax, rotDirection);
                startStepSum = 0;
                endStepSum = rotDirection == RotateDirection.CLOCKWISE ? 3 * Math.PI / 2 : Math.PI / 2;
                break;
            case WEST:
                gnl = new GNLHorizontalRotWest(world, xMin, xMax, zMin, zMax, rotDirection);
                startStepSum = 3 * Math.PI / 2;
                endStepSum = rotDirection == RotateDirection.CLOCKWISE ? Math.PI : 0;
                break;
            default:
                PLogger.get()
                       .dumpStackTrace(
                           "Invalid currentDirection for cylindrical mover: " + currentDirection.toString());
                gnl = null;
                break;
        }
        BigDoors.get().getMessagingInterface()
                .broadcastMessage("Constructor BIGDOOR! endCount = " + endCount + ", time = " + super.time);

        init();
        super.startAnimation();
    }

    /**
     * Used for initializing variables such as {@link #endCount} and {@link #soundActive}.
     */
    protected void init()
    {
        super.endCount = 20 * (int) super.time;
        step = (Math.PI / 2.0f) / super.endCount * stepMultiplier;
        halfEndCount = super.endCount / 2;
        super.soundActive = new PSoundDescription(PSound.DRAGGING, 0.8f, 0.7f);
        super.soundFinish = new PSoundDescription(PSound.THUD, 0.2f, 0.15f);
        BigDoors.get().getMessagingInterface()
                .broadcastMessage("INIT BIGDOOR! endCount = " + endCount + ", time = " + super.time);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Vector3Dd getFinalPosition(final @NotNull PBlockData block)
    {
        final @NotNull Vector3Dd startLocation = block.getStartPosition();
        final @NotNull IPLocation finalLoc = getNewLocation(block.getRadius(), startLocation.getX(),
                                                            startLocation.getY(), startLocation.getZ());
        return new Vector3Dd(finalLoc.getBlockX() + 0.5, finalLoc.getBlockY(), finalLoc.getBlockZ() + 0.5);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void executeAnimationStep(final int ticks)
    {
        final double stepSum = startStepSum + step * ticks;

        if (ticks == halfEndCount)
            applyRotation();

        final double sin = Math.sin(stepSum);
        final double cos = Math.cos(stepSum);

        for (final PBlockData block : savedBlocks)
        {
            final double radius = block.getRadius();
            final int yPos = block.getStartLocation().getBlockY();

            if (radius != 0)
            {
                final double addX = radius * sin;
                final double addZ = radius * cos;

                final Vector3Dd position = new Vector3Dd(rotationCenter.getX() + addX, yPos,
                                                         rotationCenter.getZ() + addZ);
                final Vector3Dd vec = position.subtract(block.getFBlock().getPosition());
                block.getFBlock().setVelocity(vec.multiply(0.101));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IPLocation getNewLocation(final double radius, final double xAxis, final double yAxis, final double zAxis)
    {
        return gnl.getNewLocation(radius, xAxis, yAxis, zAxis);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected float getRadius(final int xAxis, final int yAxis, final int zAxis)
    {
        // Get the radius of this pillar.
        return Math.max(Math.abs(xAxis - turningPoint.getX()), Math.abs(zAxis - turningPoint.getZ()));
    }
}
