package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.managers.DelayedCommandInputManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.testing.AssistedFactoryMocker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.inject.Provider;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SetBlocksToMoveDelayedTest
{
    @Spy
    private DelayedCommandInputManager delayedCommandInputManager =
        new DelayedCommandInputManager(mock(DebuggableRegistry.class));

    @Mock
    private IExecutor executor;

    private DelayedCommandInputRequest.IFactory<Integer> inputRequestFactory;

    @InjectMocks
    private DelayedCommand.Context context;

    @Mock
    private CommandFactory commandFactory;

    @SuppressWarnings({"unused", "unchecked"})
    private final Provider<CommandFactory> commandFactoryProvider =
        mock(Provider.class, delegatesTo((Provider<CommandFactory>) () -> commandFactory));

    @Mock
    private ICommandSender commandSender;

    private StructureRetriever structureRetriever;

    @Mock
    private Structure structure;

    @InjectMocks
    private StructureRetrieverFactory structureRetrieverFactory;

    @Mock
    private SetBlocksToMove setBlocksToMove;

    @BeforeEach
    void init()
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        //noinspection unchecked
        inputRequestFactory = AssistedFactoryMocker
            .injectMocksFromTestClass(DelayedCommandInputRequest.IFactory.class, this)
            .injectParameter(delayedCommandInputManager)
            .getFactory();

        structureRetriever = structureRetrieverFactory.of(structure);

        when(setBlocksToMove.run()).thenReturn(CompletableFuture.completedFuture(null));

        when(commandFactory
            .newSetBlocksToMove(any(), any(), anyInt()))
            .thenReturn(setBlocksToMove);
    }

    @Test
    void normal()
    {
        final SetBlocksToMoveDelayed setBlocksToMoveDelayed = new SetBlocksToMoveDelayed(context, inputRequestFactory);
        UnitTestUtil.initMessageable(commandSender);

        final CompletableFuture<?> result0 = setBlocksToMoveDelayed.runDelayed(commandSender, structureRetriever);
        final CompletableFuture<?> result1 = setBlocksToMoveDelayed.provideDelayedInput(commandSender, 10);

        assertDoesNotThrow(() -> result0.get(1, TimeUnit.SECONDS));
        assertDoesNotThrow(() -> result1.get(1, TimeUnit.SECONDS));

        verify(commandFactory, times(1)).newSetBlocksToMove(commandSender, structureRetriever, 10);
    }
}
