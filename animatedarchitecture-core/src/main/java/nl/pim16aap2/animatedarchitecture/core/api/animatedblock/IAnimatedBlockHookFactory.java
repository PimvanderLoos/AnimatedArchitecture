package nl.pim16aap2.animatedarchitecture.core.api.animatedblock;

import nl.pim16aap2.animatedarchitecture.core.managers.AnimatedBlockHookManager;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a factory for {@link IAnimatedBlockHook}s.
 * <p>
 * Instances of these class should be registered with {@link AnimatedBlockHookManager} so that they can be used to
 * inject hooks into the animated blocks.
 *
 * @author Pim
 */
public interface IAnimatedBlockHookFactory<T extends IAnimatedBlock>
{
    /**
     * Creates a new {@link IAnimatedBlockHook}.
     *
     * @param animatedBlock
     *     The {@link IAnimatedBlock} that is being hooked into.
     * @return The new hook.
     */
    @Nullable IAnimatedBlockHook newInstance(T animatedBlock);
}
