package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.util.CompletableFutureHandler;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;
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
import java.util.logging.Level;

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.*;

class BaseCommandTest
{
    private IPLogger logger;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private BaseCommand baseCommand;

    @Mock
    private AbstractDoor door;

    @Mock
    private ICommandSender commandSender;

    @BeforeEach
    void init()
    {
        logger = new BasicPLogger();

        MockitoAnnotations.openMocks(this);

        initBaseCommand(baseCommand, commandSender, logger, UnitTestUtil.initLocalizer(),
                        new CompletableFutureHandler(logger));

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

        final var player = Mockito.mock(IPPlayer.class, Answers.CALLS_REAL_METHODS);
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
        final var result = baseCommand.run();
        Assertions.assertTrue(result.get(1, TimeUnit.SECONDS));
    }

    @Test
    @SneakyThrows
    void testNegativeExecution()
    {
        Mockito.when(baseCommand.executeCommand(Mockito.any())).thenReturn(CompletableFuture.completedFuture(false));
        final var result = baseCommand.run();
        Assertions.assertFalse(result.get(1, TimeUnit.SECONDS));
    }

    @Test
    @SneakyThrows
    void invalidInput()
    {
        Mockito.when(baseCommand.validInput()).thenReturn(false);
        final var result = baseCommand.run();
        Assertions.assertFalse(result.get(1, TimeUnit.SECONDS));
    }

    @Test
    @SneakyThrows
    void testPermissionFailure()
    {
        Mockito.when(baseCommand.executeCommand(Mockito.any())).thenReturn(CompletableFuture.completedFuture(true));
        Mockito.when(commandSender.hasPermission(Mockito.any(CommandDefinition.class)))
               .thenReturn(CompletableFuture.completedFuture(new BooleanPair(false, false)));

        final var result = baseCommand.run();
        Assertions.assertTrue(result.get(1, TimeUnit.SECONDS));
    }

    @Test
    void testExceptionPermission()
    {
        logger.setConsoleLogLevel(Level.OFF);

        Mockito.when(baseCommand.executeCommand(Mockito.any())).thenReturn(CompletableFuture.completedFuture(true));

        final CompletableFuture<BooleanPair> exceptional = new CompletableFuture<>();
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
        logger.setConsoleLogLevel(Level.OFF);

        Mockito.when(baseCommand.executeCommand(Mockito.any())).thenReturn(CompletableFuture.completedFuture(true));
        final CompletableFuture<Boolean> exceptional = new CompletableFuture<>();
        exceptional.completeExceptionally(new IllegalStateException("Testing exception!"));

        Mockito.when(baseCommand.executeCommand(Mockito.any(BooleanPair.class))).thenReturn(exceptional);

        ExecutionException exception =
            Assertions.assertThrows(ExecutionException.class,
                                    () -> baseCommand.startExecution().get(1, TimeUnit.SECONDS));
        Assertions.assertEquals(IllegalStateException.class, exception.getCause().getCause().getCause().getClass());
    }

    @SneakyThrows
    private static void initBaseCommand(BaseCommand baseCommand, ICommandSender commandSender, IPLogger logger,
                                        ILocalizer localizer, CompletableFutureHandler handler)
    {
        UnitTestUtil.setField(BaseCommand.class, baseCommand, "commandSender", commandSender);
        UnitTestUtil.setField(BaseCommand.class, baseCommand, "logger", logger);
        UnitTestUtil.setField(BaseCommand.class, baseCommand, "localizer", localizer);
        UnitTestUtil.setField(BaseCommand.class, baseCommand, "handler", handler);
    }
}
