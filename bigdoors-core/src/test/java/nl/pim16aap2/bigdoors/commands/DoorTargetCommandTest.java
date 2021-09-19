package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;

class DoorTargetCommandTest
{
    @Mock
    private AbstractDoor door;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private DoorTargetCommand doorTargetCommand;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        initCommandSenderPermissions(commandSender, true, true);
        Mockito.when(door.isDoorOwner(Mockito.any(UUID.class))).thenReturn(true);
        Mockito.when(door.isDoorOwner(Mockito.any(IPPlayer.class))).thenReturn(true);

        Mockito.doReturn(true).when(doorTargetCommand).isAllowed(Mockito.any(), Mockito.anyBoolean());
        Mockito.when(doorTargetCommand.performAction(Mockito.any()))
               .thenReturn(CompletableFuture.completedFuture(true));

        final IPLogger logger = new BasicPLogger();
        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        UnitTestUtil.setField(DoorTargetCommand.class, doorTargetCommand, "doorRetriever", DoorRetriever.ofDoor(door));

        UnitTestUtil.setField(BaseCommand.class, doorTargetCommand, "commandSender", commandSender);
        UnitTestUtil.setField(BaseCommand.class, doorTargetCommand, "logger", logger);
        UnitTestUtil.setField(BaseCommand.class, doorTargetCommand, "localizer", localizer);
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
        Mockito.when(door.isDoorOwner(Mockito.any(UUID.class))).thenReturn(false);
        Mockito.when(door.isDoorOwner(Mockito.any(IPPlayer.class))).thenReturn(false);

        Assertions.assertFalse(doorTargetCommand.executeCommand(new BooleanPair(true, true))
                                                .get(1, TimeUnit.SECONDS));
    }

    @Test
    @SneakyThrows
    void testExecutionFailureNoPermission()
    {
        Mockito.doReturn(false).when(doorTargetCommand).isAllowed(Mockito.any(), Mockito.anyBoolean());

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
