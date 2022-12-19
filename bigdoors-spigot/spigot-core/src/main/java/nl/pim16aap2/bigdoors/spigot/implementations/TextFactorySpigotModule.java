package nl.pim16aap2.bigdoors.spigot.implementations;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;

import javax.inject.Singleton;

@Module
public interface TextFactorySpigotModule
{
    @Binds
    @Singleton
    ITextFactory provideTextFactory(TextFactorySpigot textFactorySpigot);
}
