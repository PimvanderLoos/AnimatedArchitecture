package nl.pim16aap2.animatedarchitecture.spigot.core.managers;

import nl.pim16aap2.animatedarchitecture.core.api.IEconomyManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EconomyManagerModuleTest
{
    @Test
    void selectEconomyManager_shouldUseVaultEconomyWhenVaultIsPresent()
    {
        // setup
        final IEconomyManager vaultManager = mock();
        final IEconomyManager disabledManager = mock();

        // execute
        final IEconomyManager result = EconomyManagerModule.selectEconomyManager(
            true,
            () -> vaultManager,
            () -> disabledManager
        );

        // verify
        assertThat(result).isSameAs(vaultManager);
    }

    @Test
    void selectEconomyManager_shouldDisableEconomyWhenVaultIsAbsent()
    {
        // setup
        final IEconomyManager vaultManager = mock();
        final IEconomyManager disabledManager = mock();

        // execute
        final IEconomyManager result = EconomyManagerModule.selectEconomyManager(
            false,
            () -> vaultManager,
            () -> disabledManager
        );

        // verify
        assertThat(result).isSameAs(disabledManager);
    }
}
