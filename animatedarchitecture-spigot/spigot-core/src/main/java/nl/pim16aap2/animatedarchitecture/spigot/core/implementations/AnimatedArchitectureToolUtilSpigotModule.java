package nl.pim16aap2.animatedarchitecture.spigot.core.implementations;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitectureToolUtil;

import javax.inject.Singleton;

@Module
public interface AnimatedArchitectureToolUtilSpigotModule
{
    @Binds
    @Singleton
    IAnimatedArchitectureToolUtil provideAnimatedArchitectureUtil(
        AnimatedArchitectureToolUtilSpigot animatedArchitectureToolUtilSpigot);
}
