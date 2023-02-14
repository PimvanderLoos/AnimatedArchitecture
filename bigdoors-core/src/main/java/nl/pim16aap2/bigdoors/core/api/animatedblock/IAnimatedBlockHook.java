package nl.pim16aap2.bigdoors.core.api.animatedblock;

import nl.pim16aap2.bigdoors.core.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.core.managers.AnimatedBlockHookManager;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Dd;

/**
 * Represents a hook for {@link IAnimatedBlock}s.
 * <p>
 * The animated block hook works similarly to the {@link IAnimationHook}, but for every block in an animation instead of
 * one per animation.
 * <p>
 * Creating a subclass of this interface allows hooking into the animated blocks themselves. Subclasses of this
 * interface can override methods for specific events. For example, overriding the {@link #postTick()} method allows the
 * hook to perform some action after every tick of the animated block. There are also methods for when an animated block
 * (re)spawns, is instantiated, moved, teleported, and more.
 * <p>
 * When creating such a hook, you can register a factory for it with the {@link AnimatedBlockHookManager}. An instance
 * of that manager can be obtained using {@link IBigDoorsPlatform#getAnimatedBlockHookManager()}. Specifically, this
 * class is used to register subclasses of {@link IAnimatedBlockHookFactory}. It is legal for the factory to create a
 * new hook for each animated block, to reuse existing hooks between any number of animated blocks, or to return null,
 * in which case the hook will not be registered for the given animated block.
 * <p>
 * Example usage:
 * <p>
 * <pre>{@code
 * private void registerMyAnimatedBlockHookFactory(IBigDoorsPlatform bigDoorsPlatform) {
 *     bigDoorsPlatform.getAnimatedBlockHookManager().registerFactory(MyAnimatedBlockHook::new);
 * }
 *
 * private static final class MyAnimatedBlockHook implements IAnimatedBlockHook {
 *
 *     private final IAnimatedBlock animatedBlock;
 *
 *     public MyAnimatedBlockHook(IAnimatedBlock animatedBlock) {
 *         this.animatedBlock = animatedBlock;
 *     }
 *
 *     @Override
 *     public String getName() {
 *         return "MyAnimatedBlockHook";
 *     }
 *
 *     @Override
 *     public void postTick() {
 *         // Some code that runs after each tick of the animated block.
 *     }
 * }}</pre>
 * <p>
 * Note that the {@link IAnimatedBlockHookFactory} has a type parameter. The type has to be a subclass of
 * {@link IAnimatedBlock}. This can be used to specialize the type of animated block for the specific platform. For
 * example, animated blocks on the Spigot platform will be subclasses of the IAnimatedBlockSpigot interface, which adds
 * some methods specific to the Spigot platform.
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
