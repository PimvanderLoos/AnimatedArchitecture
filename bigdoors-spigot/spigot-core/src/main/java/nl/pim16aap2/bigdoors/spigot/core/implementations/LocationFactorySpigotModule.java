package nl.pim16aap2.bigdoors.spigot.core.implementations;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.core.api.factories.ILocationFactory;

import javax.inject.Singleton;

@Module
public interface LocationFactorySpigotModule
{
    @Binds
    @Singleton
    ILocationFactory getLocationFactory(LocationFactorySpigot factory);
}
