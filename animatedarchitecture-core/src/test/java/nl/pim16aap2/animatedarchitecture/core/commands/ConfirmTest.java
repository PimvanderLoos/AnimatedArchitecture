package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.testing.AssistedFactoryMocker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class ConfirmTest
{
    @Mock
    private IPlayer player;

    private AssistedFactoryMocker<Confirm, Confirm.IFactory> assistedFactoryMocker;

    @BeforeEach
    void init()
        throws NoSuchMethodException
    {
        assistedFactoryMocker = AssistedFactoryMocker.injectMocksFromTestClass(Confirm.IFactory.class, this);
    }

    @Test
    void getCommand_shouldReturnConfirm()
    {
        final Confirm confirm = assistedFactoryMocker.getFactory().newConfirm(player);

        assertThat(confirm.getCommand()).isEqualTo(CommandDefinition.CONFIRM);
    }
}
