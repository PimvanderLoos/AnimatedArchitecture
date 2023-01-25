package nl.pim16aap2.bigdoors.movable.slidingdoor;

import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.moveblocks.MovementRequestData;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import nl.pim16aap2.bigdoors.util.vector.IVector3D;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a {@link BlockMover} for {@link SlidingDoor}.
 *
 * @author Pim
 */
public class SlidingMover extends BlockMover
{
    private final boolean northSouth;
    private final int moveX;
    private final int moveZ;

    private final double step;

    private volatile @Nullable IAnimatedBlock firstBlockData = null;

    protected final int blocksToMove;

    public SlidingMover(
        AbstractMovable movable, MovementRequestData data, MovementDirection movementDirection, int blocksToMove)
        throws Exception
    {
        super(movable, data, movementDirection);
        this.blocksToMove = blocksToMove;

        northSouth =
            movementDirection.equals(MovementDirection.NORTH) || movementDirection.equals(MovementDirection.SOUTH);

        moveX = northSouth ? 0 : blocksToMove;
        moveZ = northSouth ? blocksToMove : 0;

        step = ((double) blocksToMove) / ((double) super.animationDuration);
    }

    @Override
    protected Vector3Dd getFinalPosition(IVector3D startLocation, float radius)
    {
        return Vector3Dd.of(startLocation).add(moveX, 0, moveZ);
    }

    @Override
    protected void prepareAnimation()
    {
        super.prepareAnimation();
        // Gets the first block, which will be used as a base for the movement of all other blocks in the animation.
        firstBlockData = getAnimatedBlocks().isEmpty() ? null : getAnimatedBlocks().get(0);
    }

    protected Vector3Dd getGoalPos(IAnimatedBlock animatedBlock, double stepSum)
    {
        return animatedBlock.getStartPosition().add(northSouth ? 0 : stepSum,
                                                    0,
                                                    northSouth ? stepSum : 0);
    }

    @Override
    protected void executeAnimationStep(int ticks, int ticksRemaining)
    {
        if (firstBlockData == null)
            return;

        final double stepSum = step * ticks;
        for (final IAnimatedBlock animatedBlock : getAnimatedBlocks())
            applyMovement(animatedBlock, getGoalPos(animatedBlock, stepSum), ticksRemaining);
    }
}
