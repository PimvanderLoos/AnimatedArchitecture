package nl.pim16aap2.animatedarchitecture.spigot.core.config;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.animatedarchitecture.core.api.IConfig;

import javax.inject.Singleton;

@Module
public interface ConfigSpigotModule
{
    @Binds
    @Singleton
    IConfig getConfig(ConfigSpigot config);
}
