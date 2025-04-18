package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.exceptions.CommandExecutionException;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.core.tooluser.creator.Creator;
import nl.pim16aap2.testing.AssistedFactoryMocker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class SetNameTest
{
    @Mock
    private ToolUserManager toolUserManager;

    private AssistedFactoryMocker<SetName, SetName.IFactory> assistedFactoryMocker;

    @BeforeEach
    void init()
        throws NoSuchMethodException
    {
        assistedFactoryMocker = AssistedFactoryMocker.injectMocksFromTestClass(SetName.IFactory.class, this);
    }

    @AfterEach
    void afterEach()
    {
        verifyNoMoreInteractions(toolUserManager);
    }

    @Test
    void getCommand_shouldReturnSetName()
    {
        assertEquals(
            CommandDefinition.SET_NAME,
            assistedFactoryMocker.getFactory().newSetName(mock(), "name").getCommand()
        );
    }

    @Test
    void availableForNonPlayers_shouldReturnFalse()
    {
        assertFalse(assistedFactoryMocker.getFactory().newSetName(mock(), "name").availableForNonPlayers());
    }

    @Test
    void executeCommand_shouldSetNameForExistingCreator()
    {
        final IPlayer commandSender = mock();
        final Creator creator = mock();
        final String name = "my-new-portcullis";

        when(commandSender.getPlayer()).thenReturn(Optional.of(commandSender));
        when(toolUserManager.getToolUser(commandSender)).thenReturn(Optional.of(creator));

        assertDoesNotThrow(() ->
            assistedFactoryMocker.getFactory().newSetName(commandSender, name).executeCommand(null));

        verify(toolUserManager).getToolUser(commandSender);
        UnitTestUtil.verifyNoMessagesSent(commandSender);
        verify(creator).handleInput(name);
    }

    @Test
    void executeCommand_shouldThrowExceptionForNonPlayer()
    {
        final ICommandSender commandSender = mock();
        final String name = "my-new-flag";

        when(commandSender.getPlayer()).thenReturn(Optional.empty());

        assertThrows(
            IllegalStateException.class,
            () -> assistedFactoryMocker.getFactory().newSetName(commandSender, name).executeCommand(null)
        );
    }

    @Test
    void executeCommand_shouldThrowExceptionForNonCreatorToolUser()
    {
        final IPlayer commandSender = mock();
        final ToolUser toolUser = mock();
        final String name = "my-new-windmill";

        when(commandSender.getPlayer()).thenReturn(Optional.of(commandSender));
        when(toolUserManager.getToolUser(commandSender)).thenReturn(Optional.of(toolUser));

        final CommandExecutionException exception = UnitTestUtil.assertRootCause(
            CommandExecutionException.class,
            () -> assistedFactoryMocker
                .getFactory()
                .newSetName(commandSender, name)
                .executeCommand(null)
                .get()
        );

        assertTrue(exception.isUserInformed());
        assertEquals("No pending process.", exception.getMessage());

        verify(commandSender).sendError("commands.base.error.no_pending_process");
        verify(toolUserManager).getToolUser(commandSender);

        verifyNoInteractions(toolUser);
    }

    @Test
    void executeCommand_shouldThrowExceptionForNoToolUser()
    {
        final IPlayer commandSender = mock();
        final ToolUser toolUser = mock();
        final String name = "my-new-garage-door";

        when(commandSender.getPlayer()).thenReturn(Optional.of(commandSender));
        when(toolUserManager.getToolUser(commandSender)).thenReturn(Optional.empty());

        final CommandExecutionException exception = UnitTestUtil.assertRootCause(
            CommandExecutionException.class,
            () -> assistedFactoryMocker
                .getFactory()
                .newSetName(commandSender, name)
                .executeCommand(null)
                .get()
        );

        assertTrue(exception.isUserInformed());
        assertEquals("No pending process.", exception.getMessage());

        verify(commandSender).sendError("commands.base.error.no_pending_process");
        verify(toolUserManager).getToolUser(commandSender);

        verifyNoInteractions(toolUser);
    }
}
