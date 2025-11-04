package nl.pim16aap2.animatedarchitecture.spigot.util.implementations;

import nl.pim16aap2.animatedarchitecture.core.commands.IServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpigotServerTest
{
    @Mock
    private IServer server;

    @Test
    void formatCommand_shouldNotAddLeadingSlashForServer()
    {
        // setup
        when(server.formatCommand("animatedarchitecture help")).thenCallRealMethod();

        // execute & verify
        assertThat(server.formatCommand("animatedarchitecture help"))
            .isEqualTo("animatedarchitecture help");
    }

    @Test
    void formatCommand_shouldFormatWithArguments()
    {
        // setup
        when(server.formatCommand("%s %s %d", "cmd", "arg", 123)).thenCallRealMethod();

        // execute & verify
        assertThat(server.formatCommand("%s %s %d", "cmd", "arg", 123))
            .isEqualTo("cmd arg 123");
    }
}
