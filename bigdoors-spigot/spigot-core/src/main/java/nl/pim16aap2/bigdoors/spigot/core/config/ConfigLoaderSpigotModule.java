package nl.pim16aap2.bigdoors.spigot.core.config;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.core.api.IConfigLoader;

import javax.inject.Singleton;

@Module
public interface ConfigLoaderSpigotModule
{
    @Binds
    @Singleton
    IConfigLoader getConfig(ConfigLoaderSpigot config);
}
