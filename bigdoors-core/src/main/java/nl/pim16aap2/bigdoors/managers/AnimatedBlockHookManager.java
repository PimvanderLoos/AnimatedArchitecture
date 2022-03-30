package nl.pim16aap2.bigdoors.managers;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlockHook;
import nl.pim16aap2.bigdoors.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.api.debugging.IDebuggable;
import nl.pim16aap2.bigdoors.api.factories.IAnimatedBlockHookFactory;
import nl.pim16aap2.util.SafeStringBuilder;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

/**
 * Represents a manager for {@link IAnimatedBlockHookFactory} instances.
 * <p>
 * Factories that are registered with this manager will be inserted into every animated block separately.
 *
 * @author Pim
 */
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

    public <T extends IAnimatedBlock> List<IAnimatedBlockHook> instantiateHooks(T animatedBlock)
    {
        final List<IAnimatedBlockHook> instantiated = new ArrayList<>(factories.size());

        for (final IAnimatedBlockHookFactory<? extends IAnimatedBlock> factory : factories)
        {
            try
            {
                //noinspection unchecked
                final @Nullable IAnimatedBlockHook hook =
                    ((IAnimatedBlockHookFactory<T>) factory).newInstance(animatedBlock);
                if (hook != null)
                    instantiated.add(hook);
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
