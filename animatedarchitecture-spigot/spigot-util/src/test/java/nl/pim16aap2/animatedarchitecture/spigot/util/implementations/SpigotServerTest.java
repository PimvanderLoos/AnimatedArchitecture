package nl.pim16aap2.animatedarchitecture.spigot.util.implementations;

import nl.pim16aap2.animatedarchitecture.core.commands.IServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SpigotServerTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IServer server;

    @Test
    void formatCommand_shouldNotAddLeadingSlashForServer()
    {
        // execute & verify
        assertThat(server.formatCommand("animatedarchitecture", "help"))
            .isEqualTo("animatedarchitecture help");
    }

    @Test
    void formatCommand_shouldFormatWithArguments()
    {
        // execute & verify
        assertThat(server.formatCommand("cmd", "%s %d", "arg", 123))
            .isEqualTo("cmd arg 123");
    }

    @Test
    void formatCommand_shouldNotAddTrailingSpaceWhenSubCommandIsEmpty()
    {
        // execute & verify
        assertThat(server.formatCommand("animatedarchitecture", ""))
            .isEqualTo("animatedarchitecture");
    }
}
