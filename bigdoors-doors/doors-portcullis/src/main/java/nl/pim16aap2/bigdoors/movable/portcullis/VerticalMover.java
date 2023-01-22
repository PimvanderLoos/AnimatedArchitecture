package nl.pim16aap2.bigdoors.movable.portcullis;

import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.moveblocks.MovementRequestData;
import nl.pim16aap2.bigdoors.util.vector.IVector3D;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;

/**
 * Represents a {@link BlockMover} for {@link Portcullis}'s.
 *
 * @author Pim
 */
public class VerticalMover extends BlockMover
{
    protected final int blocksToMove;
    private final double step;

    public VerticalMover(AbstractMovable movable, MovementRequestData data, int blocksToMove)
        throws Exception
    {
        super(movable, data);
        this.blocksToMove = blocksToMove;
        step = ((double) blocksToMove) / ((double) super.animationDuration);
    }

    @Override
    protected Vector3Dd getFinalPosition(IVector3D startLocation, float radius)
    {
        return Vector3Dd.of(startLocation).add(0, blocksToMove, 0);
    }

    protected Vector3Dd getGoalPos(IAnimatedBlock animatedBlock, double stepSum)
    {
        return animatedBlock.getStartPosition().add(0, stepSum, 0);
    }

    @Override
    protected void executeAnimationStep(int ticks, int ticksRemaining)
    {
        final double stepSum = step * ticks;
        for (final IAnimatedBlock animatedBlock : getAnimatedBlocks())
            applyMovement(animatedBlock, getGoalPos(animatedBlock, stepSum), ticksRemaining);
    }
}
