package nl.pim16aap2.bigdoors.spigot.managers;

import dagger.Module;
import dagger.Provides;
import nl.pim16aap2.bigdoors.spigot.util.api.IBigDoorsSpigotSubPlatform;

import javax.inject.Singleton;

@Module
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class PlatformManagerSpigotModule
{
    @Provides
    @Singleton
    static IBigDoorsSpigotSubPlatform provideSpigotPlatform(SubPlatformManager platformManagerSpigot)
    {
        return platformManagerSpigot.getSpigotPlatform();
    }
}
