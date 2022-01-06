package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.*;

class BaseCommandTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private BaseCommand baseCommand;

    @Mock
    private AbstractDoor door;

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
    @SneakyThrows
    void testHasAccess()
    {
        Assertions.assertTrue(baseCommand.hasAccessToAttribute(door, DoorAttribute.DELETE, true));
        Assertions.assertTrue(baseCommand.hasAccessToAttribute(door, DoorAttribute.DELETE, false));

        final IPPlayer player = Mockito.mock(IPPlayer.class, Answers.CALLS_REAL_METHODS);
        UnitTestUtil.setField(BaseCommand.class, baseCommand, "commandSender", player);

        Mockito.when(door.getDoorOwner(player)).thenReturn(Optional.of(doorOwner3));
        Assertions.assertFalse(baseCommand.hasAccessToAttribute(door, DoorAttribute.DELETE, false));
        Assertions.assertTrue(baseCommand.hasAccessToAttribute(door, DoorAttribute.DELETE, true));

        Mockito.when(door.getDoorOwner(player)).thenReturn(Optional.of(doorOwner1));
        Assertions.assertFalse(baseCommand.hasAccessToAttribute(door, DoorAttribute.DELETE, false));

        Mockito.when(door.getDoorOwner(player)).thenReturn(Optional.of(doorOwner0));
        Assertions.assertTrue(baseCommand.hasAccessToAttribute(door, DoorAttribute.DELETE, false));
    }

    @Test
    @SneakyThrows
    void testBasic()
    {
        Mockito.when(baseCommand.executeCommand(Mockito.any())).thenReturn(CompletableFuture.completedFuture(true));
        final CompletableFuture<Boolean> result = baseCommand.run();
        Assertions.assertTrue(result.get(1, TimeUnit.SECONDS));
    }

    @Test
    @SneakyThrows
    void testNegativeExecution()
    {
        Mockito.when(baseCommand.executeCommand(Mockito.any())).thenReturn(CompletableFuture.completedFuture(false));
        final CompletableFuture<Boolean> result = baseCommand.run();
        Assertions.assertFalse(result.get(1, TimeUnit.SECONDS));
    }

    @Test
    @SneakyThrows
    void invalidInput()
    {
        Mockito.when(baseCommand.validInput()).thenReturn(false);
        final CompletableFuture<Boolean> result = baseCommand.run();
        Assertions.assertFalse(result.get(1, TimeUnit.SECONDS));
    }

    @Test
    @SneakyThrows
    void testPermissionFailure()
    {
        Mockito.when(baseCommand.executeCommand(Mockito.any())).thenReturn(CompletableFuture.completedFuture(true));
        Mockito.when(commandSender.hasPermission(Mockito.any(CommandDefinition.class)))
               .thenReturn(CompletableFuture.completedFuture(new PermissionsStatus(false, false)));

        final CompletableFuture<Boolean> result = baseCommand.run();
        Assertions.assertTrue(result.get(1, TimeUnit.SECONDS));
    }

    @Test
    void testExceptionPermission()
    {
        Mockito.when(baseCommand.executeCommand(Mockito.any())).thenReturn(CompletableFuture.completedFuture(true));

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
        Mockito.when(baseCommand.executeCommand(Mockito.any())).thenReturn(CompletableFuture.completedFuture(true));
        final CompletableFuture<Boolean> exceptional = new CompletableFuture<>();
        exceptional.completeExceptionally(new IllegalStateException("Testing exception!"));

        Mockito.when(baseCommand.executeCommand(Mockito.any(PermissionsStatus.class))).thenReturn(exceptional);

        ExecutionException exception =
            Assertions.assertThrows(ExecutionException.class,
                                    () -> baseCommand.startExecution().get(1, TimeUnit.SECONDS));
        Assertions.assertEquals(IllegalStateException.class, exception.getCause().getCause().getCause().getClass());
    }

    @SneakyThrows
    private static void initBaseCommand(BaseCommand baseCommand, ICommandSender commandSender, ILocalizer localizer)
    {
        UnitTestUtil.setField(BaseCommand.class, baseCommand, "commandSender", commandSender);
        UnitTestUtil.setField(BaseCommand.class, baseCommand, "localizer", localizer);
    }
}
