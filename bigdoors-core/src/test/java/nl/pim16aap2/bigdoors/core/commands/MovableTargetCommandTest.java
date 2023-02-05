package nl.pim16aap2.bigdoors.core.commands;

import nl.pim16aap2.bigdoors.core.UnitTestUtil;
import nl.pim16aap2.bigdoors.core.api.IPPlayer;
import nl.pim16aap2.bigdoors.core.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.core.localization.ILocalizer;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.structuretypes.StructureType;
import nl.pim16aap2.bigdoors.core.util.structureretriever.StructureRetrieverFactory;
import nl.pim16aap2.testing.AssertionsUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Timeout(1)
class StructureTargetCommandTest
{
    @Mock
    private AbstractStructure structure;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private StructureTargetCommand structureTargetCommand;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, true);
        Mockito.when(structure.isOwner(Mockito.any(UUID.class))).thenReturn(true);
        Mockito.when(structure.isOwner(Mockito.any(IPPlayer.class))).thenReturn(true);

        final StructureType structureType = Mockito.mock(StructureType.class);
        Mockito.when(structureType.getLocalizationKey()).thenReturn("StructureType");
        Mockito.when(structure.getType()).thenReturn(structureType);

        Mockito.doReturn(true).when(structureTargetCommand).isAllowed(Mockito.any(), Mockito.anyBoolean());
        Mockito.when(structureTargetCommand.performAction(Mockito.any()))
               .thenReturn(CompletableFuture.completedFuture(null));

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        UnitTestUtil.setField(StructureTargetCommand.class, structureTargetCommand, "structureRetriever",
                              StructureRetrieverFactory.ofStructure(structure));

        UnitTestUtil.setField(StructureTargetCommand.class, structureTargetCommand, "lock",
                              new ReentrantReadWriteLock());
        UnitTestUtil.setField(BaseCommand.class, structureTargetCommand, "commandSender", commandSender);
        UnitTestUtil.setField(BaseCommand.class, structureTargetCommand, "localizer", localizer);
        UnitTestUtil.setField(BaseCommand.class, structureTargetCommand, "textFactory",
                              ITextFactory.getSimpleTextFactory());
    }

    @Test
    void testExecutionSuccess()
    {
        Assertions.assertDoesNotThrow(() -> structureTargetCommand.executeCommand(new PermissionsStatus(true, true))
                                                                  .get(1, TimeUnit.SECONDS));
        Mockito.verify(structureTargetCommand).performAction(Mockito.any());
    }

    @Test
    void testExecutionFailureNoStructure()
    {
        Mockito.when(structure.isOwner(Mockito.any(UUID.class))).thenReturn(false);
        Mockito.when(structure.isOwner(Mockito.any(IPPlayer.class))).thenReturn(false);

        Assertions.assertDoesNotThrow(() -> structureTargetCommand.executeCommand(new PermissionsStatus(true, true))
                                                                  .get(1, TimeUnit.SECONDS));
    }

    @Test
    void testExecutionFailureNoPermission()
    {
        Mockito.doReturn(false).when(structureTargetCommand).isAllowed(Mockito.any(), Mockito.anyBoolean());

        Assertions.assertDoesNotThrow(() -> structureTargetCommand.executeCommand(new PermissionsStatus(true, true))
                                                                  .get(1, TimeUnit.SECONDS));
        Mockito.verify(structureTargetCommand, Mockito.never()).performAction(Mockito.any());
    }

    @Test
    void testPerformActionFailure()
    {
        Mockito.when(structureTargetCommand.performAction(Mockito.any()))
               .thenThrow(new IllegalStateException("Generic Exception!"));

        AssertionsUtil.assertThrowablesLogged(
            () -> structureTargetCommand.executeCommand(new PermissionsStatus(true, true)).get(1, TimeUnit.SECONDS),
            // Thrown by the structureTargetCommand CompletableFuture's exception handler (via Util).
            CompletionException.class,
            // Thrown when the command action failed.
            RuntimeException.class,
            // The root exception we threw; the "Generic Exception!".
            IllegalStateException.class);
    }
}
