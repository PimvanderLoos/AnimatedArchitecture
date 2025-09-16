package nl.pim16aap2.animatedarchitecture.spigot.core.implementations;

import dagger.Binds;
import dagger.Module;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IPlayerFactory;

@Module
public interface PlayerFactorySpigotModule
{
    @Binds
    @Singleton
    IPlayerFactory getPlayerFactory(PlayerFactorySpigot factory);
}
