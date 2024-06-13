package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IPermissionsManager;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.core.tooluser.creator.Creator;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import org.junit.jupiter.api.Assertions;
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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

    @BeforeEach
    void init()
    {
        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, true);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();
        Mockito.when(permissionsManager.hasPermissionToCreateStructure(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.when(permissionsManager.hasPermission(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.when(commandSender.hasPermission(Mockito.any(CommandDefinition.class)))
            .thenReturn(CompletableFuture.completedFuture(new PermissionsStatus(true, false)));

        Mockito.when(factory.newNewStructure(
                Mockito.any(ICommandSender.class),
                Mockito.any(StructureType.class),
                Mockito.any()))
            .thenAnswer(invoc -> new NewStructure(
                invoc.getArgument(0, ICommandSender.class),
                invoc.getArgument(1, StructureType.class),
                invoc.getArgument(2, String.class),
                localizer,
                ITextFactory.getSimpleTextFactory(),
                permissionsManager,
                toolUserManager,
                creatorContextProvider)
            );
    }

    @Test
    void testPermissionsWithCreateStructurePermission()
        throws ExecutionException, InterruptedException
    {
        Mockito.when(permissionsManager.hasPermissionToCreateStructure(Mockito.any(), Mockito.any())).thenReturn(true);
        final NewStructure cmd = factory.newNewStructure(commandSender, doorType);

        setCommandSenderCommandPermissions(commandSender, false, false);
        Assertions.assertEquals(new PermissionsStatus(false, false), cmd.hasPermission().get());

        setCommandSenderCommandPermissions(commandSender, false, true);
        Assertions.assertEquals(new PermissionsStatus(false, true), cmd.hasPermission().get());

        setCommandSenderCommandPermissions(commandSender, true, false);
        Assertions.assertEquals(new PermissionsStatus(true, false), cmd.hasPermission().get());

        setCommandSenderCommandPermissions(commandSender, true, true);
        Assertions.assertEquals(new PermissionsStatus(true, true), cmd.hasPermission().get());
    }

    @Test
    void testPermissionsWithoutCreateStructurePermission()
        throws ExecutionException, InterruptedException
    {
        Mockito.when(permissionsManager.hasPermissionToCreateStructure(Mockito.any(), Mockito.any())).thenReturn(false);
        final NewStructure cmd = factory.newNewStructure(commandSender, doorType);

        setCommandSenderCommandPermissions(commandSender, false, false);
        Assertions.assertEquals(new PermissionsStatus(false, false), cmd.hasPermission().get());

        setCommandSenderCommandPermissions(commandSender, false, true);
        Assertions.assertEquals(new PermissionsStatus(false, true), cmd.hasPermission().get());

        setCommandSenderCommandPermissions(commandSender, true, false);
        Assertions.assertEquals(new PermissionsStatus(false, false), cmd.hasPermission().get());

        setCommandSenderCommandPermissions(commandSender, true, true);
        Assertions.assertEquals(new PermissionsStatus(false, true), cmd.hasPermission().get());
    }

    @Test
    void testServer()
    {
        final IServer server = Mockito.mock(IServer.class, Answers.CALLS_REAL_METHODS);
        Assertions.assertDoesNotThrow(
            () -> factory.newNewStructure(server, doorType, null).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager, Mockito.never()).startToolUser(Mockito.any(), Mockito.anyInt());
    }

    @Test
    void testExecution()
    {
        final String name = "newDoor";

        final Creator unnamedCreator = Mockito.mock(Creator.class);
        final Creator namedCreator = Mockito.mock(Creator.class);

        Mockito.when(doorType.getCreator(Mockito.any(), Mockito.any(), Mockito.any()))
            .thenAnswer(inv -> name.equals(inv.getArgument(2, String.class)) ? namedCreator : unnamedCreator);

        Assertions.assertDoesNotThrow(
            () -> factory.newNewStructure(commandSender, doorType).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager).startToolUser(unnamedCreator, Constants.STRUCTURE_CREATOR_TIME_LIMIT);

        Assertions.assertDoesNotThrow(
            () -> factory.newNewStructure(commandSender, doorType, name).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(toolUserManager).startToolUser(namedCreator, Constants.STRUCTURE_CREATOR_TIME_LIMIT);
    }

    private static void setCommandSenderCommandPermissions(
        ICommandSender commandSender, boolean userPermission, boolean adminPermission)
    {
        Mockito.when(commandSender.hasPermission(Mockito.any(CommandDefinition.class)))
            .thenReturn(CompletableFuture.completedFuture(
                new PermissionsStatus(userPermission, adminPermission)));
    }
}
