package nl.pim16aap2.animatedarchitecture.spigot.core;

import dagger.Module;
import dagger.Provides;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IAnimatedArchitectureSpigotSubPlatform;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IGlowingBlockFactory;

import javax.inject.Singleton;

@Module
public class AnimatedArchitectureSpigotSubPlatformModule
{
    @Provides
    @Singleton
    static IGlowingBlockFactory getGlowingBlockFactory(IAnimatedArchitectureSpigotSubPlatform spigotPlatform)
    {
        return spigotPlatform.getGlowingBlockFactory();
    }
}
