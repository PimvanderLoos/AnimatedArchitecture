package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.ICustomCraftFallingBlock;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPLocationConst;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.api.PSound;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.PSoundDescription;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3DdConst;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class BigDoorMover extends BlockMover
{
    private final Vector3DdConst rotationCenter;
    private int halfEndCount;
    private final double angle;
    private final double endSin;
    private final double endCos;
    private double step;

    public BigDoorMover(final @NotNull AbstractDoorBase door, final @NotNull RotateDirection rotDirection,
                        final double time, final boolean skipAnimation, final double multiplier,
                        @NotNull final IPPlayer player, final @NotNull Vector3DiConst finalMin,
                        final @NotNull Vector3DiConst finalMax, final @NotNull DoorActionCause cause,
                        final @NotNull DoorActionType actionType)
    {
        super(door, time, skipAnimation, rotDirection, player, finalMin, finalMax, cause, actionType);

        angle = rotDirection == RotateDirection.CLOCKWISE ? Math.PI / 2 :
                rotDirection == RotateDirection.COUNTERCLOCKWISE ? -Math.PI / 2 : 0.0D;

        if (angle == 0.0D)
            PLogger.get().severe("Invalid open direction \"" + rotDirection.name() + "\" for door: " + getDoorUID());

        endCos = Math.cos(angle);
        endSin = Math.sin(angle);

        rotationCenter = new Vector3Dd(door.getEngine().getX() + 0.5, yMin, door.getEngine().getZ() + 0.5);

        final int xLen = Math.abs(door.getMaximum().getX() - door.getMinimum().getX());
        final int zLen = Math.abs(door.getMaximum().getZ() - door.getMinimum().getZ());
        final int doorLength = Math.max(xLen, zLen) + 1;
        final double[] vars = Util.calculateTimeAndTickRate(doorLength, time, multiplier, 3.7);
        super.time = vars[0];
//        super.tickRate = (int) vars[1];
        super.tickRate = 1;

        init();
        super.startAnimation();
    }

    /**
     * Used for initializing variables such as {@link #endCount} and {@link #soundActive}.
     */
    protected void init()
    {
        super.endCount = (int) (20 * super.time);
        step = angle / super.endCount;
        halfEndCount = super.endCount / 2;
        super.soundActive = new PSoundDescription(PSound.DRAGGING, 0.8f, 0.7f);
        super.soundFinish = new PSoundDescription(PSound.THUD, 0.2f, 0.15f);
    }

    @Override
    protected Vector3Dd getFinalPosition(final @NotNull PBlockData block)
    {
        final @NotNull Vector3DdConst startLocation = block.getStartPosition();
        final @NotNull IPLocationConst finalLoc = getNewLocation(block.getRadius(), startLocation.getX(),
                                                                 startLocation.getY(), startLocation.getZ());
        return new Vector3Dd(finalLoc.getBlockX() + 0.5, finalLoc.getBlockY(), finalLoc.getBlockZ() + 0.5);
//        return new Vector3Dd(finalLoc.getBlockX(), finalLoc.getBlockY(), finalLoc.getBlockZ());
//        return new Vector3Dd(finalLoc.getBlockX() + 1, finalLoc.getBlockY(), finalLoc.getBlockZ() + 1);
    }

    private IPPlayer pim16aap2 = null;

    private boolean hasPreparedEnd = false;

    @Override
    protected void prepareToEndAnimation()
    {
        if (hasPreparedEnd)
            return;
        for (final PBlockData block : savedBlocks)
        {
            final Vector3Dd goalPos = getGoalPos(endCos, endSin, block.getStartX(), block.getStartY(),
                                                 block.getStartZ());
            block.getFBlock().teleport(goalPos, new Vector3Dd(0, 0, 0),
                                       ICustomCraftFallingBlock.TeleportMode.NO_VELOCITY);
        }
        hasPreparedEnd = true;
    }

    @Override
    protected void executeAnimationStep(final int ticks)
    {
        if (ticks == halfEndCount)
            applyRotation();

        if (pim16aap2 == null)
            pim16aap2 = BigDoors.get().getPlatform().getPPlayerFactory()
                                .create(UUID.fromString("27e6c556-4f30-32bf-a005-c80a46ddd935"), "pim16aap2");

        final double stepSum = step * ticks;
        final double cos = Math.cos(stepSum);
        final double sin = Math.sin(stepSum);

        for (final PBlockData block : savedBlocks)
        {
            final double radius = block.getRadius();
            if (radius == 0)
                continue;

//            if (ticks % 20 > 0)
//                continue;

            final Vector3Dd goalPos = getGoalPos(cos, sin, block.getStartX(), block.getStartY(), block.getStartZ());

//            BigDoors.get().getMessagingInterface().broadcastMessage(goalPos.toString(3));

//            block.getFBlock().setPosition(goalPos, new Vector3Dd(0, 0, 0));
            block.getFBlock().teleport(goalPos);

//            if (ticks % 2 == 0)
//                BigDoors.get().getPlatform().getGlowingBlockSpawner()
//                        .spawnGlowingBlock(pim16aap2, door.getWorld().getUID(), 1,
//                                           goalPos.getX(), goalPos.getY(), goalPos.getZ(), PColor.GOLD);

//            final Vector3Dd vec = goalPos.subtract(block.getFBlock().getPosition());
//            block.getFBlock().setVelocity(vec.multiply(0.101));
        }
    }

    @NotNull
    private Vector3Dd getGoalPos(final double angle, final double startX, final double startY, final double startZ)
    {
        return getGoalPos(Math.cos(angle), Math.sin(angle), startX, startY, startZ);
    }

    @NotNull
    private Vector3Dd getGoalPos(final double cos, final double sin, final double startX, final double startY,
                                 final double startZ)
    {
        double translatedX = startX - rotationCenter.getX();
        double translatedZ = startZ - rotationCenter.getZ();

        double changeX = translatedX * cos - translatedZ * sin;
        double changeZ = translatedX * sin + translatedZ * cos;

        return new Vector3Dd(rotationCenter.getX() + changeX, startY, rotationCenter.getZ() + changeZ);
    }

    @Override
    @NotNull
    protected IPLocation getNewLocation(final double radius, final double xAxis, final double yAxis, final double zAxis)
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
