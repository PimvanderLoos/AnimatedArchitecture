package nl.pim16aap2.bigdoors.spigot.core.config;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.core.api.IConfig;

import javax.inject.Singleton;

@Module
public interface ConfigSpigotModule
{
    @Binds
    @Singleton
    IConfig getConfig(ConfigSpigot config);
}
