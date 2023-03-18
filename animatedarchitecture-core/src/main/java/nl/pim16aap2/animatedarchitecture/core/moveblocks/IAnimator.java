package nl.pim16aap2.animatedarchitecture.core.moveblocks;

import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;

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
     */
    void applyMovement(IAnimatedBlock animatedBlock, RotatedPosition goalPos);
}
