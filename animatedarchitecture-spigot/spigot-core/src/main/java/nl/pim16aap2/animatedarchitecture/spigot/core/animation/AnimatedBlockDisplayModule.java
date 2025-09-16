package nl.pim16aap2.animatedarchitecture.spigot.core.animation;

import dagger.Binds;
import dagger.Module;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockFactory;

@Module
public interface AnimatedBlockDisplayModule
{
    @Binds
    @Singleton
    IAnimatedBlockFactory getAnimatedBlockFactory(AnimatedBlockDisplayFactory factory);
}
