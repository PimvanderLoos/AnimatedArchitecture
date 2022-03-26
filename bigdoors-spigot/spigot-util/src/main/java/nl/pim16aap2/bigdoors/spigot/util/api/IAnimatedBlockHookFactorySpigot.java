package nl.pim16aap2.bigdoors.spigot.util.api;

import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.api.animatedblockhook.IAnimatedBlockHook;
import nl.pim16aap2.bigdoors.api.factories.IAnimatedBlockHookFactory;

/**
 * Represents a specialization of {@link IAnimatedBlockHookFactory} for the Spigot platform.
 *
 * @author Pim
 */
public interface IAnimatedBlockHookFactorySpigot extends IAnimatedBlockHookFactory<IAnimatedBlockSpigot>
{
    /**
     * Creates a new {@link IAnimatedBlockHook} specialized for {@link IAnimatedBlockSpigot}.
     *
     * @param animatedBlock
     *     The {@link IAnimatedBlockSpigot} that is being hooked into.
     * @return The new hook.
     */
    IAnimatedBlockHook<IAnimatedBlockSpigot> newInstance(IAnimatedBlockSpigot animatedBlock);

    @Override
    default IAnimatedBlockHook<IAnimatedBlockSpigot> newInstance(IAnimatedBlock animatedBlock)
    {
        if (animatedBlock instanceof IAnimatedBlockSpigot animatedBlockSpigot)
            return newInstance(animatedBlockSpigot);
        throw new IllegalArgumentException(
            "Trying to instantiate with incorrect type of animated block: " + animatedBlock.getClass().getName());
    }
}
