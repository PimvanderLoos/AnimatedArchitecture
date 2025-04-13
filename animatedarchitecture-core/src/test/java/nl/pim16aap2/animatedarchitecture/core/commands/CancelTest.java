package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureSpecificationManager;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;
import nl.pim16aap2.testing.AssistedFactoryMocker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.animatedarchitecture.core.commands.CommandTestingUtil.initCommandSenderPermissions;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class CancelTest
{
    @Mock
    private IPlayer commandSender;

    @Mock
    private StructureSpecificationManager structureSpecificationManager;

    private Cancel.IFactory factory;

    @Mock
    private ToolUserManager toolUserManager;

    @Mock
    private IExecutor executor;

    @BeforeEach
    void init()
        throws NoSuchMethodException
    {
        factory = AssistedFactoryMocker.injectMocksFromTestClass(Cancel.IFactory.class, this).getFactory();
    }

    @Test
    void cancelPlayer_shouldCancelToolUser()
    {
        // Setup
        final var cancel = factory.newCancel(commandSender);
        when(toolUserManager.cancelToolUser(commandSender)).thenReturn(true);

        // Execute
        cancel.cancelPlayer(commandSender);

        // Verify
        UnitTestUtil.assertThatMessageable(commandSender).sentSuccessMessage("commands.cancel.success");
    }

    @Test
    void cancelPlayer_shouldCancelRequest()
    {
        // Setup
        final var cancel = factory.newCancel(commandSender);
        when(structureSpecificationManager.cancelRequest(commandSender)).thenReturn(true);

        // Execute
        cancel.cancelPlayer(commandSender);

        // Verify
        UnitTestUtil.assertThatMessageable(commandSender).sentSuccessMessage("commands.cancel.success");
    }

    @Test
    void cancelPlayer_shouldSendErrorWhenNothingToCancel()
    {
        // Setup
        final var cancel = factory.newCancel(commandSender);

        // Execute
        cancel.cancelPlayer(commandSender);

        // Verify
        UnitTestUtil.assertThatMessageable(commandSender).sentErrorMessage("commands.cancel.no_process");
    }

    @Test
    void run_shouldCancelIfNeeded()
    {
        // Setup
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());
        when(toolUserManager.cancelToolUser(commandSender)).thenReturn(true);
        when(commandSender.getPlayer()).thenReturn(Optional.of(commandSender));
        initCommandSenderPermissions(commandSender, true, true);

        // Execute
        Assertions.assertDoesNotThrow(() -> factory.newCancel(commandSender).run().get(1, TimeUnit.SECONDS));

        // Verify
        verify(toolUserManager).cancelToolUser(commandSender);
        UnitTestUtil.assertThatMessageable(commandSender).sentSuccessMessage("commands.cancel.success");
    }
}
