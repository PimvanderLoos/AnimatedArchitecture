package nl.pim16aap2.bigdoors.spigot.managers;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import nl.pim16aap2.bigdoors.spigot.util.api.IBigDoorsSpigotSubPlatform;
import nl.pim16aap2.bigdoors.spigot.util.api.ISubPlatformManagerSpigot;

import javax.inject.Singleton;

@Module
public abstract class PlatformManagerSpigotModule
{
    @Provides
    @Singleton
    static IBigDoorsSpigotSubPlatform provideSpigotPlatform(SubPlatformManagerSpigot platformManagerSpigot)
    {
        return platformManagerSpigot.getSpigotPlatform();
    }

    @Binds
    @Singleton
    abstract ISubPlatformManagerSpigot providePlatformManagerSpigot(SubPlatformManagerSpigot manager);
}
