package nl.pim16aap2.bigdoors.managers;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.animatedblockhook.IAnimatedBlockHook;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.api.debugging.IDebuggable;
import nl.pim16aap2.bigdoors.api.factories.IAnimatedBlockHookFactory;
import nl.pim16aap2.util.SafeStringBuilder;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

@Singleton
@Flogger
public final class AnimatedBlockHookManager implements IDebuggable
{
    private final List<IAnimatedBlockHookFactory<? extends IAnimatedBlock>> factories = new CopyOnWriteArrayList<>();

    @Inject//
    AnimatedBlockHookManager(DebuggableRegistry debuggableRegistry)
    {
        debuggableRegistry.registerDebuggable(this);
    }

    public void registerFactory(IAnimatedBlockHookFactory<? extends IAnimatedBlock> factory)
    {
        this.factories.add(factory);
    }

    public <T extends IAnimatedBlock> List<IAnimatedBlockHook<T>> instantiateHooks(T animatedBlock)
    {
        final List<IAnimatedBlockHook<T>> instantiated = new ArrayList<>(factories.size());

        for (final IAnimatedBlockHookFactory<? extends IAnimatedBlock> factory : factories)
        {
            try
            {
                //noinspection unchecked
                instantiated.add(((IAnimatedBlockHookFactory<T>) factory).newInstance(animatedBlock));
            }
            catch (Exception e)
            {
                log.at(Level.SEVERE).withCause(e).log("Failed to create hook with factory '%s'.",
                                                      factory.getClass().getName());
            }
        }

        return instantiated;
    }

    @Override
    public String getDebugInformation()
    {
        final SafeStringBuilder sb = new SafeStringBuilder("Registered animated block hook factories:\n");
        factories.forEach(factory -> sb.append("- ").append(factory.getClass().getName()).append('\n'));
        return sb.toString();
    }
}
