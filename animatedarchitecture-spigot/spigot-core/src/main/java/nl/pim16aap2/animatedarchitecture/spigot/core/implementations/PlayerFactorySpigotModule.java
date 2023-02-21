package nl.pim16aap2.animatedarchitecture.spigot.core.implementations;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IPlayerFactory;

import javax.inject.Singleton;

@Module
public interface PlayerFactorySpigotModule
{
    @Binds
    @Singleton
    IPlayerFactory getPlayerFactory(PlayerFactorySpigot factory);
}
