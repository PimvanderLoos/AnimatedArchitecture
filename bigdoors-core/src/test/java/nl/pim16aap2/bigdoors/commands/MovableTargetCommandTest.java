package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;
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
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;

class MovableTargetCommandTest
{
    @Mock
    private AbstractMovable movable;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private MovableTargetCommand movableTargetCommand;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        initCommandSenderPermissions(commandSender, true, true);
        Mockito.when(movable.isOwner(Mockito.any(UUID.class))).thenReturn(true);
        Mockito.when(movable.isOwner(Mockito.any(IPPlayer.class))).thenReturn(true);

        final MovableType movableType = Mockito.mock(MovableType.class);
        Mockito.when(movableType.getLocalizationKey()).thenReturn("MovableType");
        Mockito.when(movable.getType()).thenReturn(movableType);

        Mockito.doReturn(true).when(movableTargetCommand).isAllowed(Mockito.any(), Mockito.anyBoolean());
        Mockito.when(movableTargetCommand.performAction(Mockito.any()))
               .thenReturn(CompletableFuture.completedFuture(true));

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        UnitTestUtil.setField(MovableTargetCommand.class, movableTargetCommand, "movableRetriever",
                              MovableRetrieverFactory.ofMovable(movable));

        UnitTestUtil.setField(MovableTargetCommand.class, movableTargetCommand, "lock", new ReentrantReadWriteLock());
        UnitTestUtil.setField(BaseCommand.class, movableTargetCommand, "commandSender", commandSender);
        UnitTestUtil.setField(BaseCommand.class, movableTargetCommand, "localizer", localizer);
        UnitTestUtil.setField(BaseCommand.class, movableTargetCommand, "textFactory",
                              ITextFactory.getSimpleTextFactory());
    }

    @Test
    void testExecutionSuccess()
        throws Exception
    {
        Assertions.assertTrue(movableTargetCommand.executeCommand(new PermissionsStatus(true, true))
                                                  .get(1, TimeUnit.SECONDS));
        Mockito.verify(movableTargetCommand).performAction(Mockito.any());
    }

    @Test
    void testExecutionFailureNoMovable()
        throws Exception
    {
        Mockito.when(movable.isOwner(Mockito.any(UUID.class))).thenReturn(false);
        Mockito.when(movable.isOwner(Mockito.any(IPPlayer.class))).thenReturn(false);

        Assertions.assertFalse(movableTargetCommand.executeCommand(new PermissionsStatus(true, true))
                                                   .get(1, TimeUnit.SECONDS));
    }

    @Test
    void testExecutionFailureNoPermission()
        throws Exception
    {
        Mockito.doReturn(false).when(movableTargetCommand).isAllowed(Mockito.any(), Mockito.anyBoolean());

        Assertions.assertTrue(movableTargetCommand.executeCommand(new PermissionsStatus(true, true))
                                                  .get(1, TimeUnit.SECONDS));
        Mockito.verify(movableTargetCommand, Mockito.never()).performAction(Mockito.any());
    }

    @Test
    void testPerformActionFailure()
        throws Exception
    {
        Mockito.when(movableTargetCommand.performAction(Mockito.any()))
               .thenThrow(new IllegalStateException("Generic Exception!"));

        AssertionsUtil.assertThrowablesLogged(
            () -> movableTargetCommand.executeCommand(new PermissionsStatus(true, true)).get(1, TimeUnit.SECONDS),
            // Thrown by the movableTargetCommand CompletableFuture's exception handler (via Util).
            CompletionException.class,
            // Thrown when the command action failed.
            RuntimeException.class,
            // The root exception we threw; the "Generic Exception!".
            IllegalStateException.class);
    }
}
