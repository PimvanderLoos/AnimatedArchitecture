package nl.pim16aap2.animatedarchitecture.spigot.core.config;

import dagger.Binds;
import dagger.Module;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.core.config.IConfig;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IBlockAnalyzerConfig;

@Module
public interface ConfigSpigotModule
{
    @Binds
    @Singleton
    IConfig getConfig(ConfigSpigot config);

    @Binds
    @Singleton
    IBlockAnalyzerConfig getBlockAnalyzerConfig(ConfigSpigot config);
}
