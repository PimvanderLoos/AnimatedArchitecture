package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ICommandSenderTest
{
    @Mock
    private ICommandSender commandSender;

    @Mock
    private IPlayer player;

    @Test
    void formatCommand_shouldAddLeadingSlashForPlayers()
    {
        // setup
        when(commandSender.isPlayer()).thenReturn(true);
        when(commandSender.formatCommand("animatedarchitecture help")).thenCallRealMethod();

        // execute & verify
        assertThat(commandSender.formatCommand("animatedarchitecture help"))
            .isEqualTo("/animatedarchitecture help");
    }

    @Test
    void formatCommand_shouldNotAddLeadingSlashForNonPlayers()
    {
        // setup
        when(commandSender.isPlayer()).thenReturn(false);
        when(commandSender.formatCommand("animatedarchitecture help")).thenCallRealMethod();

        // execute & verify
        assertThat(commandSender.formatCommand("animatedarchitecture help"))
            .isEqualTo("animatedarchitecture help");
    }
}
