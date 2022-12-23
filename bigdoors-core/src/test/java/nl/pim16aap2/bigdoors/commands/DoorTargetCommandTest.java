package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;
import nl.pim16aap2.testing.AssertionsUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        UnitTestUtil.setField(DoorTargetCommand.class, doorTargetCommand, "doorRetriever",
                              DoorRetrieverFactory.ofDoor(door));

        UnitTestUtil.setField(BaseCommand.class, doorTargetCommand, "commandSender", commandSender);
        UnitTestUtil.setField(BaseCommand.class, doorTargetCommand, "localizer", localizer);
        UnitTestUtil.setField(BaseCommand.class, doorTargetCommand, "textFactory", ITextFactory.getSimpleTextFactory());
    }

    @Test
    void testExecutionSuccess()
        throws Exception
    {
        Assertions.assertTrue(doorTargetCommand.executeCommand(new PermissionsStatus(true, true))
                                               .get(1, TimeUnit.SECONDS));
        Mockito.verify(doorTargetCommand).performAction(Mockito.any());
    }

    @Test
    void testExecutionFailureNoDoor()
        throws Exception
    {
        Mockito.when(door.isDoorOwner(Mockito.any(UUID.class))).thenReturn(false);
        Mockito.when(door.isDoorOwner(Mockito.any(IPPlayer.class))).thenReturn(false);

        Assertions.assertFalse(doorTargetCommand.executeCommand(new PermissionsStatus(true, true))
                                                .get(1, TimeUnit.SECONDS));
    }

    @Test
    void testExecutionFailureNoPermission()
        throws Exception
    {
        Mockito.doReturn(false).when(doorTargetCommand).isAllowed(Mockito.any(), Mockito.anyBoolean());

        Assertions.assertTrue(doorTargetCommand.executeCommand(new PermissionsStatus(true, true))
                                               .get(1, TimeUnit.SECONDS));
        Mockito.verify(doorTargetCommand, Mockito.never()).performAction(Mockito.any());
    }

    @Test
    void testPerformActionFailure()
        throws Exception
    {
        Mockito.when(doorTargetCommand.performAction(Mockito.any()))
               .thenThrow(new IllegalStateException("Generic Exception!"));

        AssertionsUtil.assertThrowablesLogged(
            () -> doorTargetCommand.executeCommand(new PermissionsStatus(true, true)).get(1, TimeUnit.SECONDS),
            // Thrown by the doorTargetCommand CompletableFuture's exception handler (via Util).
            CompletionException.class,
            // Thrown when the command action failed.
            RuntimeException.class,
            // The root exception we threw; the "Generic Exception!".
            IllegalStateException.class);
    }
}
