package nl.pim16aap2.animatedarchitecture.structures.portcullis;

import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.AnimationUtil;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.Animator;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.IAnimator;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;

/**
 * Represents a {@link Animator} for {@link Portcullis}'s.
 *
 * @author Pim
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
    public RotatedPosition getFinalPosition(IVector3D startLocation, float radius)
    {
        return new RotatedPosition(Vector3Dd.of(startLocation).add(0, blocksToMove, 0));
    }

    private RotatedPosition getGoalPos(IAnimatedBlock animatedBlock, double stepSum)
    {
        return new RotatedPosition(animatedBlock.getStartPosition().position().add(0, stepSum, 0));
    }

    @Override
    public void executeAnimationStep(IAnimator animator, int ticks, int ticksRemaining)
    {
        final double stepSum = step * ticks;
        for (final IAnimatedBlock animatedBlock : animator.getAnimatedBlocks())
            animator.applyMovement(animatedBlock, getGoalPos(animatedBlock, stepSum), ticksRemaining);
    }
}
