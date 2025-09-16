package nl.pim16aap2.animatedarchitecture.spigot.core.implementations;

import dagger.Binds;
import dagger.Module;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IWorldFactory;

@Module
public interface WorldFactorySpigotModule
{
    @Binds
    @Singleton
    IWorldFactory getWorldFactory(WorldFactorySpigot factory);
}
