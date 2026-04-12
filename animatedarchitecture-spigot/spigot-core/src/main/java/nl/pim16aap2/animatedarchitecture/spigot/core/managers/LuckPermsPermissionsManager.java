package nl.pim16aap2.animatedarchitecture.spigot.core.managers;

import lombok.CustomLog;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.user.User;
import net.luckperms.api.platform.PlayerAdapter;
import net.luckperms.api.query.QueryMode;
import net.luckperms.api.query.QueryOptions;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.concurrent.CompletableFuture;

/**
 * Handles permission checks through LuckPerms.
 */
@CustomLog
public final class LuckPermsPermissionsManager extends AbstractPermissionsManagerSpigot implements IDebuggable
{
    private final IExecutor executor;
    private final LuckPerms luckPerms;
    private final PlayerAdapter<Player> playerAdapter;

    /**
     * Creates a LuckPerms permission manager from the registered LuckPerms service provider.
     *
     * @param executor
     *     The executor used to detect the server main thread.
     * @param debuggableRegistry
     *     The registry for debug output.
     * @return The LuckPerms permission manager.
     */
    public static LuckPermsPermissionsManager create(
        IExecutor executor,
        DebuggableRegistry debuggableRegistry
    )
    {
        final RegisteredServiceProvider<LuckPerms> provider =
            Bukkit.getServicesManager().getRegistration(LuckPerms.class);

        if (provider == null)
        {
            throw new IllegalStateException("LuckPerms is enabled, but no LuckPerms API service is registered.");
        }

        return new LuckPermsPermissionsManager(executor, debuggableRegistry, provider.getProvider());
    }

    /**
     * Creates a LuckPerms permission manager.
     *
     * @param executor
     *     The executor used to detect the server main thread.
     * @param debuggableRegistry
     *     The registry for debug output.
     * @param luckPerms
     *     The LuckPerms API instance.
     */
    public LuckPermsPermissionsManager(
        IExecutor executor,
        DebuggableRegistry debuggableRegistry,
        LuckPerms luckPerms
    )
    {
        this.executor = executor;
        this.luckPerms = luckPerms;
        this.playerAdapter = luckPerms.getPlayerAdapter(Player.class);

        debuggableRegistry.registerDebuggable(this);
        log.atInfo().log("Using LuckPerms permission backend.");
    }

    @Override
    public boolean hasPermission(
        Player player,
        String permission
    )
    {
        validateSynchronousPlayer(executor, player, permission);
        if (player.isOp())
        {
            return true;
        }

        final boolean result = playerAdapter
            .getPermissionData(player)
            .checkPermission(permission)
            .asBoolean();

        log.atDebug().log(
            "LuckPerms permission check for player '%s', permission '%s': %b",
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
        return luckPerms
            .getUserManager()
            .loadUser(player.getUniqueId(), player.getName())
            .thenApply(user -> checkOfflinePermission(world, player, user, permission))
            .whenComplete((result, throwable) ->
            {
                if (throwable != null)
                {
                    log.atWarn().withCause(throwable).log(
                        "Failed to complete LuckPerms offline permission check for player '%s', permission '%s'.",
                        player.getUniqueId(),
                        permission
                    );
                }
            });
    }

    /**
     * Checks a loaded LuckPerms user against static server context plus the target Bukkit world.
     *
     * @param world
     *     The target world.
     * @param player
     *     The target player.
     * @param user
     *     The loaded LuckPerms user.
     * @param permission
     *     The permission to check.
     * @return True if the user has the permission.
     */
    private boolean checkOfflinePermission(
        World world,
        OfflinePlayer player,
        User user,
        String permission
    )
    {
        try
        {
            final QueryOptions queryOptions = createOfflineQueryOptions(world);
            final CachedPermissionData permissionData = user.getCachedData().getPermissionData(queryOptions);
            final boolean result = permissionData.checkPermission(permission).asBoolean();

            log.atDebug().log(
                "LuckPerms offline permission check for player '%s', world '%s', permission '%s': %b",
                player.getUniqueId(),
                world.getName(),
                permission,
                result
            );
            return result;
        }
        finally
        {
            if (!player.isOnline())
            {
                luckPerms.getUserManager().cleanupUser(user);
            }
        }
    }

    /**
     * Creates query options for offline permission checks.
     *
     * @param world
     *     The target Bukkit world.
     * @return The static LuckPerms query options plus a world context.
     */
    QueryOptions createOfflineQueryOptions(World world)
    {
        final ContextManager contextManager = luckPerms.getContextManager();

        final ImmutableContextSet context = ImmutableContextSet.builder()
            .addAll(contextManager.getStaticQueryOptions().context())
            .add(DefaultContextKeys.WORLD_KEY, world.getName())
            .build();

        return contextManager
            .queryOptionsBuilder(QueryMode.CONTEXTUAL)
            .context(context)
            .build();
    }

    @Override
    public String getDebugInformation()
    {
        return "Permission backend: LuckPerms\n"
            + "LuckPerms server name: " + luckPerms.getServerName() + "\n"
            + "LuckPerms plugin info: " + luckPerms.getPluginMetadata();
    }
}
