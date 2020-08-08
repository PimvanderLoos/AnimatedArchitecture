package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPLocationConst;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PBlockData;
import nl.pim16aap2.bigdoors.api.PSound;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.Elevator;
import nl.pim16aap2.bigdoors.doors.Portcullis;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PSoundDescription;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.IVector3DdConst;
import nl.pim16aap2.bigdoors.util.vector.IVector3DiConst;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a {@link BlockMover} for {@link Portcullis}'s and {@link Elevator}.
 *
 * @author Pim
 */
public class VerticalMover extends BlockMover
{
    private double step;

    @Nullable
    private PBlockData firstBlockData = null;

    public VerticalMover(final double time, final @NotNull AbstractDoorBase door, final boolean skipAnimation,
                         final int blocksToMove, final double multiplier, final @NotNull IPPlayer player,
                         final @NotNull IVector3DiConst finalMin, final @NotNull IVector3DiConst finalMax,
                         final @NotNull DoorActionCause cause, final @NotNull DoorActionType actionType)
    {
        super(door, time, skipAnimation, PBlockFace.UP, RotateDirection.NONE, blocksToMove, player, finalMin,
              finalMax, cause, actionType);

        double speed = 1;
        double pcMult = multiplier;
        pcMult = pcMult == 0.0 ? 1.0 : pcMult;
        final int maxSpeed = 6;

        // If the time isn't default, calculate speed.
        if (time != 0.0)
        {
            speed = Math.abs(blocksToMove) / time;
            super.time = time;
        }

        // If the non-default exceeds the max-speed or isn't set, calculate default
        // speed.
        if (time == 0.0 || speed > maxSpeed)
        {
            speed = blocksToMove < 0 ? 1.7 : 0.8 * pcMult;
            speed = speed > maxSpeed ? maxSpeed : speed;
            super.time = Math.abs(blocksToMove) / speed;
        }

        tickRate = Util.tickRateFromSpeed(speed);

        init();
        super.startAnimation();
    }

    /**
     * Used for initializing variables such as {@link #endCount} and {@link #soundActive}.
     */
    protected void init()
    {
        super.endCount = (int) (20 * super.time);
        step = ((double) blocksMoved) / ((double) super.endCount);
        super.soundActive = new PSoundDescription(PSound.DRAGGING, 0.8f, 0.7f);
        super.soundFinish = new PSoundDescription(PSound.THUD, 0.2f, 0.15f);
    }

    @Override
    protected Vector3Dd getFinalPosition(final @NotNull PBlockData block)
    {
        final @NotNull IVector3DdConst startLocation = block.getStartPosition();
        final @NotNull IPLocationConst finalLoc = getNewLocation(block.getRadius(), startLocation.getX(),
                                                                 startLocation.getY(), startLocation.getZ());
        return new Vector3Dd(finalLoc.getBlockX() + 0.5, finalLoc.getBlockY(), finalLoc.getBlockZ() + 0.5);
    }

    @Override
    protected void prepareAnimation()
    {
        // Gets the first block, which will be used as a base for the movement of all other blocks in the animation.
        firstBlockData = savedBlocks.get(0);
    }

    @Override
    protected void executeAnimationStep(final int ticks)
    {
//        // This isn't used currently, but the idea is to spawn solid blocks where this door is / is going to be.
//        // A cheap way to create fake solid blocks. Should really be part of the blocks themselves, but
//        // this was just to see how viable it is. Leaving it here for future reference.
//        BigDoors.get().getPlatform().newPExecutor().runSync(
//            () ->
//            {
//                int fullBlocksMoved = (int) Math.round(stepSum + Math.max(0.5, 2 * step));
//                Vector3Di newMin = new Vector3Di(door.getMinimum().getX(),
//                                                 door.getMinimum().getY() + fullBlocksMoved,
//                                                 door.getMinimum().getZ());
//                Vector3Di newMax = new Vector3Di(door.getMaximum().getX(),
//                                                 door.getMaximum().getY() + fullBlocksMoved,
//                                                 door.getMaximum().getZ());
//                updateSolidBlocks(newMin, newMax);
//            });

        if (firstBlockData == null)
            return;

        final double stepSum = step * ticks;

        final Vector3Dd pos = firstBlockData.getStartPosition();
        pos.add(0, stepSum, 0);

        final Vector3Dd vec = pos.subtract(firstBlockData.getFBlock().getPosition());
        vec.multiply(0.101);

        for (final PBlockData mbd : savedBlocks)
            mbd.getFBlock().setVelocity(vec);
    }

    @Override
    protected IPLocation getNewLocation(final double radius, final double xAxis, final double yAxis, final double zAxis)
    {
        return locationFactory.create(world, xAxis, yAxis + blocksMoved, zAxis);
    }
}
