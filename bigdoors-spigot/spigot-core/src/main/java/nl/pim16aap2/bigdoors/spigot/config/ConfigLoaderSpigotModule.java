package nl.pim16aap2.bigdoors.spigot.config;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.api.IConfigLoader;

import javax.inject.Singleton;

@Module
public interface ConfigLoaderSpigotModule
{
    @Binds
    @Singleton
    IConfigLoader getConfig(ConfigLoaderSpigot config);
}
