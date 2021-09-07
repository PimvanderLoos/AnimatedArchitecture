package nl.pim16aap2.bigdoors.spigot.factories.pplayerfactory;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;

import javax.inject.Singleton;

@Module
public interface PPlayerFactorySpigotModule
{
    @Binds
    @Singleton
    IPPlayerFactory getPPlayerFactory(PPlayerFactorySpigot factory);
}
