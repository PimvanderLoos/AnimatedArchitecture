package nl.pim16aap2.animatedarchitecture.spigot.core.implementations;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;

import javax.inject.Singleton;

@Module
public interface TextFactorySpigotModule
{
    @Binds
    @Singleton
    ITextFactory provideTextFactory(TextFactorySpigot textFactorySpigot);
}
