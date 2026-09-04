package nl.pim16aap2.animatedarchitecture.spigot.core.managers;

import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DisabledEconomyManagerTest
{
    @Test
    void isEconomyEnabled_shouldReturnFalse()
    {
        // setup
        final DisabledEconomyManager manager = new DisabledEconomyManager();

        // execute
        final boolean result = manager.isEconomyEnabled();

        // verify
        assertThat(result).isFalse();
    }

    @Test
    void buyStructure_shouldAllowPurchaseWhenEconomyIsDisabled()
    {
        // setup
        final DisabledEconomyManager manager = new DisabledEconomyManager();

        // execute
        final boolean result = manager.buyStructure(
            mock(IPlayer.class),
            mock(IWorld.class),
            mock(StructureType.class),
            10
        );

        // verify
        assertThat(result).isTrue();
    }

    @Test
    void getPrice_shouldReturnEmptyWhenEconomyIsDisabled()
    {
        // setup
        final DisabledEconomyManager manager = new DisabledEconomyManager();

        // execute
        final var result = manager.getPrice(mock(StructureType.class), 10);

        // verify
        assertThat(result).isEmpty();
    }
}
