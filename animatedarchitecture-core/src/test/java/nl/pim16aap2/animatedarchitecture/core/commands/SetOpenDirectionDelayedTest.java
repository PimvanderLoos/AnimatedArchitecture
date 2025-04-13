package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.managers.DelayedCommandInputManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

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
class SetOpenDirectionDelayedTest
{
    @Spy
    private DelayedCommandInputManager delayedCommandInputManager =
        new DelayedCommandInputManager(mock(DebuggableRegistry.class));

    @Mock
    private IExecutor executor;

    @Mock
    private DelayedCommandInputRequest.IFactory<MovementDirection> inputRequestFactory;

    @InjectMocks
    private DelayedCommand.Context context;

    @Mock
    private CommandFactory commandFactory;

    @SuppressWarnings({"unchecked", "unused"})
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
    private SetOpenDirection setOpenDirection;

    @BeforeEach
    void init()
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        DelayedCommandTest.initInputRequestFactory(
            inputRequestFactory,
            executor,
            delayedCommandInputManager
        );

        structureRetriever = structureRetrieverFactory.of(structure);

        when(setOpenDirection.run()).thenReturn(CompletableFuture.completedFuture(null));
        when(commandFactory.newSetOpenDirection(any(), any(), any())).thenReturn(setOpenDirection);
    }

    @Test
    void normal()
    {
        final var setOpenDirectionDelayed = new SetOpenDirectionDelayed(context, inputRequestFactory);
        UnitTestUtil.initMessageable(commandSender);

        final CompletableFuture<?> result0 = setOpenDirectionDelayed.runDelayed(commandSender, structureRetriever);
        final CompletableFuture<?> result1 =
            setOpenDirectionDelayed.provideDelayedInput(commandSender, MovementDirection.UP);

        assertDoesNotThrow(() -> result0.get(1, TimeUnit.SECONDS));
        assertDoesNotThrow(() -> result1.get(1, TimeUnit.SECONDS));

        verify(commandFactory, times(1))
            .newSetOpenDirection(commandSender, structureRetriever, MovementDirection.UP);
    }
}
