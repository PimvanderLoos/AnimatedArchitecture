package nl.pim16aap2.animatedarchitecture.spigot.core.implementations;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IWorldFactory;

import javax.inject.Singleton;

@Module
public interface WorldFactorySpigotModule
{
    @Binds
    @Singleton
    IWorldFactory getWorldFactory(WorldFactorySpigot factory);
}
