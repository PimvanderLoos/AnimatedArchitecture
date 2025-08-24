package nl.pim16aap2.animatedarchitecture.spigot.core.implementations;

import dagger.Binds;
import dagger.Module;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;

@Module
public interface TextFactorySpigotModule
{
    @Binds
    @Singleton
    ITextFactory provideTextFactory(TextFactorySpigot textFactorySpigot);
}
