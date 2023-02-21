package nl.pim16aap2.animatedarchitecture.spigot.core.managers;

import dagger.Module;
import dagger.Provides;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IAnimatedArchitectureSpigotSubPlatform;

import javax.inject.Singleton;

@Module
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class PlatformManagerSpigotModule
{
    @Provides
    @Singleton
    static IAnimatedArchitectureSpigotSubPlatform provideSpigotPlatform(SubPlatformManager platformManagerSpigot)
    {
        return platformManagerSpigot.getSpigotPlatform();
    }
}
