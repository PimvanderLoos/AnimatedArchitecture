package nl.pim16aap2.animatedarchitecture.core.api.animatedblock;

import nl.pim16aap2.animatedarchitecture.core.moveblocks.Animation;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a factory for {@link IAnimationHook}s.
 *
 * @author Pim
 */
public interface IAnimationHookFactory<T extends IAnimatedBlock>
{
    /**
     * Creates a new {@link IAnimationHook}.
     *
     * @param animation
     *     The {@link Animation} that is being hooked into.
     * @return The new hook.
     */
    @Nullable IAnimationHook newInstance(Animation<T> animation);
}
