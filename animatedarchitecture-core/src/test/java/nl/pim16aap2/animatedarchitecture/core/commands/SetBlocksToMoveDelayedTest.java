package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.DelayedCommandInputManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.inject.Provider;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.AdditionalAnswers.delegatesTo;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SetBlocksToMoveDelayedTest
{
    @Spy
    DelayedCommandInputManager delayedCommandInputManager =
        new DelayedCommandInputManager(Mockito.mock(DebuggableRegistry.class));

    ILocalizer localizer = UnitTestUtil.initLocalizer();

    @Mock
    DelayedCommandInputRequest.IFactory<Integer> inputRequestFactory;

    @InjectMocks
    DelayedCommand.Context context;

    @Mock
    CommandFactory commandFactory;
    @SuppressWarnings({"unused", "unchecked"})
    Provider<CommandFactory> commandFactoryProvider =
        Mockito.mock(Provider.class, delegatesTo((Provider<CommandFactory>) () -> commandFactory));

    @Mock
    ICommandSender commandSender;

    StructureRetriever structureRetriever;

    @Mock
    Structure structure;

    @InjectMocks
    StructureRetrieverFactory structureRetrieverFactory;

    @Mock
    SetBlocksToMove setBlocksToMove;

    @BeforeEach
    void init()
    {
        DelayedCommandTest.initInputRequestFactory(inputRequestFactory, localizer, delayedCommandInputManager);

        structureRetriever = structureRetrieverFactory.of(structure);

        Mockito.when(setBlocksToMove.run()).thenReturn(CompletableFuture.completedFuture(null));

        Mockito.when(commandFactory.newSetBlocksToMove(Mockito.any(), Mockito.any(), Mockito.anyInt()))
            .thenReturn(setBlocksToMove);
    }

    @Test
    void normal()
    {
        final SetBlocksToMoveDelayed setBlocksToMoveDelayed = new SetBlocksToMoveDelayed(context, inputRequestFactory);

        final CompletableFuture<?> result0 = setBlocksToMoveDelayed.runDelayed(commandSender, structureRetriever);
        final CompletableFuture<?> result1 = setBlocksToMoveDelayed.provideDelayedInput(commandSender, 10);

        Assertions.assertDoesNotThrow(() -> result0.get(1, TimeUnit.SECONDS));
        Assertions.assertDoesNotThrow(() -> result1.get(1, TimeUnit.SECONDS));

        Mockito.verify(commandFactory, Mockito.times(1)).newSetBlocksToMove(commandSender, structureRetriever, 10);
    }
}
