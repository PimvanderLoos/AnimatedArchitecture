package nl.pim16aap2.bigdoors.spigot.core;

import dagger.Module;
import dagger.Provides;
import nl.pim16aap2.bigdoors.core.api.IBlockAnalyzer;
import nl.pim16aap2.bigdoors.core.api.animatedblock.IAnimatedBlockFactory;
import nl.pim16aap2.bigdoors.spigot.util.api.IBigDoorsSpigotSubPlatform;
import nl.pim16aap2.bigdoors.spigot.util.api.IGlowingBlockFactory;

import javax.inject.Singleton;

@Module
public class BigDoorsSpigotSubPlatformModule
{
    @Provides
    @Singleton
    static IGlowingBlockFactory getGlowingBlockFactory(IBigDoorsSpigotSubPlatform spigotPlatform)
    {
        return spigotPlatform.getGlowingBlockFactory();
    }

    @Provides
    @Singleton
    static IAnimatedBlockFactory getAnimatedBlockFactory(IBigDoorsSpigotSubPlatform spigotPlatform)
    {
        return spigotPlatform.getAnimatedBlockFactory();
    }

    @Provides
    @Singleton
    static IBlockAnalyzer getBlockAnalyzer(IBigDoorsSpigotSubPlatform spigotPlatform)
    {
        return spigotPlatform.getBlockAnalyzer();
    }
}
