package nl.pim16aap2.bigdoors.spigot.managers;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import nl.pim16aap2.bigdoors.spigot.util.api.IPlatformManagerSpigot;
import nl.pim16aap2.bigdoors.spigot.util.api.ISpigotPlatform;

import javax.inject.Singleton;

@Module
public abstract class PlatformManagerSpigotModule
{
    @Provides
    @Singleton
    static ISpigotPlatform provideSpigotPlatform(PlatformManagerSpigot platformManagerSpigot)
    {
        return platformManagerSpigot.getSpigotPlatform();
    }

    @Binds
    @Singleton
    abstract IPlatformManagerSpigot providePlatformManagerSpigot(PlatformManagerSpigot manager);
}
