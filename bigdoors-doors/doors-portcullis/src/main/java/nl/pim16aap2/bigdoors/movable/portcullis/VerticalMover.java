package nl.pim16aap2.bigdoors.movable.portcullis;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionCause;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionType;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableSnapshot;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
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

    public VerticalMover(
        Context context, AbstractMovable movable, MovableSnapshot snapshot, double time, boolean skipAnimation,
        int blocksToMove, IPPlayer player, Cuboid newCuboid, MovableActionCause cause, MovableActionType actionType)
        throws Exception
    {
        super(context, movable, snapshot, time, skipAnimation, RotateDirection.NONE, player, newCuboid, cause,
              actionType);
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
