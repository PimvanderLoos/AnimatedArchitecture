package nl.pim16aap2.bigdoors.doors.portcullis;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.IVector3D;
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

    private @Nullable IAnimatedBlock firstBlockData = null;

    protected final int blocksToMove;

    public VerticalMover(
        Context context, AbstractDoor door, double time, boolean skipAnimation, int blocksToMove, double multiplier,
        IPPlayer player, Cuboid newCuboid, DoorActionCause cause, DoorActionType actionType)
        throws Exception
    {
        super(context, door, time, skipAnimation, RotateDirection.NONE, player, newCuboid, cause, actionType);
        this.blocksToMove = blocksToMove;

        double speed = 1;
        double pcMultiplier = multiplier;
        pcMultiplier = pcMultiplier == 0.0 ? 1.0 : pcMultiplier;
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
            speed = blocksToMove < 0 ? 1.7 : 0.8 * pcMultiplier;
            speed = speed > maxSpeed ? maxSpeed : speed;
            super.time = Math.abs(blocksToMove) / speed;
        }

        init();
        super.startAnimation();
    }

    /**
     * Used for initializing variables such as {@link #animationDuration}.
     */
    protected void init()
    {
        super.animationDuration = (int) (20 * super.time);
        step = ((double) blocksToMove) / ((double) super.animationDuration);
    }

    @Override
    protected Vector3Dd getFinalPosition(IVector3D startLocation, float radius)
    {
        return Vector3Dd.of(startLocation).add(0, blocksToMove, 0);
    }

    @Override
    protected void prepareAnimation()
    {
        super.prepareAnimation();
        // Gets the first block, which will be used as a base for the movement of all other blocks in the animation.
        firstBlockData = animatedBlocks.isEmpty() ? null : animatedBlocks.get(0);
    }

    protected Vector3Dd getGoalPos(IAnimatedBlock animatedBlock, double stepSum)
    {
        return animatedBlock.getStartPosition().add(0, stepSum, 0);
    }

    // Yes, it's bad practice to keep commented-out code around.
    // However, I do intend to use it again in the future, and it's easier to leave it here than hide it somewhere.
    @SuppressWarnings("CommentedOutCode")
    @Override
    protected void executeAnimationStep(int ticks)
    {
        // TODO: Check if this is worth pursuing with the new movement system.
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
        for (final IAnimatedBlock animatedBlock : animatedBlocks)
            applyMovement(animatedBlock, getGoalPos(animatedBlock, stepSum));
    }
}
