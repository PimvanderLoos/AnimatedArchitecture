package nl.pim16aap2.animatedarchitecture.core.api.animatedblock;

import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatform;
import nl.pim16aap2.animatedarchitecture.core.managers.AnimatedBlockHookManager;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.RotatedPosition;

/**
 * Represents a hook for {@link IAnimatedBlock}s.
 * <p>
 * The animated block hook works similarly to the {@link IAnimationHook}, but for every block in an animation instead of
 * one per animation.
 * <p>
 * Creating a subclass of this interface allows hooking into the animated blocks themselves. Subclasses of this
 * interface can override methods for specific events. For example, overriding the {@link #preMove(RotatedPosition)}
 * method allows the hook to perform some action every time the animated block is about to be moved. There are also
 * methods for when an animated block (re)spawns, killing, block manipulations, etc.
 * <p>
 * When creating such a hook, you can register a factory for it with the {@link AnimatedBlockHookManager}. An instance
 * of that manager can be obtained using {@link IAnimatedArchitecturePlatform#getAnimatedBlockHookManager()}.
 * Specifically, this class is used to register subclasses of {@link IAnimatedBlockHookFactory}. It is legal for the
 * factory to create a new hook for each animated block, to reuse existing hooks between any number of animated blocks,
 * or to return null, in which case the hook will not be registered for the given animated block.
 * <p>
 * Example usage:
 * <p>
 * <pre>{@code
 * private void registerMyAnimatedBlockHookFactory(IAnimatedArchitecturePlatform animatedArchitecturePlatform) {
 *     animatedArchitecturePlatform.getAnimatedBlockHookManager().registerFactory(MyAnimatedBlockHook::new);
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
 *     public void preSpawn() {
 *         // Some code that runs before the animated block is spawned.
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
     * Fires right before an animated block will be spawned. Respawning is not counted. Use {@link #preRespawn()} for
     * that.
     * <p>
     * This is always called on the main thread.
     */
    default void preSpawn()
    {
    }

    /**
     * Fires after an animated block has been spawned. Respawning is not counted. Use {@link #postRespawn()} for that.
     * <p>
     * This is always called on the main thread.
     */
    default void postSpawn()
    {
    }

    /**
     * Fires right before an animated block will be respawned.
     * <p>
     * This is always called on the main thread.
     */
    default void preRespawn()
    {
    }

    /**
     * Fires after an animated block has been respawned.
     * <p>
     * This is always called on the main thread.
     */
    default void postRespawn()
    {
    }

    /**
     * Fires right before an animated block will be killed.
     * <p>
     * Respawning does not count.
     * <p>
     * This is always called on the main thread.
     */
    default void preKill()
    {
    }

    /**
     * Fires after an animated block was killed.
     * <p>
     * Respawning does not count.
     * <p>
     * This is always called on the main thread.
     */
    default void postKill()
    {
    }

    /**
     * Fires right before the original block that serves as template for the animated block will be removed from the
     * world.
     * <p>
     * This is always called on the main thread.
     */
    default void preDeleteOriginalBlock()
    {
    }

    /**
     * Fires after the original block that serves as template for the animated block has been removed from the world.
     * <p>
     * This is always called on the main thread.
     */
    default void postDeleteOriginalBlock()
    {
    }

    /**
     * Fires right before the block will be placed back into the world when the animation ends.
     * <p>
     * This is always called on the main thread.
     */
    default void preBlockPlace()
    {
    }

    /**
     * Fires after the block has been placed back into the world when the animation ends.
     * <p>
     * This is always called on the main thread.
     */
    default void postBlockPlace()
    {
    }

    /**
     * Fires after this animated block has been moved.
     * <p>
     * This may be called asynchronously.
     *
     * @param newPosition
     *     The position the animated block was moved to.
     */
    default void postMove(RotatedPosition newPosition)
    {
    }

    /**
     * Fires before this animated block will be moved.
     * <p>
     * This may be called asynchronously.
     *
     * @param newPosition
     *     The position the animated block will move to.
     */
    default void preMove(RotatedPosition newPosition)
    {
    }
}
