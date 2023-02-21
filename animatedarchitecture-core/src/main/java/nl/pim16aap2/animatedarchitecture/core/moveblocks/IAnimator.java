package nl.pim16aap2.animatedarchitecture.core.moveblocks;

import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;

import java.util.List;

/**
 * Represents an object that controls an animation.
 */
public interface IAnimator
{
    /**
     * @return All the animated blocks that are part of the animation.
     */
    List<IAnimatedBlock> getAnimatedBlocks();

    /**
     * Moves the selected animated block.
     *
     * @param animatedBlock
     *     The animated block that should be moved.
     * @param goalPos
     *     The target position of the animated block.
     * @param ticksRemaining
     *     The number of ticks remaining in the animation.
     */
    void applyMovement(IAnimatedBlock animatedBlock, IVector3D goalPos, int ticksRemaining);

    /**
     * Rotates in the openDirection and then respawns an {@link IAnimatedBlock}. This is executed on the main thread.
     *
     * @param direction
     *     The direction of the rotation.
     */
    void applyRotation(MovementDirection direction);

    /**
     * Respawns all blocks. This is executed on the main thread.
     */
    void respawnBlocks();
}
