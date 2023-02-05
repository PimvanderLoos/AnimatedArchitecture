package nl.pim16aap2.bigdoors.spigot.core.factories.plocationfactory;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.core.api.factories.IPLocationFactory;

import javax.inject.Singleton;

@Module
public interface PLocationFactorySpigotModule
{
    @Binds
    @Singleton
    IPLocationFactory getPLocationFactory(PLocationFactorySpigot factory);
}
