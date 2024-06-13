package nl.pim16aap2.animatedarchitecture.structures.portcullis;

import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationUtil;
import nl.pim16aap2.animatedarchitecture.core.animation.Animator;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimator;
import nl.pim16aap2.animatedarchitecture.core.animation.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;

/**
 * Represents a {@link Animator} for {@link Portcullis}'s.
 */
public final class VerticalAnimationComponent implements IAnimationComponent
{
    private final int blocksToMove;
    private final double step;

    public VerticalAnimationComponent(AnimationRequestData data, int blocksToMove)
    {
        this.blocksToMove = blocksToMove;

        final double animationDuration =
            AnimationUtil.getAnimationTicks(data.getAnimationTime(), data.getServerTickTime());
        step = blocksToMove / animationDuration;
    }

    @Override
    public RotatedPosition getFinalPosition(int xAxis, int yAxis, int zAxis)
    {
        return new RotatedPosition(new Vector3Dd(xAxis, yAxis + blocksToMove, zAxis));
    }

    private RotatedPosition getGoalPos(IAnimatedBlock animatedBlock, double stepSum)
    {
        return new RotatedPosition(animatedBlock.getStartPosition().position().add(0, stepSum, 0));
    }

    @Override
    public void executeAnimationStep(IAnimator animator, Iterable<IAnimatedBlock> animatedBlocks, int ticks)
    {
        final double stepSum = step * ticks;
        for (final IAnimatedBlock animatedBlock : animatedBlocks)
            animator.applyMovement(animatedBlock, getGoalPos(animatedBlock, stepSum));
    }
}
