package nl.pim16aap2.animatedarchitecture.spigot.core.managers;

import nl.pim16aap2.animatedarchitecture.spigot.util.api.IPermissionsManagerSpigot;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class PermissionsManagerModuleTest
{
    @Test
    void selectPermissionsManager_shouldPreferLuckPermsWhenBothBackendsArePresent()
    {
        // setup
        final IPermissionsManagerSpigot luckPermsManager = mock(IPermissionsManagerSpigot.class);
        final IPermissionsManagerSpigot vaultManager = mock(IPermissionsManagerSpigot.class);
        final AtomicBoolean vaultSupplierCalled = new AtomicBoolean(false);

        // execute
        final IPermissionsManagerSpigot result = PermissionsManagerModule.selectPermissionsManager(
            true,
            () -> luckPermsManager,
            true,
            () ->
            {
                vaultSupplierCalled.set(true);
                return vaultManager;
            }
        );

        // verify
        assertThat(result).isSameAs(luckPermsManager);
        assertThat(vaultSupplierCalled).isFalse();
    }

    @Test
    void selectPermissionsManager_shouldFallBackToVaultWhenLuckPermsIsAbsent()
    {
        // setup
        final IPermissionsManagerSpigot luckPermsManager = mock(IPermissionsManagerSpigot.class);
        final IPermissionsManagerSpigot vaultManager = mock(IPermissionsManagerSpigot.class);

        // execute
        final IPermissionsManagerSpigot result = PermissionsManagerModule.selectPermissionsManager(
            false,
            () -> luckPermsManager,
            true,
            () -> vaultManager
        );

        // verify
        assertThat(result).isSameAs(vaultManager);
    }

    @Test
    void selectPermissionsManager_shouldFailFastWhenNoPermissionBackendIsPresent()
    {
        // setup

        // execute + verify
        assertThatThrownBy(() -> PermissionsManagerModule.selectPermissionsManager(
            false,
            () -> mock(IPermissionsManagerSpigot.class),
            false,
            () -> mock(IPermissionsManagerSpigot.class)
        ))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Install LuckPerms or Vault");
    }
}
