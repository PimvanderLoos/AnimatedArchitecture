package nl.pim16aap2.animatedarchitecture.core.commands;

import lombok.Setter;
import nl.altindag.log.LogCaptor;
import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.exceptions.CommandExecutionException;
import nl.pim16aap2.animatedarchitecture.core.exceptions.NoStructuresForCommandException;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureOwner;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.testing.annotations.WithLogCapture;
import nl.pim16aap2.util.exceptions.ContextualOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static nl.pim16aap2.testing.assertions.LogCaptorAssert.assertThatLogCaptor;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@WithLogCapture
class BaseCommandTest
{
    @Test
    void shouldInformUser_shouldReturnTrueForUninformedCommandExecutionException()
    {
        final var exception = new CommandExecutionException(false);
        assertTrue(BaseCommand.shouldInformUser(exception));
    }

    @Test
    void shouldInformUser_shouldReturnFalseForInformedCommandExecutionException()
    {
        final var exception = new CommandExecutionException(true);
        assertFalse(BaseCommand.shouldInformUser(exception));
    }

    @Test
    void shouldInformUser_shouldReturnTrueForWrappedUninformedCommandExecutionException()
    {
        final var exception = new CommandExecutionException(false);
        final var wrappedException = new CompletionException(exception);
        assertTrue(BaseCommand.shouldInformUser(wrappedException));
    }

    @Test
    void shouldInformUser_shouldReturnFalseForWrappedInformedCommandExecutionException()
    {
        final var exception = new CommandExecutionException(true);
        final var wrappedException = new CompletionException(exception);
        assertFalse(BaseCommand.shouldInformUser(wrappedException));
    }

    @Test
    void shouldInformUser_shouldReturnTrueForOtherExceptions()
    {
        final var exception = new RuntimeException();
        final var wrappedException = new CompletionException(exception);
        assertTrue(BaseCommand.shouldInformUser(wrappedException));
    }

    @Test
    void handleRunException_shouldLogAtSevereForUninformedException(LogCaptor logCaptor)
    {
        final var exception = new CommandExecutionException(false);
        final var command = TestCommand.withMocks();

        command.handleRunException(exception);

        assertThatLogCaptor(logCaptor)
            .atError()
            .singleWithMessageExactly("Failed to execute command: %s", command);
    }

    @Test
    void handleRunException_shouldLogAtFineForInformedException(LogCaptor logCaptor)
    {
        logCaptor.setLogLevelToTrace();

        final var exception = new CommandExecutionException(true);
        final var command = TestCommand.withMocks();

        command.handleRunException(exception);

        assertThatLogCaptor(logCaptor)
            .atDebug()
            .singleWithMessageExactly("Failed to execute command: %s", command);
    }

    @Test
    void handleRunException_shouldSendPlayerGenericErrorForUninformedException()
    {
        final ICommandSender commandSender = mock();
        final var exception = new CommandExecutionException(false);
        final var command = spy(TestCommand.withMocks(commandSender));

        when(commandSender.isPlayer()).thenReturn(true);

        command.handleRunException(exception);

        verify(command).sendGenericErrorMessage();
    }

    @Test
    void hasAccessToAttribute_shouldReturnTrueWithBypass()
    {
        final var command = TestCommand.withMocks();

        assertTrue(command.hasAccessToAttribute(mock(), mock(), true));
    }

    @Test
    void hasAccessToAttribute_shouldReturnTrueForNonPlayers()
    {
        final ICommandSender commandSender = mock();
        final var command = TestCommand.withMocks(commandSender);

        when(commandSender.isPlayer()).thenReturn(false);

        assertTrue(command.hasAccessToAttribute(mock(), mock(), false));
    }

