package nl.pim16aap2.bigdoors.movable.portcullis;

import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.moveblocks.AnimationUtil;
import nl.pim16aap2.bigdoors.moveblocks.Animator;
import nl.pim16aap2.bigdoors.moveblocks.IAnimationComponent;
import nl.pim16aap2.bigdoors.moveblocks.IAnimator;
import nl.pim16aap2.bigdoors.moveblocks.MovementRequestData;
import nl.pim16aap2.bigdoors.util.vector.IVector3D;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;

/**
 * Represents a {@link Animator} for {@link Portcullis}'s.
 *
 * @author Pim
 */
public final class VerticalAnimationComponent implements IAnimationComponent
{
    private final int blocksToMove;
    private final double step;

    public VerticalAnimationComponent(MovementRequestData data, int blocksToMove)
    {
        this.blocksToMove = blocksToMove;

        final double animationDuration =
            AnimationUtil.getAnimationTicks(data.getAnimationTime(), data.getServerTickTime());
        step = blocksToMove / animationDuration;
    }

    @Override
    public Vector3Dd getFinalPosition(IVector3D startLocation, float radius)
    {
        return Vector3Dd.of(startLocation).add(0, blocksToMove, 0);
    }

    private Vector3Dd getGoalPos(IAnimatedBlock animatedBlock, double stepSum)
    {
        return animatedBlock.getStartPosition().add(0, stepSum, 0);
    }

    @Override
    public void executeAnimationStep(IAnimator animator, int ticks, int ticksRemaining)
    {
        final double stepSum = step * ticks;
        for (final IAnimatedBlock animatedBlock : animator.getAnimatedBlocks())
            animator.applyMovement(animatedBlock, getGoalPos(animatedBlock, stepSum), ticksRemaining);
    }
}
