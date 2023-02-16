package nl.pim16aap2.animatedarchitecture.core.util;

import nl.pim16aap2.animatedarchitecture.core.moveblocks.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;

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

    /**
     * Teleports this {@link IGlowingBlock} to the provided position.
     *
     * @param position
     *     The new position of the {@link IGlowingBlock}.
     */
    default void teleport(Vector3Dd position)
    {
        teleport(new RotatedPosition(position));
    }

    /**
     * Retrieves the ID of the entity used as glowing block.
     *
     * @return The entity ID.
     */
    int getEntityId();
}
