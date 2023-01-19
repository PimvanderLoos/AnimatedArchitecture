package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DelayedCommandInputManager;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.util.functional.TriFunction;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;
import nl.pim16aap2.testing.logging.LogInspector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import javax.inject.Provider;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.AdditionalAnswers.delegatesTo;

@Timeout(1)
@SuppressWarnings("unused")
class DelayedCommandTest
{
    CommandFactory commandFactory = Mockito.mock(CommandFactory.class);

    @SuppressWarnings("unchecked")
    Provider<CommandFactory> commandFactoryProvider =
        Mockito.mock(Provider.class, delegatesTo((Provider<CommandFactory>) () -> commandFactory));

    @Spy
    DelayedCommandInputManager delayedCommandInputManager = new DelayedCommandInputManager(
        Mockito.mock(DebuggableRegistry.class));

    ILocalizer localizer = UnitTestUtil.initLocalizer();

    @Mock
    DelayedCommandInputRequest.IFactory<Object> inputRequestFactory;

    @Spy
    ITextFactory textFactory = ITextFactory.getSimpleTextFactory();

    @InjectMocks
    DelayedCommand.Context context;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    ICommandSender commandSender;

    MovableRetriever movableRetriever;

    @Mock
    AbstractMovable movable;

    @InjectMocks
    MovableRetrieverFactory movableRetrieverFactory;

    @Mock
    TriFunction<ICommandSender, MovableRetriever, Object, CompletableFuture<Boolean>> delayedFunction;

    AutoCloseable openMocks;

    @BeforeEach
    void init()
    {
        openMocks = MockitoAnnotations.openMocks(this);
        initInputRequestFactory(inputRequestFactory, localizer, delayedCommandInputManager);
        movableRetriever = movableRetrieverFactory.of(movable);
        LogInspector.get().clearHistory();
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
        final DelayedCommandImpl delayedCommand = new DelayedCommandImpl(context, inputRequestFactory, delayedFunction);

        delayedCommand.runDelayed(commandSender, movableRetriever);
        Mockito.verify(commandSender, Mockito.times(1))
               .sendMessage(UnitTestUtil.toText(DelayedCommandImpl.INPUT_REQUEST_MSG));
        Mockito.verify(inputRequestFactory, Mockito.times(1)).create(
            Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(delayedFunction, Mockito.never()).apply(Mockito.any(), Mockito.any(), Mockito.any());

        final Object input = new Object();
        delayedCommand.provideDelayedInput(commandSender, input).join();
        Mockito.verify(delayedFunction, Mockito.times(1)).apply(commandSender, movableRetriever, input);

        delayedCommand.provideDelayedInput(commandSender, new Object());
        Mockito.verify(commandSender, Mockito.times(1))
               .sendMessage(UnitTestUtil.toText("commands.base.error.not_waiting"));
    }

    @Test
    void notWaiting()
    {
        final DelayedCommandImpl delayedCommand = new DelayedCommandImpl(context, inputRequestFactory, delayedFunction);

        delayedCommand.provideDelayedInput(commandSender, new Object());
        Mockito.verify(commandSender, Mockito.times(1))
               .sendMessage(UnitTestUtil.toText("commands.base.error.not_waiting"));
    }

    @Test
    void exception()
    {
        Mockito.when(delayedFunction.apply(Mockito.any(), Mockito.any(), Mockito.any()))
               .thenThrow(RuntimeException.class);
        final DelayedCommandImpl delayedCommand = new DelayedCommandImpl(context, inputRequestFactory, delayedFunction);
        delayedCommand.runDelayed(commandSender, movableRetriever);
        Assertions.assertEquals(0, LogInspector.get().getThrowingCount());

        Assertions.assertDoesNotThrow(
            () -> delayedCommand.provideDelayedInput(commandSender, new Object()).get(1, TimeUnit.SECONDS));
        Assertions.assertEquals(1, LogInspector.get().getThrowingCount());
    }

    /**
     * Initializes the input request factory such that the creation method returns a new request using the provided
     * input.
     */
    public static <T> void initInputRequestFactory(
        DelayedCommandInputRequest.IFactory<T> inputRequestFactory, ILocalizer localizer,
        DelayedCommandInputManager delayedCommandInputManager)
    {
        Mockito.when(inputRequestFactory.create(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any(),
                                                Mockito.any(), Mockito.any())).thenAnswer(
            invocation -> new DelayedCommandInputRequest<T>(
                invocation.getArgument(0, Long.class),
                invocation.getArgument(1, ICommandSender.class),
                invocation.getArgument(2, CommandDefinition.class),
                invocation.getArgument(3),
                invocation.getArgument(4),
                invocation.getArgument(5),
                localizer,
                ITextFactory.getSimpleTextFactory(),
                delayedCommandInputManager));
    }

    static final class DelayedCommandImpl extends DelayedCommand<Object>
    {
        public static final CommandDefinition COMMAND_DEFINITION =
            new CommandDefinition("DelayedCommandImpl", null, null);

        public static final String INPUT_REQUEST_MSG = "DelayedCommandImpl INPUT REQUEST MSG";

        private final TriFunction<ICommandSender, MovableRetriever, Object, CompletableFuture<Boolean>> delayedFunction;

        DelayedCommandImpl(
            Context context, DelayedCommandInputRequest.IFactory<Object> inputRequestFactory,
            TriFunction<ICommandSender, MovableRetriever, Object, CompletableFuture<Boolean>> delayedFunction)
        {
            super(context, inputRequestFactory, Object.class);
            this.delayedFunction = delayedFunction;
        }

        @Override
        protected CommandDefinition getCommandDefinition()
        {
            return COMMAND_DEFINITION;
        }

        @Override
        protected CompletableFuture<?> delayedInputExecutor(
            ICommandSender commandSender, MovableRetriever movableRetriever, Object delayedInput)
        {
            return delayedFunction.apply(commandSender, movableRetriever, delayedInput);
        }

        @Override
        protected String inputRequestMessage(ICommandSender commandSender, MovableRetriever movableRetriever)
        {
            return INPUT_REQUEST_MSG;
        }
    }
}
