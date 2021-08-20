package nl.pim16aap2.bigdoors.util;

import nl.pim16aap2.bigdoors.api.PColor;
import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;

public interface IGlowingBlock extends IRestartable
{
    /**
     * Kills this {@link IGlowingBlock}
     */
    void kill();

    /**
     * Teleports this {@link IGlowingBlock} to the provided position.
     *
     * @param position The new position of the {@link IGlowingBlock}.
     */
    void teleport(Vector3Dd position);

    /**
     * Spawns this {@link IGlowingBlock} for the specified number of ticks.
     *
     * @param pColor The color of the outline of the block.
     * @param x      The x-coordinate of the block.
     * @param y      The y-coordiante of the block.
     * @param z      The z-coordiante of the block.
     * @param ticks  The number of ticks to keep this entity visible for the player.
     */
    void spawn(PColor pColor, double x, double y, double z, long ticks);
}
