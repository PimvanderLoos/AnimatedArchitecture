package nl.pim16aap2.animatedarchitecture.spigot.core.implementations;

import dagger.Binds;
import dagger.Module;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ILocationFactory;

@Module
public interface LocationFactorySpigotModule
{
    @Binds
    @Singleton
    ILocationFactory getLocationFactory(LocationFactorySpigot factory);
}
