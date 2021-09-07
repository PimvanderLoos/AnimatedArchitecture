package nl.pim16aap2.bigdoors.spigot;

import dagger.Module;
import dagger.Provides;
import nl.pim16aap2.bigdoors.api.IBlockAnalyzer;
import nl.pim16aap2.bigdoors.api.factories.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IPBlockDataFactory;
import nl.pim16aap2.bigdoors.spigot.util.api.IGlowingBlockFactory;
import nl.pim16aap2.bigdoors.spigot.util.api.ISpigotPlatform;

import javax.inject.Singleton;

@Module
public class BigDoorsSpigotPlatformModule
{
    @Provides
    @Singleton
    static IGlowingBlockFactory getGlowingBlockFactory(ISpigotPlatform spigotPlatform)
    {
        return spigotPlatform.getGlowingBlockFactory();
    }

    @Provides
    @Singleton
    static IPBlockDataFactory getPBlockDataFactory(ISpigotPlatform spigotPlatform)
    {
        return spigotPlatform.getPBlockDataFactory();
    }

    @Provides
    @Singleton
    static IFallingBlockFactory getFallingBlockFactory(ISpigotPlatform spigotPlatform)
    {
        return spigotPlatform.getFallingBlockFactory();
    }

    @Provides
    @Singleton
    static IBlockAnalyzer getBlockAnalyzer(ISpigotPlatform spigotPlatform)
    {
        return spigotPlatform.getBlockAnalyzer();
    }

}
