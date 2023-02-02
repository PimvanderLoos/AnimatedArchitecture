package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableAttribute;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.*;

@Timeout(1)
class BaseCommandTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private BaseCommand baseCommand;

    @Mock
    private AbstractMovable door;

    @Mock
    private ICommandSender commandSender;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        initBaseCommand(baseCommand, commandSender, UnitTestUtil.initLocalizer());

        Mockito.when(baseCommand.getCommand()).thenReturn(CommandDefinition.ADD_OWNER);
        Mockito.when(baseCommand.validInput()).thenCallRealMethod();
        Mockito.when(baseCommand.hasPermission()).thenCallRealMethod();

        initCommandSenderPermissions(commandSender, true, true);
    }

    @Test
    void testHasAccess()
    {
        Assertions.assertTrue(baseCommand.hasAccessToAttribute(door, MovableAttribute.DELETE, true));
        Assertions.assertTrue(baseCommand.hasAccessToAttribute(door, MovableAttribute.DELETE, false));

        final IPPlayer player = Mockito.mock(IPPlayer.class, Answers.CALLS_REAL_METHODS);
        UnitTestUtil.setField(BaseCommand.class, baseCommand, "commandSender", player);

        Mockito.when(door.getOwner(player)).thenReturn(Optional.of(movableOwnerNoPerm));
        Assertions.assertFalse(baseCommand.hasAccessToAttribute(door, MovableAttribute.DELETE, false));
        Assertions.assertTrue(baseCommand.hasAccessToAttribute(door, MovableAttribute.DELETE, true));

        Mockito.when(door.getOwner(player)).thenReturn(Optional.of(movableOwnerAdmin));
        Assertions.assertFalse(baseCommand.hasAccessToAttribute(door, MovableAttribute.DELETE, false));

        Mockito.when(door.getOwner(player)).thenReturn(Optional.of(movableOwnerCreator));
        Assertions.assertTrue(baseCommand.hasAccessToAttribute(door, MovableAttribute.DELETE, false));
    }

    @Test
    void testBasic()
    {
        Mockito.when(baseCommand.executeCommand(Mockito.any())).thenReturn(CompletableFuture.completedFuture(null));
        final CompletableFuture<?> result = baseCommand.run();
        Assertions.assertDoesNotThrow(() -> result.get(1, TimeUnit.SECONDS));
    }

    @Test
    void testNegativeExecution()
    {
        Mockito.when(baseCommand.executeCommand(Mockito.any())).thenReturn(CompletableFuture.completedFuture(null));
        final CompletableFuture<?> result = baseCommand.run();
        Assertions.assertDoesNotThrow(() -> result.get(1, TimeUnit.SECONDS));
    }

    @Test
    void invalidInput()
    {
        Mockito.when(baseCommand.validInput()).thenReturn(false);
        final CompletableFuture<?> result = baseCommand.run();
        Assertions.assertDoesNotThrow(() -> result.get(1, TimeUnit.SECONDS));
    }

    @Test
    void testPermissionFailure()
    {
        Mockito.when(baseCommand.executeCommand(Mockito.any())).thenReturn(CompletableFuture.completedFuture(null));
        Mockito.when(commandSender.hasPermission(Mockito.any(CommandDefinition.class)))
               .thenReturn(CompletableFuture.completedFuture(new PermissionsStatus(false, false)));

        final CompletableFuture<?> result = baseCommand.run();
        Assertions.assertDoesNotThrow(() -> result.get(1, TimeUnit.SECONDS));
    }

    @Test
    void testExceptionPermission()
    {
        Mockito.when(baseCommand.executeCommand(Mockito.any())).thenReturn(CompletableFuture.completedFuture(null));

        final CompletableFuture<PermissionsStatus> exceptional = new CompletableFuture<>();
        exceptional.completeExceptionally(new IllegalStateException("Testing exception!"));

        Mockito.when(commandSender.hasPermission(Mockito.any(CommandDefinition.class))).thenReturn(exceptional);

        ExecutionException exception =
            Assertions.assertThrows(ExecutionException.class,
                                    () -> baseCommand.startExecution().get(1, TimeUnit.SECONDS));
        Assertions.assertEquals(IllegalStateException.class, exception.getCause().getClass());
    }

    @Test
    void testExecutionException()
    {
        Mockito.when(baseCommand.executeCommand(Mockito.any())).thenReturn(CompletableFuture.completedFuture(null));

        Mockito.when(baseCommand.executeCommand(Mockito.any(PermissionsStatus.class)))
               .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Testing exception!")));

        ExecutionException exception =
            Assertions.assertThrows(ExecutionException.class,
                                    () -> baseCommand.startExecution().get(1, TimeUnit.SECONDS));
        Assertions.assertEquals(IllegalStateException.class, exception.getCause().getCause().getCause().getClass());
    }

    private static void initBaseCommand(BaseCommand baseCommand, ICommandSender commandSender, ILocalizer localizer)
    {
        UnitTestUtil.setField(BaseCommand.class, baseCommand, "commandSender", commandSender);
        UnitTestUtil.setField(BaseCommand.class, baseCommand, "localizer", localizer);
        UnitTestUtil.setField(BaseCommand.class, baseCommand, "textFactory", ITextFactory.getSimpleTextFactory());
    }
}