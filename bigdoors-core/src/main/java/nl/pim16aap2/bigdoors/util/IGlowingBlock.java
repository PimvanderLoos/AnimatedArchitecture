package nl.pim16aap2.bigdoors.util;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.PColor;
import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.util.vector.Vector3DdConst;

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
    void teleport(final @NonNull Vector3DdConst position);

    /**
     * Spawns this {@link IGlowingBlock} for the specified number of ticks.
     *
     * @param pColor The color of the outline of the block.
     * @param x      The x-coordinate of the block.
     * @param y      The y-coordiante of the block.
     * @param z      The z-coordiante of the block.
     * @param ticks  The number of ticks to keep this entity visible for the player.
     */
    void spawn(final @NonNull PColor pColor, final double x, final double y, final double z, final long ticks);
}
