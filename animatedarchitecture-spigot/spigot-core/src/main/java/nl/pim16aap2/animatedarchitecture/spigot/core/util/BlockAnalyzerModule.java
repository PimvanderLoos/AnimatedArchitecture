package nl.pim16aap2.animatedarchitecture.spigot.core.util;

import dagger.Module;
import dagger.Provides;
import nl.pim16aap2.animatedarchitecture.core.api.IBlockAnalyzer;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IBlockAnalyzerSpigot;

import javax.inject.Singleton;

@Module
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class BlockAnalyzerModule
{
    @Provides
    @Singleton
    static IBlockAnalyzerSpigot getBlockAnalyzerSpigot(SpigotSubPlatformProvider provider)
    {
        return provider.getSubPlatform().getBlockAnalyzer();
    }

    @Provides
    @Singleton
    static IBlockAnalyzer<?> getBlockAnalyzer(SpigotSubPlatformProvider provider)
    {
        return provider.getSubPlatform().getBlockAnalyzer();
    }
}
