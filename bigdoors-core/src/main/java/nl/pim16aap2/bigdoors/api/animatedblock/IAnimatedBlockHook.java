package nl.pim16aap2.bigdoors.api.animatedblock;

import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;

/**
 * Represents a hook for {@link IAnimatedBlock}s.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
public interface IAnimatedBlockHook<T extends IAnimatedBlock>
{
    /**
     * @return The name of this hook to use for logging purposes.
     */
    String getName();

    /**
     * Fires after an animated block has been spawned.
     *
     * @param animatedBlock
     *     The animated block that was spawned into a world.
     */
    default void onSpawn(T animatedBlock)
    {
    }

    /**
     * Fires after an animated block has died.
     *
     * @param animatedBlock
     *     The animated block that died.
     */
    default void onDie(T animatedBlock)
    {
    }

    /**
     * Fires after an animated block has been teleported.
     *
     * @param animatedBlock
     *     The animated block that was teleported.
     * @param newPosition
     *     The position the animated block was teleported to.
     */
    default void onTeleport(T animatedBlock, Vector3Dd newPosition)
    {
    }

    /**
     * Fires after this animated block has been moved.
     * <p>
     * This may happen either because of a teleport or because of tick-based movement.
     *
     * @param animatedBlock
     *     The animated block that was moved.
     * @param newPosition
     *     The position the animated block was moved to.
     */
    default void onMoved(T animatedBlock, Vector3Dd newPosition)
    {
    }

    /**
     * Fires right before a tick is processed.
     *
     * @param animatedBlock
     *     The animated block that is processing a tick.
     */
    default void preTick(T animatedBlock)
    {
    }

    /**
     * Fires right after a tick is processed.
     *
     * @param animatedBlock
     *     The animated block that is processing a tick.
     */
    default void postTick(T animatedBlock)
    {
    }
}
