package nl.pim16aap2.animatedarchitecture.spigot.core.managers;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import lombok.CustomLog;
import nl.pim16aap2.animatedarchitecture.core.api.IEconomyManager;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.config.IConfig;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import org.bukkit.Bukkit;

import java.util.function.Supplier;

/**
 * Provides the canonical economy manager for the Spigot platform.
 */
@Module
@CustomLog
public final class EconomyManagerModule
{
    private static final String VAULT_PLUGIN_NAME = "Vault";

    private EconomyManagerModule()
    {
    }

    /**
     * Creates the economy manager. Vault economy is used when Vault is present; otherwise economy is disabled.
     *
     * @param config
     *     The plugin configuration.
     * @param structureTypeManager
     *     The manager that exposes enabled structure types.
     * @param debuggableRegistry
     *     The registry for debug output.
     * @param restartableHolder
     *     The holder that manages restartable services.
     * @return The active economy manager.
     */
    @Provides
    @Singleton
    static IEconomyManager provideEconomyManager(
        IConfig config,
        StructureTypeManager structureTypeManager,
        DebuggableRegistry debuggableRegistry,
        RestartableHolder restartableHolder)
    {
        return selectEconomyManager(
            isPluginEnabled(VAULT_PLUGIN_NAME),
            () -> new VaultEconomyManager(config, structureTypeManager, debuggableRegistry, restartableHolder),
            () ->
            {
                log.atInfo().log("Vault is not installed or not enabled; economy integration is disabled.");
                final DisabledEconomyManager manager = new DisabledEconomyManager();
                debuggableRegistry.registerDebuggable(manager);
                return manager;
            }
        );
    }

    static IEconomyManager selectEconomyManager(
        boolean vaultEnabled,
        Supplier<IEconomyManager> vaultEconomyManagerSupplier,
        Supplier<IEconomyManager> disabledEconomyManagerSupplier)
    {
        if (!vaultEnabled)
        {
            return disabledEconomyManagerSupplier.get();
        }

        return vaultEconomyManagerSupplier.get();
    }

    private static boolean isPluginEnabled(String pluginName)
    {
        return Bukkit.getPluginManager().isPluginEnabled(pluginName);
    }
}
