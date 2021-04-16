package nl.pim16aap2.bigdoors.doors.slidingdoor;

import lombok.NonNull;
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
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3DdConst;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a {@link BlockMover} for {@link SlidingDoor}.
 *
 * @author Pim
 */
public class SlidingMover extends BlockMover
{
    private final boolean NS;
    private final int moveX;
    private final int moveZ;

    private double step;

    private @Nullable PBlockData firstBlockData = null;

    protected final int blocksToMove;

    public SlidingMover(final @NonNull AbstractDoorBase door, final double time, final boolean skipAnimation,
                        final int blocksToMove, final @NonNull RotateDirection openDirection, final double multiplier,
                        final @NonNull IPPlayer player, final @NonNull CuboidConst newCuboid,
                        final @NonNull DoorActionCause cause, final @NonNull DoorActionType actionType)
        throws Exception
    {
        super(door, time, skipAnimation, openDirection, player, newCuboid, cause, actionType);
        this.blocksToMove = blocksToMove;

        NS = openDirection.equals(RotateDirection.NORTH) || openDirection.equals(RotateDirection.SOUTH);

        moveX = NS ? 0 : blocksToMove;
        moveZ = NS ? blocksToMove : 0;

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

        // If the non-default exceeds the max-speed or isn't set, calculate default speed.
        if (time == 0.0 || speed > maxSpeed)
        {
            speed = 1.4 * pcMult;
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
    protected @NonNull IPLocation getNewLocation(final double radius, final double xAxis, final double yAxis,
                                                 final double zAxis)
    {
        return locationFactory.create(world, xAxis + moveX, yAxis, zAxis + moveZ);
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
    protected void prepareAnimation()
    {
        // Gets the first block, which will be used as a base for the movement of all other blocks in the animation.
        firstBlockData = savedBlocks.get(0);
    }

    protected @NonNull Vector3Dd getGoalPos(final @NonNull PBlockData pBlockData, final double stepSum)
    {
        return pBlockData.getStartPosition().add(NS ? 0 : stepSum, 0, NS ? stepSum : 0);
    }

    @Override
    protected void executeAnimationStep(final int ticks)
    {
        if (firstBlockData == null)
            return;

        final double stepSum = step * ticks;
        for (final PBlockData pBlockData : savedBlocks)
            pBlockData.getFBlock().teleport(getGoalPos(pBlockData, stepSum));
    }
}
