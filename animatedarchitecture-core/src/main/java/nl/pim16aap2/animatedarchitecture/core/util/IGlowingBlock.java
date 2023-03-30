package nl.pim16aap2.animatedarchitecture.core.util;

import nl.pim16aap2.animatedarchitecture.core.animation.RotatedPosition;

public interface IGlowingBlock
{
    /**
     * Kills this {@link IGlowingBlock}
     */
    void kill();

    /**
     * Teleports this {@link IGlowingBlock} to the provided position.
     *
     * @param rotatedPosition
     *     The rotated position to teleport to.
     */
    void teleport(RotatedPosition rotatedPosition);
}
