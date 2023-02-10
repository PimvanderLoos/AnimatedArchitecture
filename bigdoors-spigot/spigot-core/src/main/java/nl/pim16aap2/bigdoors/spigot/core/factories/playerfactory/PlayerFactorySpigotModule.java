package nl.pim16aap2.bigdoors.spigot.core.factories.playerfactory;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.core.api.factories.IPlayerFactory;

import javax.inject.Singleton;

@Module
public interface PlayerFactorySpigotModule
{
    @Binds
    @Singleton
    IPlayerFactory getPlayerFactory(PlayerFactorySpigot factory);
}
