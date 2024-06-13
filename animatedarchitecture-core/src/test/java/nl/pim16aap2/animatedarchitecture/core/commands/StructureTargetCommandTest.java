package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.altindag.log.LogCaptor;
import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.testing.logging.LogAssertionsUtil;
import nl.pim16aap2.testing.logging.WithLogCapture;
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

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Timeout(1)
@WithLogCapture
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StructureTargetCommandTest
{
    @Mock
    private AbstractStructure structure;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer commandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private StructureTargetCommand structureTargetCommand;

    private final StructureAttribute structureAttribute = StructureAttribute.INFO;

    @BeforeEach
    void init()
    {
        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, true);
        Mockito.when(structure.isOwner(Mockito.any(UUID.class), Mockito.any())).thenReturn(true);
        Mockito.when(structure.isOwner(Mockito.any(IPlayer.class), Mockito.any())).thenReturn(true);

        final StructureType structureType = Mockito.mock(StructureType.class);
        Mockito.when(structureType.getLocalizationKey()).thenReturn("StructureType");
        Mockito.when(structure.getType()).thenReturn(structureType);

        Mockito.doReturn(true).when(structureTargetCommand).isAllowed(Mockito.any(), Mockito.anyBoolean());
        Mockito.when(structureTargetCommand.performAction(Mockito.any()))
            .thenReturn(CompletableFuture.completedFuture(null));

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        UnitTestUtil.setField(
            StructureTargetCommand.class, structureTargetCommand, "structureRetriever",
            StructureRetrieverFactory.ofStructure(structure));
        UnitTestUtil.setField(
            StructureTargetCommand.class, structureTargetCommand, "structureAttribute", structureAttribute);
        UnitTestUtil.setField(
            StructureTargetCommand.class, structureTargetCommand, "$lock", new ReentrantReadWriteLock());

        UnitTestUtil.setField(BaseCommand.class, structureTargetCommand, "commandSender", commandSender);
        UnitTestUtil.setField(BaseCommand.class, structureTargetCommand, "localizer", localizer);
        UnitTestUtil.setField(
            BaseCommand.class,
            structureTargetCommand,
            "textFactory",
            ITextFactory.getSimpleTextFactory()
        );
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
        Mockito.when(structure.isOwner(Mockito.any(UUID.class), Mockito.any())).thenReturn(false);
        Mockito.when(structure.isOwner(Mockito.any(IPlayer.class), Mockito.any())).thenReturn(false);

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
    void testPerformActionFailure(LogCaptor logCaptor)
        throws ExecutionException, InterruptedException, TimeoutException
    {
        Mockito.when(structureTargetCommand.performAction(Mockito.any()))
            .thenThrow(new IllegalStateException("Generic Exception!"));

        structureTargetCommand.executeCommand(new PermissionsStatus(true, true)).get(1, TimeUnit.SECONDS);

        LogAssertionsUtil.assertThrowableLogged(
            logCaptor,
            -1,
            null,
            new LogAssertionsUtil.ThrowableSpec(CompletionException.class),
            new LogAssertionsUtil.ThrowableSpec(
                RuntimeException.class,
                "Failed to perform command BaseCommand" +
                    "(commandSender=nl.pim16aap2.animatedarchitecture.core.api.IPlayer",
                LogAssertionsUtil.MessageComparisonMethod.STARTS_WITH
            ),
            new LogAssertionsUtil.ThrowableSpec(
                IllegalStateException.class,
                "Generic Exception!"
            )
        );
    }
}
