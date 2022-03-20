package nl.pim16aap2.bigdoors.managers;

import nl.pim16aap2.bigdoors.api.IAnimatedBlockHook;
import nl.pim16aap2.bigdoors.api.IAnimatedBlockHookFactory;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Singleton
public final class AnimatedBlockHookManager
{
    private final List<IAnimatedBlockHookFactory<? extends IAnimatedBlock>> factories = new CopyOnWriteArrayList<>();

    @Inject//
    AnimatedBlockHookManager()
    {
    }

    public void registerFactory(IAnimatedBlockHookFactory<? extends IAnimatedBlock> factory)
    {
        this.factories.add(factory);
    }

    public List<IAnimatedBlockHook<IAnimatedBlock>> instantiateHooks(IAnimatedBlock animatedBlock)
    {
        final List<IAnimatedBlockHook<IAnimatedBlock>> instantiated = new ArrayList<>(factories.size());

        for (final IAnimatedBlockHookFactory<? extends IAnimatedBlock> factory : factories)
        {
            final IAnimatedBlockHook<? extends IAnimatedBlock> hook = factory.newInstance(animatedBlock);
            //noinspection unchecked
            final IAnimatedBlockHook<IAnimatedBlock> castHook = (IAnimatedBlockHook<IAnimatedBlock>) hook;
            instantiated.add(castHook);
        }

        return instantiated;
    }
}
