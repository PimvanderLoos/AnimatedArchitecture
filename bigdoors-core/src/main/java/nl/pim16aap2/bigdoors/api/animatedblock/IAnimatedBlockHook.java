package nl.pim16aap2.bigdoors.api.animatedblock;

import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;

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
     * Fires after an animated block has been teleported.
     *
     * @param newPosition
     *     The position the animated block was teleported to.
     */
    default void onTeleport(Vector3Dd newPosition)
    {
    }

    /**
     * Fires after this animated block has been moved.
     * <p>
     * This may happen either because of a teleport or because of tick-based movement.
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
