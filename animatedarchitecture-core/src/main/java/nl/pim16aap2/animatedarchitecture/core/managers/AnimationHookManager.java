package nl.pim16aap2.animatedarchitecture.core.managers;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.Animation;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimationHook;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimationHookFactory;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.util.SafeStringBuilder;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents a manager for {@link IAnimationHookFactory} instances.
 * <p>
 * Factories that are registered with this manager will be inserted into every animation.
 */
@Singleton
@Flogger
public final class AnimationHookManager implements IDebuggable
{
    private final List<IAnimationHookFactory<? extends IAnimatedBlock>> factories = new CopyOnWriteArrayList<>();

    @Inject
    AnimationHookManager(DebuggableRegistry debuggableRegistry)
    {
        debuggableRegistry.registerDebuggable(this);
    }

    public void registerFactory(IAnimationHookFactory<? extends IAnimatedBlock> factory)
    {
        this.factories.add(factory);
    }

    public <T extends IAnimatedBlock> List<IAnimationHook> instantiateHooks(Animation<T> animation)
    {
        final ArrayList<IAnimationHook> instantiated = new ArrayList<>(factories.size());

        for (final IAnimationHookFactory<? extends IAnimatedBlock> factory : factories)
        {
            try
            {
                // Casting here should be fine as the platform determines the type
                // of animated block being used (e.g. spigot block on Spigot)
                // and the factories should only be loaded for the specific platform.
                //noinspection unchecked
                final @Nullable IAnimationHook hook = ((IAnimationHookFactory<T>) factory).newInstance(animation);
                if (hook != null)
                    instantiated.add(hook);
            }
            catch (Exception e)
            {
                log.atSevere().withCause(e).log(
                    "Failed to create animation hook with factory '%s'.",
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
        final SafeStringBuilder sb = new SafeStringBuilder("Registered animation hook factories:\n");
        factories.forEach(factory -> sb.append("- ").append(factory.getClass().getName()).append('\n'));
        return sb.toString();
    }
}
