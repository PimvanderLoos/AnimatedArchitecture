package nl.pim16aap2.bigdoors.core.util;

import nl.pim16aap2.bigdoors.core.util.vector.Vector3Dd;

public interface IGlowingBlock
{
    /**
     * Kills this {@link IGlowingBlock}
     */
    void kill();

    /**
     * Teleports this {@link IGlowingBlock} to the provided position.
     *
     * @param position
     *     The new position of the {@link IGlowingBlock}.
     */
    void teleport(Vector3Dd position);

    /**
     * Retrieves the ID of the entity used as glowing block.
     *
     * @return The entity ID.
     */
    int getEntityId();
}