    @Test
    void hasAccessToAttribute_shouldReturnFalseForNonOwners()
    {
        final IPlayer commandSender = mock();
        final var command = TestCommand.withMocks(commandSender);
        final Structure structure = mock();

        when(commandSender.isPlayer()).thenReturn(true);
        when(commandSender.getPlayer()).thenReturn(Optional.of(commandSender));
        when(structure.getOwner(commandSender)).thenReturn(Optional.empty());

        assertFalse(command.hasAccessToAttribute(structure, mock(), false));
    }

    @Test
    void hasAccessToAttribute_shouldCheckStructureAttribute()
    {
        final IPlayer commandSender = mock();
        final var command = TestCommand.withMocks(commandSender);
        final Structure structure = mock();
        final StructureOwner structureOwner = new StructureOwner(1, PermissionLevel.USER, mock());
        final StructureAttribute attribute = mock();

        when(commandSender.isPlayer()).thenReturn(true);
        when(commandSender.getPlayer()).thenReturn(Optional.of(commandSender));
        when(structure.getOwner(commandSender)).thenReturn(Optional.of(structureOwner));

        when(attribute.canAccessWith(PermissionLevel.USER)).thenReturn(true);
        assertTrue(command.hasAccessToAttribute(structure, attribute, false));

        when(attribute.canAccessWith(PermissionLevel.USER)).thenReturn(false);
        assertFalse(command.hasAccessToAttribute(structure, attribute, false));
    }

    @Test
    void availableForNonPlayers_shouldDefaultToTrue()
    {
        final var command = TestCommand.withMocks();
        assertTrue(command.availableForNonPlayers());
    }

    @Test
    void availableForNonPlayers_shouldReturnTrue()
    {
        final var command = TestCommand.withMocks();
        assertTrue(command.availableForNonPlayers());
    }

    @Test
    void sendGenericErrorMessage_shouldSendMessage()
    {
        final ICommandSender commandSender = mock();
        final var command = TestCommand.withMocks(commandSender);

        command.sendGenericErrorMessage();

        verify(commandSender).sendError("commands.base.error.generic");
    }

    @Test
    void handlePermissionResult_shouldThrowExceptionForNoPermission()
    {
        final ICommandSender commandSender = mock();
        final var command = TestCommand.withMocks(commandSender);
        final PermissionsStatus permissions = new PermissionsStatus(false, false);

        final CommandExecutionException exception = UnitTestUtil.assertRootCause(
            CommandExecutionException.class,
            () -> command.handlePermissionResult(permissions)
        );

        assertTrue(exception.isUserInformed());
        assertEquals(
            String.format("CommandSender %s does not have permission to run command: %s", commandSender, command),
            exception.getMessage()
        );

        verify(commandSender).sendError("commands.base.error.no_permission_for_command");
    }

    @Test
    void handlePermissionResult_shouldContinueWithPermission()
    {
        final var command = spy(TestCommand.withMocks());
        final PermissionsStatus permissions = new PermissionsStatus(true, true);

        command.handlePermissionResult(permissions);

        verify(command).executeCommand(permissions);
    }

    @Test
    void hasPermission_shouldDelegateToCommandSender()
        throws ExecutionException, InterruptedException
    {
        final ICommandSender commandSender = mock();
        final var command = TestCommand.withMocks(commandSender);
        final CommandDefinition commandDefinition = mock();
        final var permissions = new PermissionsStatus(true, false);

        command.setCommandDefinition(commandDefinition);
        when(commandSender.hasPermission(commandDefinition)).thenReturn(CompletableFuture.completedFuture(permissions));

        assertEquals(permissions, command.hasPermission().get());
    }

    @Test
    void getStructure_shouldFindStructureFromRetriever()
        throws ExecutionException, InterruptedException
    {
        final ICommandSender commandSender = mock();
        final Structure structure = mock();
        final StructureRetriever structureRetriever = StructureRetrieverFactory.ofStructure(structure);
        final var command = TestCommand.withMocks(commandSender);
        final PermissionLevel permissionLevel = PermissionLevel.USER;

        when(commandSender.getPlayer()).thenReturn(Optional.empty());

        assertEquals(structure, command.getStructure(structureRetriever, permissionLevel).get());
    }

