package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
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

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.*;

class DoorTargetCommandTest
{
    private IBigDoorsPlatform platform;

    @Mock
    private DoorRetriever doorRetriever;

    @Mock
    private AbstractDoorBase door;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private DoorTargetCommand doorTargetCommand;

    @Mock
    private ToolUserManager toolUserManager;

    @BeforeEach
    void init()
    {
        platform = initPlatform();
        MockitoAnnotations.openMocks(this);

        initCommandSenderPermissions(commandSender, true, true);
        initDoorRetriever(doorRetriever, door);

        Mockito.when(doorTargetCommand.getDoorRetriever()).thenReturn(doorRetriever);
        Mockito.when(doorTargetCommand.isAllowed(Mockito.any(), Mockito.anyBoolean())).thenReturn(true);
        Mockito.when(doorTargetCommand.getCommandSender()).thenReturn(commandSender);
        Mockito.when(doorTargetCommand.performAction(Mockito.any()))
               .thenReturn(CompletableFuture.completedFuture(true));
    }


    @Test
    @SneakyThrows
    void testExecutionSuccess()
    {
        Assertions.assertTrue(doorTargetCommand.executeCommand(new BooleanPair(true, true))
                                               .get(1, TimeUnit.SECONDS));
        Mockito.verify(doorTargetCommand).performAction(Mockito.any());
    }

    @Test
    @SneakyThrows
    void testExecutionFailureNoDoor()
    {
        Mockito.when(doorRetriever.getDoorInteractive(Mockito.any()))
               .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        Assertions.assertFalse(doorTargetCommand.executeCommand(new BooleanPair(true, true))
                                                .get(1, TimeUnit.SECONDS));
    }

    @Test
    @SneakyThrows
    void testExecutionFailureNoPermission()
    {
        Mockito.when(doorTargetCommand.isAllowed(Mockito.any(), Mockito.anyBoolean())).thenReturn(false);

        Assertions.assertTrue(doorTargetCommand.executeCommand(new BooleanPair(true, true))
                                               .get(1, TimeUnit.SECONDS));
        Mockito.verify(doorTargetCommand, Mockito.never()).performAction(Mockito.any());
    }

    @Test
    @SneakyThrows
    void testPerformActionFailure()
    {
        Mockito.when(doorTargetCommand.performAction(Mockito.any()))
               .thenThrow(new IllegalStateException("Generic Exception!"));

        ExecutionException exception = Assertions.assertThrows(ExecutionException.class, () ->
            doorTargetCommand.executeCommand(new BooleanPair(true, true)).get(1, TimeUnit.SECONDS));

        // "We need to go deeper"
        Assertions.assertEquals(IllegalStateException.class,
                                exception.getCause().getCause().getCause().getCause().getClass());
    }
}
