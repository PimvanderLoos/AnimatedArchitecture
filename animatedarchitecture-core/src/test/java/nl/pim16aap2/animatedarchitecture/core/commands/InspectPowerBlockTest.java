package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;
import nl.pim16aap2.animatedarchitecture.core.tooluser.PowerBlockInspector;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.anyInt;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InspectPowerBlockTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer commandSender;

    @Mock
    private ToolUser toolUser;

    @Mock
    private ToolUserManager toolUserManager;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private InspectPowerBlock.IFactory factory;

    @Mock
    private IExecutor executor;

    @BeforeEach
    void init()
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        final UUID uuid = UUID.randomUUID();

        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, true);
        when(commandSender.getUUID()).thenReturn(uuid);
        when(toolUserManager.getToolUser(uuid)).thenReturn(Optional.of(toolUser));

        final PowerBlockInspector.IFactory inspectPowerBlockFactory = mock(PowerBlockInspector.IFactory.class);

        when(factory
            .newInspectPowerBlock(any(ICommandSender.class)))
            .thenAnswer(invoc -> new InspectPowerBlock(
                invoc.getArgument(0, ICommandSender.class),
                executor,
                toolUserManager,
                inspectPowerBlockFactory)
            );
    }

    @Test
    void testServer()
    {
        final IServer server = mock(IServer.class, Answers.CALLS_REAL_METHODS);
        assertDoesNotThrow(() -> factory.newInspectPowerBlock(server).run().get(1, TimeUnit.SECONDS));
        verify(toolUserManager, Mockito.never()).startToolUser(any(), anyInt());
    }

    @Test
    void testExecution()
    {
        assertDoesNotThrow(() -> factory.newInspectPowerBlock(commandSender).run().get(1, TimeUnit.SECONDS));
        verify(toolUserManager).startToolUser(any(), anyInt());
    }
}
