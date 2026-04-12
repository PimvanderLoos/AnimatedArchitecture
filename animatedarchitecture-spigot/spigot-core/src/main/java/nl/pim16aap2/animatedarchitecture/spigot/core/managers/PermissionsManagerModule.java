package nl.pim16aap2.animatedarchitecture.spigot.core.managers;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPermissionsManager;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IPermissionsManagerSpigot;
import org.bukkit.Bukkit;

import java.util.function.Supplier;

/**
 * Provides the canonical permission manager for the Spigot platform.
 */
@Module
public final class PermissionsManagerModule
{
    private static final String LUCKPERMS_PLUGIN_NAME = "LuckPerms";
    private static final String VAULT_PLUGIN_NAME = "Vault";

    private PermissionsManagerModule()
    {
    }

    /**
     * Selects the active permission backend. LuckPerms is preferred over Vault when both are present.
     *
     * @param executor
     *     The executor used by permission backends.
     * @param debuggableRegistry
     *     The registry for debug output.
     * @return The active Spigot permission manager.
     */
    @Provides
    @Singleton
    static IPermissionsManagerSpigot providePermissionsManagerSpigot(
        IExecutor executor,
        DebuggableRegistry debuggableRegistry)
    {
        return selectPermissionsManager(
            isPluginEnabled(LUCKPERMS_PLUGIN_NAME),
            () -> LuckPermsPermissionsManager.create(executor, debuggableRegistry),
            isPluginEnabled(VAULT_PLUGIN_NAME),
            () -> VaultPermissionsManager.create(executor, debuggableRegistry)
        );
    }

    static IPermissionsManagerSpigot selectPermissionsManager(
        boolean luckPermsEnabled,
        Supplier<IPermissionsManagerSpigot> luckPermsSupplier,
        boolean vaultEnabled,
        Supplier<IPermissionsManagerSpigot> vaultSupplier)
    {
        if (luckPermsEnabled)
            return luckPermsSupplier.get();

        if (vaultEnabled)
            return vaultSupplier.get();

        throw new IllegalStateException(
            "Failed to initialize permissions! Install LuckPerms or Vault with a permission provider."
        );
    }

    /**
     * Exposes the selected Spigot permission manager through the platform-independent permission interface.
     *
     * @param permissionsManager
     *     The selected Spigot permission manager.
     * @return The platform-independent permission manager binding.
     */
    @Provides
    @Singleton
    static IPermissionsManager providePermissionsManager(IPermissionsManagerSpigot permissionsManager)
    {
        return permissionsManager;
    }

    private static boolean isPluginEnabled(String pluginName)
    {
        return Bukkit.getPluginManager().isPluginEnabled(pluginName);
    }
}
