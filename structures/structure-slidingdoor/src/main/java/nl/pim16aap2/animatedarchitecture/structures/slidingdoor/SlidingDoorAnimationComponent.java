package nl.pim16aap2.animatedarchitecture.structures.slidingdoor;

import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.AnimationUtil;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.Animator;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.IAnimator;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a {@link Animator} for {@link SlidingDoor}.
 *
 * @author Pim
 */
public class SlidingDoorAnimationComponent implements IAnimationComponent
{
    private final boolean northSouth;
    private final int moveX;
    private final int moveZ;

    private final double step;

    private volatile @Nullable IAnimatedBlock firstBlockData = null;

    protected final int blocksToMove;

    protected final StructureSnapshot snapshot;

    public SlidingDoorAnimationComponent(
        AnimationRequestData data, MovementDirection movementDirection, int blocksToMove)
    {
        this.snapshot = data.getStructureSnapshot();
        this.blocksToMove = blocksToMove;

        northSouth =
            movementDirection.equals(MovementDirection.NORTH) || movementDirection.equals(MovementDirection.SOUTH);

        moveX = northSouth ? 0 : blocksToMove;
        moveZ = northSouth ? blocksToMove : 0;

        final double animationDuration =
            AnimationUtil.getAnimationTicks(data.getAnimationTime(), data.getServerTickTime());
        step = blocksToMove / animationDuration;
    }

    @Override
    public RotatedPosition getFinalPosition(IVector3D startLocation, float radius)
    {
        return new RotatedPosition(Vector3Dd.of(startLocation).add(moveX, 0, moveZ));
    }

    @Override
    public void prepareAnimation(IAnimator animator)
    {
        // Gets the first block, which will be used as a base for the movement of all other blocks in the animation.
        firstBlockData = animator.getAnimatedBlocks().isEmpty() ? null : animator.getAnimatedBlocks().get(0);
    }

    protected RotatedPosition getGoalPos(IAnimatedBlock animatedBlock, double stepSum)
    {
        return new RotatedPosition(
            animatedBlock.getStartPosition().position()
                         .add(northSouth ? 0 : stepSum,
                              0,
                              northSouth ? stepSum : 0));
    }

    @Override
    public void executeAnimationStep(IAnimator animator, int ticks, int ticksRemaining)
    {
        if (firstBlockData == null)
            return;

        final double stepSum = step * ticks;
        for (final IAnimatedBlock animatedBlock : animator.getAnimatedBlocks())
            animator.applyMovement(animatedBlock, getGoalPos(animatedBlock, stepSum), ticksRemaining);
    }
}
