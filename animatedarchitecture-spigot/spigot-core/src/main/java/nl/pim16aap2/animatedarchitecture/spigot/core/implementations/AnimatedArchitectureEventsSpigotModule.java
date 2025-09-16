package nl.pim16aap2.animatedarchitecture.spigot.core.implementations;

import dagger.Binds;
import dagger.Module;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IAnimatedArchitectureEventFactory;
import nl.pim16aap2.animatedarchitecture.core.events.IAnimatedArchitectureEventCaller;
import nl.pim16aap2.animatedarchitecture.spigot.core.events.AnimatedArchitectureEventCallerSpigot;

@Module
public interface AnimatedArchitectureEventsSpigotModule
{
    @Binds
    @Singleton
    IAnimatedArchitectureEventFactory getAnimatedArchitectureEventFactory(
        AnimatedArchitectureEventFactorySpigot factory
    );

    @Binds
    @Singleton
    IAnimatedArchitectureEventCaller getStructureEventCaller(AnimatedArchitectureEventCallerSpigot caller);
}