    @Test
    void getStructure_shouldGetStructureInteractivelyForPlayer()
        throws ExecutionException, InterruptedException
    {
        final ICommandSender commandSender = mock();
        final IPlayer playerCommandSender = mock();
        final var command = TestCommand.withMocks(commandSender);
        final Structure structure = mock();
        final StructureRetriever structureRetriever = spy(StructureRetrieverFactory.ofStructure(structure));
        final var permissionLevel = PermissionLevel.USER;

        when(structure.isOwner(playerCommandSender, permissionLevel)).thenReturn(true);
        when(commandSender.getPlayer()).thenReturn(Optional.of(playerCommandSender));

        command.getStructure(structureRetriever, permissionLevel).get();

        verify(structureRetriever).getStructureInteractive(playerCommandSender, permissionLevel);
    }

    @Test
    void getStructure_shouldThrowExceptionWhenNoStructureFound()
    {
        final ICommandSender commandSender = mock();
        final var command = TestCommand.withMocks(commandSender);
        final Structure structure = mock();
        final StructureRetriever structureRetriever = spy(StructureRetrieverFactory.ofStructure(structure));
        final PermissionLevel permissionLevel = PermissionLevel.USER;

        when(structureRetriever.getStructure()).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        when(commandSender.getPlayer()).thenReturn(Optional.empty());

        final NoStructuresForCommandException exception = UnitTestUtil.assertRootCause(
            NoStructuresForCommandException.class,
            () -> command.getStructure(structureRetriever, permissionLevel).get()
        );

        verify(commandSender).sendError("commands.base.error.cannot_find_target_structure");
        assertTrue(exception.isUserInformed());
    }

    @Test
    void getStructure_shouldHaveExceptionContext()
    {
        final ICommandSender commandSender = mock();
        final var command = TestCommand.withMocks(commandSender);
        final Structure structure = mock();
        final StructureRetriever structureRetriever = spy(StructureRetrieverFactory.ofStructure(structure));

        when(structureRetriever
            .getStructure())
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Test exception")));

        assertThatExceptionOfType(ExecutionException.class)
            .isThrownBy(() -> command.getStructure(structureRetriever, PermissionLevel.USER).get())
            .havingCause()
            .isExactlyInstanceOf(ContextualOperationException.class)
            .withMessageStartingWith("Get structure from retriever 'StructureRetriever.StructureObjectRetriever(")
            .havingCause()
            .isExactlyInstanceOf(RuntimeException.class)
            .withMessageStartingWith("Test exception")
            .withNoCause();
    }

    /**
     * Test class that extends {@link BaseCommand} to test the abstract methods.
     */
    @Setter
    private static final class TestCommand extends BaseCommand
    {
        private CommandDefinition commandDefinition = mock();

        TestCommand(
            ICommandSender commandSender,
            IExecutor executor)
        {
            super(commandSender, executor);
        }

        /**
         * Create a new instance of {@link TestCommand}.
         * <p>
         * Any provided objects will be used as constructor arguments if they match the required type.
         * <p>
         * Missing objects will be mocked.
         *
         * @param objects
         *     The objects to use as constructor arguments. The order does not matter.
         * @return The new instance of {@link TestCommand}.
         */
        static TestCommand withMocks(Object... objects)
        {
            return new TestCommand(
                getOrMock(objects, ICommandSender.class),
                getOrMock(objects, IExecutor.class)
            );
        }

        private static <T> T getOrMock(Object[] objects, Class<T> clazz)
        {
            for (Object object : objects)
                if (clazz.isInstance(object))
                    return clazz.cast(object);
            return mock(clazz);
        }

        @Override
        public CommandDefinition getCommand()
        {
            return commandDefinition;
        }

        @Override
        protected CompletableFuture<?> executeCommand(PermissionsStatus permissions)
        {
            return CompletableFuture.completedFuture(null);
        }
    }
}
