package nl.pim16aap2.animatedarchitecture.spigot.core.managers;

import lombok.CustomLog;
import net.milkbowl.vault.permission.Permission;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Handles permission checks through Vault when LuckPerms is unavailable.
 */
@CustomLog
public final class VaultPermissionsManager extends AbstractPermissionsManagerSpigot implements IDebuggable
{
    private final IExecutor executor;
    private final Permission permissions;

    /**
     * Creates a Vault permission manager from the registered Vault permission service provider.
     *
     * @param executor
     *     The executor used for async checks and main-thread detection.
     * @param debuggableRegistry
     *     The registry for debug output.
     * @return The Vault permission manager.
     */
    public static VaultPermissionsManager create(IExecutor executor, DebuggableRegistry debuggableRegistry)
    {
        final RegisteredServiceProvider<Permission> permissionProvider =
            Bukkit.getServer().getServicesManager().getRegistration(Permission.class);

        if (permissionProvider == null)
        {
            throw new IllegalStateException("Vault is enabled, but no Vault permission provider is registered.");
        }

        return new VaultPermissionsManager(
            executor,
            debuggableRegistry,
            Objects.requireNonNull(permissionProvider.getProvider(), "Vault permission provider is null.")
        );
    }

    /**
     * Creates a Vault permission manager.
     *
     * @param executor
     *     The executor used for async checks and main-thread detection.
     * @param debuggableRegistry
     *     The registry for debug output.
     * @param permissions
     *     The Vault permission provider.
     */
    public VaultPermissionsManager(
        IExecutor executor,
        DebuggableRegistry debuggableRegistry,
        Permission permissions
    )
    {
        this.executor = executor;
        this.permissions = permissions;

        debuggableRegistry.registerDebuggable(this);
        log.atInfo().log("Using Vault permission backend: %s", permissions.getName());
    }

    @Override
    public boolean hasPermission(Player player, String permission)
    {
        validateSynchronousPlayer(executor, player, permission);
        if (player.isOp())
        {
            return true;
        }

        final boolean result = permissions.playerHas(player.getWorld().getName(), player, permission);
        log.atDebug().log(
            "Vault permission check for player '%s', permission '%s': %b",
            player.getName(),
            permission,
            result
        );
        return result;
    }

    @Override
    public CompletableFuture<Boolean> hasPermissionOffline(
        World world,
        OfflinePlayer player,
        String permission
    )
    {
        return CompletableFuture.supplyAsync(
            () -> permissions.playerHas(world.getName(), player, permission),
            executor.getVirtualExecutor()
        );
    }

    @Override
    public String getDebugInformation()
    {
        return "Permission backend: Vault\n"
            + "Vault permission provider: " + permissions.getName();
    }
}
