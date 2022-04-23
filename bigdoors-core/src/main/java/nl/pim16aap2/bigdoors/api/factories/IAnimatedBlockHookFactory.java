package nl.pim16aap2.bigdoors.api.factories;

import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlockHook;
import nl.pim16aap2.bigdoors.managers.AnimatedBlockHookManager;
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
