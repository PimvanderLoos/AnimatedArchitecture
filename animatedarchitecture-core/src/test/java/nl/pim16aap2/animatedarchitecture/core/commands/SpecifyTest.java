package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureSpecificationManager;
import nl.pim16aap2.testing.AssistedFactoryMocker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class SpecifyTest
{
    @Mock
    private StructureSpecificationManager structureSpecificationManager;

    @Mock
    private IExecutor executor;

    private AssistedFactoryMocker<Specify, Specify.IFactory> assistedFactoryMocker;

    @BeforeEach
    void init()
        throws NoSuchMethodException
    {
        assistedFactoryMocker = new AssistedFactoryMocker<>(Specify.class, Specify.IFactory.class)
            .injectParameters(executor, structureSpecificationManager);
    }

    @AfterEach
    void afterEach()
    {
        verifyNoMoreInteractions(structureSpecificationManager);
    }

    @Test
    void getCommand_shouldReturnSpecify()
    {
        assertEquals(
            CommandDefinition.SPECIFY,
            assistedFactoryMocker.getFactory().newSpecify(mock(), "name").getCommand()
        );
    }

    @Test
    void availableForNonPlayers_shouldReturnFalse()
    {
        assertFalse(assistedFactoryMocker.getFactory().newSpecify(mock(), "name").availableForNonPlayers());
    }

    @Test
    void runWithRawResult_shouldAbortAndSendErrorForNonPlayerCommandSender()
        throws ExecutionException, InterruptedException, TimeoutException
    {
        final IServer server = mock();
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        final Specify specify = spy(
            assistedFactoryMocker
                .getFactory()
                .newSpecify(server, "my-new-portcullis-name")
        );
        specify.run().get(1, TimeUnit.SECONDS);

        verify(specify, never()).executeCommand(any());
        verify(server).sendError(eq("commands.base.error.only_available_for_players"));
        verify(structureSpecificationManager, never()).handleInput(any(), any());
    }

    @Test
    void executeCommand_shouldSendErrorMessageWhenNoProcessExists()
    {
        final IPlayer player = mock();
        final String name = "my-new-windmill-name";
        when(structureSpecificationManager.handleInput(player, name)).thenReturn(false);

        assistedFactoryMocker
            .getFactory()
            .newSpecify(player, name)
            .executeCommand(null)
            .join();

        verify(player).sendError(eq("commands.base.error.no_pending_process"));
    }

    @Test
    void executeCommand_shouldNotSendErrorMessageWhenProcessExists()
    {
        final IPlayer player = mock();
        final String name = "my-new-flag-name";
        when(structureSpecificationManager.handleInput(player, name)).thenReturn(true);

        assistedFactoryMocker
            .getFactory()
            .newSpecify(player, name)
            .executeCommand(null)
            .join();

        verify(player, never()).sendError(anyString());
    }
}
