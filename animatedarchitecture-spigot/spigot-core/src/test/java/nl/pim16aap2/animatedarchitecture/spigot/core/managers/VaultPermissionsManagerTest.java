package nl.pim16aap2.animatedarchitecture.spigot.core.managers;

import net.milkbowl.vault.permission.Permission;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VaultPermissionsManagerTest
{
    @Mock
    private IExecutor executor;
    @Mock
    private Permission permissions;
    @Mock
    private Player player;
    @Mock
    private OfflinePlayer offlinePlayer;
    @Mock
    private World world;

    private VaultPermissionsManager manager;

    @AfterEach
    void afterEach()
    {
        verifyNoMoreInteractions(
            executor,
            permissions
        );
    }

    @BeforeEach
    void init()
    {
        when(permissions.getName()).thenReturn("VaultPerms");

        manager = new VaultPermissionsManager(executor, new DebuggableRegistry(), permissions);
    }

    @Test
    void hasPermission_shouldCallVaultPlayerHasForOnlinePlayer()
    {
        // setup
        when(player.isOp()).thenReturn(false);
        when(player.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(permissions.playerHas("world", player, "animatedarchitecture.test")).thenReturn(true);

        // execute
        final boolean result = manager.hasPermission(player, "animatedarchitecture.test");

        // verify
        assertThat(result).isTrue();

        verify(permissions).playerHas("world", player, "animatedarchitecture.test");
        verify(executor).isMainThread();
    }

    @Test
    void hasPermission_shouldKeepOfflineMainThreadGuard()
    {
        // setup
        when(executor.isMainThread()).thenReturn(true);
        when(player.isOnline()).thenReturn(false);
        when(player.getName()).thenReturn("Player");

        // execute + verify
        assertThatThrownBy(() -> manager.hasPermission(player, "animatedarchitecture.test"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Cannot check permissions for offline players on the main thread");

        verify(executor).isMainThread();
    }

    @Test
    void hasPermissionOffline_shouldCallVaultPlayerHasAsynchronously()
    {
        // setup
        final ExecutorService executorService = Executors.newSingleThreadExecutor();

        when(executor.getVirtualExecutor()).thenReturn(executorService);
        when(world.getName()).thenReturn("world");
        when(permissions.playerHas("world", offlinePlayer, "animatedarchitecture.test")).thenReturn(false);

        // execute
        final boolean result = manager
            .hasPermissionOffline(world, offlinePlayer, "animatedarchitecture.test")
            .join();
        executorService.shutdownNow();

        // verify
        assertThat(result).isFalse();

        verify(permissions).playerHas("world", offlinePlayer, "animatedarchitecture.test");
    }
}
