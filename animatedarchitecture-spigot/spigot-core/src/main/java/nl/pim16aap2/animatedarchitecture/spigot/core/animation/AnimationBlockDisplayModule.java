package nl.pim16aap2.animatedarchitecture.spigot.core.animation;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.animatedarchitecture.core.api.IBlockAnalyzer;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockFactory;

import javax.inject.Singleton;

@Module
public interface AnimationBlockDisplayModule
{
    @Binds
    @Singleton
    IAnimatedBlockFactory getAnimatedBlockFactory(AnimatedBlockDisplayFactory factory);

    @Binds
    @Singleton
    IBlockAnalyzer getBlockAnalyzer(BlockAnalyzer blockAnalyzer);
}
