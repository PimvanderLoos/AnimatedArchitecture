package nl.pim16aap2.animatedarchitecture.spigot.core.managers;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.animatedarchitecture.core.api.IEconomyManager;
import nl.pim16aap2.animatedarchitecture.core.api.IPermissionsManager;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IPermissionsManagerSpigot;

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

    @Binds
    @Singleton
    IPermissionsManagerSpigot getPermissionsManagerSpigot(VaultManager vaultManager);
}
