package nl.pim16aap2.bigdoors.doors.portcullis;

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
import org.jetbrains.annotations.Nullable;

/**
 * Represents a {@link BlockMover} for {@link Portcullis}'s.
 *
 * @author Pim
 */
public class VerticalMover extends BlockMover
{
    private double step;

    private @Nullable PBlockData firstBlockData = null;

    protected final int blocksToMove;

    public VerticalMover(AbstractDoor door, double time, boolean skipAnimation, int blocksToMove, double multiplier,
                         IPPlayer player, Cuboid newCuboid, DoorActionCause cause, DoorActionType actionType)
        throws Exception
    {
        super(door, time, skipAnimation, RotateDirection.NONE, player, newCuboid, cause, actionType);
        this.blocksToMove = blocksToMove;

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

        init();
        super.startAnimation();
    }

    /**
     * Used for initializing variables such as {@link #endCount} and {@link #soundActive}.
     */
    protected void init()
    {
        super.endCount = (int) (20 * super.time);
        step = ((double) blocksToMove) / ((double) super.endCount);
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
    protected void prepareAnimation()
    {
        // Gets the first block, which will be used as a base for the movement of all other blocks in the animation.
        firstBlockData = savedBlocks.get(0);
    }

    protected Vector3Dd getGoalPos(PBlockData pBlockData, double stepSum)
    {
        return pBlockData.getStartPosition().add(0, stepSum, 0);
    }

    @Override
    protected void executeAnimationStep(int ticks)
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
        for (PBlockData pBlockData : savedBlocks)
            pBlockData.getFBlock().teleport(getGoalPos(pBlockData, stepSum));
    }

    @Override
    protected IPLocation getNewLocation(double radius, double xAxis, double yAxis, double zAxis)
    {
        return locationFactory.create(world, xAxis, yAxis + blocksToMove, zAxis);
    }
}
