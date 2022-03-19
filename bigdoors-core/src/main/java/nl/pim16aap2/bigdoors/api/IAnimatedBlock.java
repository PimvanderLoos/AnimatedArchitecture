package nl.pim16aap2.bigdoors.api;

import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;

/**
 * Represents a customized version of the EntityFallingBlock from NMS.
 *
 * @author Pim
 */
public interface IAnimatedBlock
{
    /**
     * Checks if this animated block is currently alive.
     *
     * @return True if this animated block is alive.
     */
    boolean isAlive();

    /**
     * Gets the current position of this animated block. This value is updated after each tick/teleport.
     *
     * @return The current position of this animated block.
     */
    Vector3Dd getCurrentPosition();

    /**
     * Gets the previous position of this animated block. This value is updated after each tick/teleport and descibes
     * the location this animated block was at before it moved.
     *
     * @return The previous position of this animated block.
     */
    Vector3Dd getPreviousPosition();

    /**
     * Gets the current velocity in x/y/z terms for this animated block.
     *
     * @return The current velocity of this animated block.
     */
    Vector3Dd getVelocity();

    /**
     * Gets the world this animated block exists in.
     *
     * @return The world this animated block exists in.
     */
    IPWorld getPWorld();
}
