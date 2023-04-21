package nl.pim16aap2.animatedarchitecture.core.api.animatedblock;

import nl.pim16aap2.animatedarchitecture.core.animation.Animation;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimator;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatform;
import nl.pim16aap2.animatedarchitecture.core.audio.AudioAnimationHook;
import nl.pim16aap2.animatedarchitecture.core.managers.AnimationHookManager;

/**
 * Represents a hook into an {@link Animation}.
 * <p>
 * The animation hook works very similarly to the {@link IAnimatedBlockHook}. The main difference is that instead of
 * hooking into the animated blocks themselves, they hook into the animation.
 * <p>
 * The animation hook allows its hooks to override methods to perform actions on specific events, such as
 * {@link #onAnimationCompleted()} or {@link #onPostAnimationStep()}.
 * <p>
 * To use animation hooks, you need to register a subclass of {@link IAnimationHookFactory} with the
 * {@link AnimationHookManager}. An instance of this class can be obtained using
 * {@link IAnimatedArchitecturePlatform#getAnimationHookManager()}.
 * <p>
 * Example usage:
 * <p>
 * <pre>{@code
 * private void registerMyAnimationHookFactory(IAnimatedArchitecturePlatform animatedArchitecturePlatform)
 * {
 *     animatedArchitecturePlatform.getAnimationHookManager().registerFactory(
 *         animation -> new MyAnimationHook(animation));
 * }
 *
 * private static final class MyAnimationHook implements IAnimationHook<IAnimatedBlock>
 * {
 *     private final Animation<IAnimatedBlock> animation;
 *
 *     MyAnimationHook(Animation<IAnimatedBlock> animation)
 *     {
 *         this.animation = animation;
 *     }
 *
 *     @Override
 *     public String getName()
 *     {
 *         return "MyAnimationHook";
 *     }
 *
 *     @Override
 *     public void onPostAnimationStep()
 *     {
 *         // Some code that is executed after each step of the animation.
 *     }
 * }}</pre>
 * <p>
 * The factory receives an {@link Animation} object and is called when a new animation is started by the
 * {@link IAnimator}. Whether the factory returns a new hook or reuses an existing one is up to the implementation. If
 * the factory returns null, the hook will not be registered for the given animation.
 * <p>
 * You can refer to the animation hook used by the audio system to see a full implementation:
 * {@link AudioAnimationHook}.
 *
 * @author Pim
 */
public interface IAnimationHook
{
    /**
     * @return The name of this hook. Used for logging purposes.
     */
    String getName();

    /**
     * Executed before a new animation step is applied.
     * <p>
     * Note that this may never happen if the animation is skipped.
     */
    default void onPreAnimationStep()
    {
    }

    /**
     * Executed after a new animation step is applied.
     * <p>
     * Note that this may never happen if the animation is skipped.
     */
    default void onPostAnimationStep()
    {
    }

    /**
     * Executed right before the animation starts.
     * <p>
     * Note that this may never happen if the animation is skipped.
     */
    default void onPrepare()
    {
    }

    /**
     * Executed when the animation is in the finishing phase.
     * <p>
     * Note that this may never happen if the animation is skipped.
     */
    default void onAnimationEnding()
    {
    }

    /**
     * Executed when the animation has been aborted.
     * <p>
     * This is not part of the normal animation pipeline. It may occur when the plugin stops, or when the chunks for the
     * animation are being unloaded.
     */
    default void onAnimationAborted()
    {
    }

    /**
     * Executed when the animation finishes.
     */
    default void onAnimationCompleted()
    {
    }
}
