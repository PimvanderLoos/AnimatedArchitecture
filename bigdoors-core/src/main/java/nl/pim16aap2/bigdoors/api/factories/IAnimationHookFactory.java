package nl.pim16aap2.bigdoors.api.factories;

import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimationHook;
import nl.pim16aap2.bigdoors.moveblocks.IAnimationProgress;
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
     *     The {@link IAnimationProgress} that is being hooked into.
     * @return The new hook.
     */
    @Nullable IAnimationHook<T> newInstance(IAnimationProgress<T> animation);
}
