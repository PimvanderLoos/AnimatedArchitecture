package nl.pim16aap2.animatedarchitecture.spigot.core.managers;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedDataManager;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.context.ContextSetFactory;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.platform.PlayerAdapter;
import net.luckperms.api.query.QueryMode;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LuckPermsPermissionsManagerTest
{
    @Mock
    private IExecutor executor;
    @Mock
    private LuckPerms luckPerms;
    @Mock
    private PlayerAdapter<Player> playerAdapter;
    @Mock
    private Player player;
    @Mock
    private World world;
    @Mock
    private UserManager userManager;
    @Mock
    private User user;
    @Mock
    private CachedDataManager cachedDataManager;
    @Mock
    private CachedPermissionData cachedPermissionData;
    @Mock
    private ContextManager contextManager;
    @Mock
    private ContextSetFactory contextSetFactory;
    @Mock
    private ImmutableContextSet.Builder contextSetBuilder;
    @Mock
    private QueryOptions.Builder queryOptionsBuilder;
    @Mock
    private QueryOptions staticQueryOptions;
    @Mock
    private QueryOptions queryOptions;
    @Mock
    private ImmutableContextSet staticContextSet;
    @Mock
    private ImmutableContextSet offlineContextSet;

    private LuckPermsPermissionsManager manager;

    @BeforeEach
    void init()
    {
        // setup
        when(luckPerms.getPlayerAdapter(Player.class)).thenReturn(playerAdapter);

        // execute
        manager = new LuckPermsPermissionsManager(executor, new DebuggableRegistry(), luckPerms);
    }

    @Test
    void hasPermission_shouldUseLuckPermsPlayerAdapterForOnlinePlayer()
    {
        // setup
        when(player.isOp()).thenReturn(false);
        when(player.getName()).thenReturn("Player");
        when(playerAdapter.getPermissionData(player)).thenReturn(cachedPermissionData);
        when(cachedPermissionData.checkPermission("animatedarchitecture.test")).thenReturn(Tristate.TRUE);

        // execute
        final boolean result = manager.hasPermission(player, "animatedarchitecture.test");

        // verify
        assertThat(result).isTrue();
        verify(playerAdapter).getPermissionData(player);
        verify(cachedPermissionData).checkPermission("animatedarchitecture.test");
    }

    @Test
    void hasPermission_shouldPreserveOpBypass()
    {
        // setup
        when(player.isOp()).thenReturn(true);

        // execute
        final boolean result = manager.hasPermission(player, "animatedarchitecture.test");

        // verify
        assertThat(result).isTrue();
    }

    @Test
    void hasPermissionOffline_shouldCompleteWithLuckPermsPermissionResult()
    {
        // setup
        final UUID uuid = UUID.randomUUID();
        final OfflinePlayer offlinePlayer = setupOfflineLuckPermsCheck(uuid, Tristate.FALSE);

        // execute
        final boolean result = manager
            .hasPermissionOffline(world, offlinePlayer, "animatedarchitecture.test")
            .join();

        // verify
        assertThat(result).isFalse();
        verify(userManager).loadUser(uuid, "Player");
        verify(cachedPermissionData).checkPermission("animatedarchitecture.test");
        verify(userManager).cleanupUser(user);
    }

    @Test
    void createOfflineQueryOptions_shouldIncludeWorldContext()
    {
        // setup
        setupQueryOptions("world_nether");

        // execute
        final QueryOptions result = manager.createOfflineQueryOptions(world);

        // verify
        assertThat(result).isSameAs(queryOptions);
        verify(contextSetBuilder).addAll(staticContextSet);
        verify(contextSetBuilder).add("world", "world_nether");
    }

    @Test
    void hasPermissionOffline_shouldPropagateFailedLuckPermsFuture()
    {
        // setup
        final UUID uuid = UUID.randomUUID();
        final OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        final RuntimeException failure = new RuntimeException("load failed");
        when(offlinePlayer.getUniqueId()).thenReturn(uuid);
        when(offlinePlayer.getName()).thenReturn("Player");
        when(luckPerms.getUserManager()).thenReturn(userManager);
        when(userManager.loadUser(uuid, "Player")).thenReturn(CompletableFuture.failedFuture(failure));

        // execute
        final CompletableFuture<Boolean> result =
            manager.hasPermissionOffline(world, offlinePlayer, "animatedarchitecture.test");

        // verify
        assertThatThrownBy(result::join)
            .isInstanceOf(CompletionException.class)
            .hasCause(failure);
    }

    private OfflinePlayer setupOfflineLuckPermsCheck(UUID uuid, Tristate permissionResult)
    {
        final OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        when(offlinePlayer.getUniqueId()).thenReturn(uuid);
        when(offlinePlayer.getName()).thenReturn("Player");
        when(offlinePlayer.isOnline()).thenReturn(false);
        setupQueryOptions("world");
        when(luckPerms.getUserManager()).thenReturn(userManager);
        when(userManager.loadUser(uuid, "Player")).thenReturn(CompletableFuture.completedFuture(user));
        when(user.getCachedData()).thenReturn(cachedDataManager);
        when(cachedDataManager.getPermissionData(queryOptions)).thenReturn(cachedPermissionData);
        when(cachedPermissionData.checkPermission("animatedarchitecture.test")).thenReturn(permissionResult);
        return offlinePlayer;
    }

    private void setupQueryOptions(String worldName)
    {
        when(world.getName()).thenReturn(worldName);
        when(luckPerms.getContextManager()).thenReturn(contextManager);
        when(contextManager.getContextSetFactory()).thenReturn(contextSetFactory);
        when(contextSetFactory.immutableBuilder()).thenReturn(contextSetBuilder);
        when(contextManager.getStaticQueryOptions()).thenReturn(staticQueryOptions);
        when(staticQueryOptions.context()).thenReturn(staticContextSet);
        when(contextSetBuilder.addAll(staticContextSet)).thenReturn(contextSetBuilder);
        when(contextSetBuilder.add("world", worldName)).thenReturn(contextSetBuilder);
        when(contextSetBuilder.build()).thenReturn(offlineContextSet);
        when(contextManager.queryOptionsBuilder(QueryMode.CONTEXTUAL)).thenReturn(queryOptionsBuilder);
        when(queryOptionsBuilder.context(offlineContextSet)).thenReturn(queryOptionsBuilder);
        when(queryOptionsBuilder.build()).thenReturn(queryOptions);
    }
}
