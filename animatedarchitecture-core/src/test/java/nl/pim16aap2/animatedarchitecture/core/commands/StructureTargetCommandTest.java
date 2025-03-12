//package nl.pim16aap2.animatedarchitecture.core.commands;
//
//import nl.altindag.log.LogCaptor;
//import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
//import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
//import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
//import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
//import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
//import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
//import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
//import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
//import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
//import nl.pim16aap2.testing.logging.LogAssertionsUtil;
//import nl.pim16aap2.testing.logging.WithLogCapture;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.Timeout;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Answers;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.mockito.junit.jupiter.MockitoSettings;
//import org.mockito.quality.Strictness;
//
//import java.util.UUID;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.CompletionException;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.TimeoutException;
//import java.util.concurrent.locks.ReentrantReadWriteLock;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@Timeout(1)
//@WithLogCapture
//@ExtendWith(MockitoExtension.class)
//@MockitoSettings(strictness = Strictness.LENIENT)
//class StructureTargetCommandTest
//{
//    @Mock
//    private Structure structure;
//
//    @Mock(answer = Answers.CALLS_REAL_METHODS)
//    private IPlayer commandSender;
//
//    @Mock
//    private IExecutor executor;
//
//    private StructureTargetCommand structureTargetCommand;
//
//    private final StructureAttribute structureAttribute = StructureAttribute.INFO;
//
//    @BeforeEach
//    void init()
//    {
//        CommandTestingUtil.initCommandSenderPermissions(commandSender, true, true);
//        when(structure.isOwner(any(UUID.class), any())).thenReturn(true);
//        when(structure.isOwner(any(IPlayer.class), any())).thenReturn(true);
//
//        final StructureType structureType = mock();
//        when(structureType.getLocalizationKey()).thenReturn("StructureType");
//        when(structure.getType()).thenReturn(structureType);
//
//        doReturn(true).when(structureTargetCommand).isAllowed(any(), anyBoolean());
//        when(structureTargetCommand
//            .performAction(any()))
//            .thenReturn(CompletableFuture.completedFuture(null));
//
//        final ILocalizer localizer = UnitTestUtil.initLocalizer();
//
//        UnitTestUtil.setField(
//            StructureTargetCommand.class,
//            structureTargetCommand,
//            "structureRetriever",
//            StructureRetrieverFactory.ofStructure(structure)
//        );
//
//        UnitTestUtil.setField(
//            StructureTargetCommand.class,
//            structureTargetCommand,
//            "structureAttribute",
//            structureAttribute
//        );
//
//        UnitTestUtil.setField(
//            StructureTargetCommand.class,
//            structureTargetCommand,
//            "$lock",
//            new ReentrantReadWriteLock()
//        );
//
//        UnitTestUtil.setField(
//            BaseCommand.class,
//            structureTargetCommand,
//            "commandSender",
//            commandSender
//        );
//
//        UnitTestUtil.setField(
//            BaseCommand.class,
//            structureTargetCommand,
//            "localizer",
//            localizer
//        );
//
//        UnitTestUtil.setField(
//            BaseCommand.class,
//            structureTargetCommand,
//            "textFactory",
//            ITextFactory.getSimpleTextFactory()
//        );
//    }
//
//    @Test
//    void testExecutionSuccess()
//    {
//        assertDoesNotThrow(
//            () -> structureTargetCommand.executeCommand(new PermissionsStatus(true, true)).get(1, TimeUnit.SECONDS));
//        verify(structureTargetCommand).performAction(any());
//    }
//
//    @Test
//    void testExecutionFailureNoStructure()
//    {
//        when(structure.isOwner(any(UUID.class), any())).thenReturn(false);
//        when(structure.isOwner(any(IPlayer.class), any())).thenReturn(false);
//
//        assertDoesNotThrow(
//            () -> structureTargetCommand.executeCommand(new PermissionsStatus(true, true)).get(1, TimeUnit.SECONDS));
//    }
//
//    @Test
//    void testExecutionFailureNoPermission()
//    {
//        doReturn(false).when(structureTargetCommand).isAllowed(any(), anyBoolean());
//
//        assertDoesNotThrow(
//            () -> structureTargetCommand.executeCommand(new PermissionsStatus(true, true)).get(1, TimeUnit.SECONDS));
//
//        verify(structureTargetCommand, never()).performAction(any());
//    }
//
//    @Test
//    void testPerformActionFailure(LogCaptor logCaptor)
//        throws ExecutionException, InterruptedException, TimeoutException
//    {
//        when(structureTargetCommand
//            .performAction(any()))
//            .thenThrow(new IllegalStateException("Generic Exception!"));
//
//        structureTargetCommand.executeCommand(new PermissionsStatus(true, true)).get(1, TimeUnit.SECONDS);
//
//        LogAssertionsUtil.assertThrowableLogged(
//            logCaptor,
//            -1,
//            null,
//            new LogAssertionsUtil.ThrowableSpec(CompletionException.class),
//            new LogAssertionsUtil.ThrowableSpec(
//                RuntimeException.class,
//                "Failed to perform command BaseCommand" +
//                    "(commandSender=nl.pim16aap2.animatedarchitecture.core.api.IPlayer",
//                LogAssertionsUtil.MessageComparisonMethod.STARTS_WITH
//            ),
//            new LogAssertionsUtil.ThrowableSpec(
//                IllegalStateException.class,
//                "Generic Exception!"
//            )
//        );
//    }
//}
