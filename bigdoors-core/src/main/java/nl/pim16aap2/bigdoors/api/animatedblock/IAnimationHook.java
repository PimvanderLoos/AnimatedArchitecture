package nl.pim16aap2.bigdoors.api.animatedblock;

import nl.pim16aap2.bigdoors.api.factories.IAnimationHookFactory;
import nl.pim16aap2.bigdoors.managers.AnimationHookManager;
import nl.pim16aap2.bigdoors.moveblocks.IAnimationProgress;

/**
 * Represents a hook into an {@link IAnimationProgress}.
 * <p>
 * To hook into an animation, this hook needs to be registered with {@link AnimationHookManager} via the use of an
 * {@link IAnimationHookFactory}.
 *
 * @param <T>
 *     The type of {@link IAnimatedBlock} used for the animation.
 * @author Pim
 */
public interface IAnimationHook<T extends IAnimatedBlock>
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
     * Executed when the animation finishes.
     */
    default void onAnimationCompleted()
    {
    }
}
