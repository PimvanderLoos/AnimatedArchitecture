package nl.pim16aap2.animatedarchitecture.core.managers;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockHook;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockHookFactory;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.util.StringUtil;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents a manager for {@link IAnimatedBlockHookFactory} instances.
 * <p>
 * Factories that are registered with this manager will be inserted into every animated block separately.
 */
@Singleton
@Flogger
public final class AnimatedBlockHookManager implements IDebuggable
{
    private final List<IAnimatedBlockHookFactory<? extends IAnimatedBlock>> factories = new CopyOnWriteArrayList<>();

    @Inject
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
        final ArrayList<IAnimatedBlockHook> instantiated = new ArrayList<>(factories.size());

        for (final IAnimatedBlockHookFactory<? extends IAnimatedBlock> factory : factories)
        {
            try
            {
                // Casting here should be fine as the platform determines the type
                // of animated block being used (e.g. spigot block on Spigot)
                // and the factories should only be loaded for the specific platform.
                //noinspection unchecked
                final @Nullable IAnimatedBlockHook hook =
                    ((IAnimatedBlockHookFactory<T>) factory).newInstance(animatedBlock);
                if (hook != null)
                    instantiated.add(hook);
            }
            catch (Exception e)
            {
                log.atSevere().withCause(e).log(
                    "Failed to create animated block hook with factory '%s'.",
                    factory.getClass().getName()
                );
            }
        }
        instantiated.trimToSize();
        return instantiated;
    }

    @Override
    public String getDebugInformation()
    {
        return "Registered animated block hook factories: " +
            StringUtil.formatCollection(factories, factory -> factory.getClass().getName());
    }
}
