package nl.pim16aap2.bigdoors.spigot.managers;

import dagger.Module;
import dagger.Provides;
import nl.pim16aap2.bigdoors.spigot.util.api.ISpigotPlatform;

import javax.inject.Singleton;

@Module
public class PlatformManagerSpigotModule
{
    @Provides
    @Singleton
    static ISpigotPlatform provideSpigotPlatform(PlatformManagerSpigot platformManagerSpigot)
    {
        return platformManagerSpigot.getSpigotPlatform();
    }
}
