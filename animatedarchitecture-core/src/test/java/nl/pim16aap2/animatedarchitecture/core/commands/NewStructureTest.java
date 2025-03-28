package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPermissionsManager;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.core.tooluser.creator.Creator;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.anyInt;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NewStructureTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer commandSender;

    @Mock
    private StructureType doorType;

    @Mock
    private ToolUserManager toolUserManager;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private NewStructure.IFactory factory;

    @Mock
    private javax.inject.Provider<ToolUser.Context> creatorContextProvider;

    @Mock
    private IPermissionsManager permissionsManager;

    @Mock
    private IExecutor executor;

    @BeforeEach
    void init()
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, true);

        when(permissionsManager.hasPermissionToCreateStructure(any(), any())).thenReturn(true);
        when(permissionsManager.hasPermission(any(), any())).thenReturn(true);
        when(commandSender
            .hasPermission(any(CommandDefinition.class)))
            .thenReturn(CompletableFuture.completedFuture(new PermissionsStatus(true, false)));

        when(factory
            .newNewStructure(any(ICommandSender.class), any(StructureType.class), any()))
            .thenAnswer(invoc -> new NewStructure(
                invoc.getArgument(0, ICommandSender.class),
                invoc.getArgument(1, StructureType.class),
                invoc.getArgument(2, String.class),
                executor,
                permissionsManager,
                toolUserManager,
                creatorContextProvider)
            );
    }

    @Test
    void testPermissionsWithCreateStructurePermission()
        throws ExecutionException, InterruptedException
    {
        when(permissionsManager.hasPermissionToCreateStructure(any(), any())).thenReturn(true);
        final NewStructure cmd = factory.newNewStructure(commandSender, doorType);

        setCommandSenderCommandPermissions(commandSender, false, false);
        assertEquals(new PermissionsStatus(false, false), cmd.hasPermission().get());

        setCommandSenderCommandPermissions(commandSender, false, true);
        assertEquals(new PermissionsStatus(false, true), cmd.hasPermission().get());

        setCommandSenderCommandPermissions(commandSender, true, false);
        assertEquals(new PermissionsStatus(true, false), cmd.hasPermission().get());

        setCommandSenderCommandPermissions(commandSender, true, true);
        assertEquals(new PermissionsStatus(true, true), cmd.hasPermission().get());
    }

    @Test
    void testPermissionsWithoutCreateStructurePermission()
        throws ExecutionException, InterruptedException
    {
        when(permissionsManager.hasPermissionToCreateStructure(any(), any())).thenReturn(false);
        final NewStructure cmd = factory.newNewStructure(commandSender, doorType);

        setCommandSenderCommandPermissions(commandSender, false, false);
        assertEquals(new PermissionsStatus(false, false), cmd.hasPermission().get());

        setCommandSenderCommandPermissions(commandSender, false, true);
        assertEquals(new PermissionsStatus(false, true), cmd.hasPermission().get());

        setCommandSenderCommandPermissions(commandSender, true, false);
        assertEquals(new PermissionsStatus(false, false), cmd.hasPermission().get());

        setCommandSenderCommandPermissions(commandSender, true, true);
        assertEquals(new PermissionsStatus(false, true), cmd.hasPermission().get());
    }

    @Test
    void testServer()
    {
        final IServer server = mock(IServer.class, Answers.CALLS_REAL_METHODS);
        assertDoesNotThrow(() -> factory.newNewStructure(server, doorType, null).run().get(1, TimeUnit.SECONDS));
        verify(toolUserManager, never()).startToolUser(any(), anyInt());
    }

    @Test
    void testExecution()
    {
        final String name = "newDoor";

        final Creator unnamedCreator = mock(Creator.class);
        final Creator namedCreator = mock(Creator.class);

        when(doorType
            .getCreator(any(), any(), any()))
            .thenAnswer(inv -> name.equals(inv.getArgument(2, String.class)) ? namedCreator : unnamedCreator);

        assertDoesNotThrow(
            () -> factory.newNewStructure(commandSender, doorType).run().get(1, TimeUnit.SECONDS));
        verify(toolUserManager).startToolUser(unnamedCreator, Constants.STRUCTURE_CREATOR_TIME_LIMIT);

        assertDoesNotThrow(
            () -> factory.newNewStructure(commandSender, doorType, name).run().get(1, TimeUnit.SECONDS));
        verify(toolUserManager).startToolUser(namedCreator, Constants.STRUCTURE_CREATOR_TIME_LIMIT);
    }

    private static void setCommandSenderCommandPermissions(
        ICommandSender commandSender,
        boolean userPermission,
        boolean adminPermission)
    {
        when(commandSender
            .hasPermission(any(CommandDefinition.class)))
            .thenReturn(CompletableFuture.completedFuture(new PermissionsStatus(userPermission, adminPermission)));
    }
}
