package nl.pim16aap2.bigdoors.core.api.animatedblock;

import nl.pim16aap2.bigdoors.core.util.vector.Vector3Dd;

/**
 * Represents a hook for {@link IAnimatedBlock}s.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
public interface IAnimatedBlockHook
{
    /**
     * @return The name of this hook to use for logging purposes.
     */
    String getName();

    /**
     * Fires after an animated block has been spawned. Respawning is not counted. See {@link #onRespawn()} for that.
     * <p>
     * This may be called asynchronously.
     */
    default void onSpawn()
    {
    }

    /**
     * Fires after an animated block has been respawned.
     */
    default void onRespawn()
    {
    }

    /**
     * Fires after an animated block has died.
     */
    default void onDie()
    {
    }

    /**
     * Fires after the original block that serves as template for the animated block has been removed from the world.
     */
    default void onDeleteOriginalBlock()
    {
    }

    /**
     * Fires after the block has been placed back into the world when the animation ends.
     */
    default void onBlockPlace()
    {
    }

    /**
     * Fires after an animated block has been teleported.
     * <p>
     * This may be called asynchronously.
     *
     * @param from
     *     The position of the animated block before the teleport.
     * @param to
     *     The position the animated block was teleported to.
     */
    default void onTeleport(Vector3Dd from, Vector3Dd to)
    {
    }

    /**
     * Fires after this animated block has been moved.
     * <p>
     * This may happen either because of a teleport or because of tick-based movement.
     * <p>
     * This may be called asynchronously.
     *
     * @param newPosition
     *     The position the animated block was moved to.
     */
    default void onMoved(Vector3Dd newPosition)
    {
    }

    /**
     * Fires right before a tick is processed.
     */
    default void preTick()
    {
    }

    /**
     * Fires right after a tick is processed.
     */
    default void postTick()
    {
    }
}
