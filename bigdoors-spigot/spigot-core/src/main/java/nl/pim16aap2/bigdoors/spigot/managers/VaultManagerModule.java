package nl.pim16aap2.bigdoors.spigot.managers;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.api.IEconomyManager;
import nl.pim16aap2.bigdoors.api.IPermissionsManager;

import javax.inject.Singleton;

@Module
public interface VaultManagerModule
{
    @Binds
    @Singleton
    IPermissionsManager bindPermissionsManager(VaultManager vaultManager);

    @Binds
    @Singleton
    IEconomyManager bindEconomyManager(VaultManager vaultManager);
}
