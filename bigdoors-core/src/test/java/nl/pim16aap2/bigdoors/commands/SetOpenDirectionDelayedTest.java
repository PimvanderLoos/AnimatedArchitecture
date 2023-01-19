package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DelayedCommandInputManager;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import javax.inject.Provider;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.commands.DelayedCommandTest.initInputRequestFactory;
import static org.mockito.AdditionalAnswers.delegatesTo;

@Timeout(1)
@SuppressWarnings("unused")
class SetOpenDirectionDelayedTest
{
    @Spy
    DelayedCommandInputManager delayedCommandInputManager =
        new DelayedCommandInputManager(Mockito.mock(DebuggableRegistry.class));

    ILocalizer localizer = UnitTestUtil.initLocalizer();

    @Mock
    DelayedCommandInputRequest.IFactory<RotateDirection> inputRequestFactory;

    @InjectMocks
    DelayedCommand.Context context;

    @Mock
    CommandFactory commandFactory;

    @SuppressWarnings("unchecked")
    Provider<CommandFactory> commandFactoryProvider =
        Mockito.mock(Provider.class, delegatesTo((Provider<CommandFactory>) () -> commandFactory));

    @Mock
    ICommandSender commandSender;

    MovableRetriever movableRetriever;

    @Mock
    AbstractMovable movable;

    @InjectMocks
    MovableRetrieverFactory movableRetrieverFactory;

    @Mock
    SetOpenDirection setOpenDirection;

    AutoCloseable openMocks;

    @BeforeEach
    void init()
    {
        openMocks = MockitoAnnotations.openMocks(this);

        initInputRequestFactory(inputRequestFactory, localizer, delayedCommandInputManager);

        movableRetriever = movableRetrieverFactory.of(movable);

        Mockito.when(setOpenDirection.run()).thenReturn(CompletableFuture.completedFuture(null));

        Mockito.when(commandFactory.newSetOpenDirection(Mockito.any(), Mockito.any(), Mockito.any()))
               .thenReturn(setOpenDirection);
    }

    @AfterEach
    void cleanup()
        throws Exception
    {
        openMocks.close();
    }

    @Test
    void normal()
    {
        final SetOpenDirectionDelayed setOpenDirectionDelayed =
            new SetOpenDirectionDelayed(context, inputRequestFactory);

        final CompletableFuture<?> result0 = setOpenDirectionDelayed.runDelayed(commandSender, movableRetriever);
        final CompletableFuture<?> result1 =
            setOpenDirectionDelayed.provideDelayedInput(commandSender, RotateDirection.UP);

        Assertions.assertDoesNotThrow(() -> result0.get(1, TimeUnit.SECONDS));
        Assertions.assertDoesNotThrow(() -> result1.get(1, TimeUnit.SECONDS));

        Mockito.verify(commandFactory, Mockito.times(1))
               .newSetOpenDirection(commandSender, movableRetriever, RotateDirection.UP);
    }
}
