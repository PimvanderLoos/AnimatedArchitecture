package nl.pim16aap2.animatedarchitecture.core.api;

import nl.pim16aap2.animatedarchitecture.core.moveblocks.RotatedPosition;

/**
 * Represents a highlighted block that is used for showing previews to users.
 */
public interface IHighlightedBlock
{
    /**
     * Kills the highlighted block and removes it from the world.
     */
    void kill();

    /**
     * Moves this highlighted block to the target position.
     *
     * @param target
     *     The target position the block should move to.
     */
    void moveToTarget(RotatedPosition target);
}
