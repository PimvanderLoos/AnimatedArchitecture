package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.altindag.log.LogCaptor;
import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.DelayedCommandInputManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.util.functional.TriFunction;
import nl.pim16aap2.testing.logging.LogAssertionsUtil;
import nl.pim16aap2.testing.logging.WithLogCapture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
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
@SuppressWarnings("unused")
@WithLogCapture
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

    StructureRetriever structureRetriever;

    @Mock
    Structure structure;

    @InjectMocks
    StructureRetrieverFactory structureRetrieverFactory;

    @Mock
    TriFunction<ICommandSender, StructureRetriever, Object, CompletableFuture<Boolean>> delayedFunction;


    @BeforeEach
    void init()
    {
        initInputRequestFactory(inputRequestFactory, localizer, delayedCommandInputManager);
        structureRetriever = structureRetrieverFactory.of(structure);
    }

    @Test
    void normal()
    {
        final DelayedCommandImpl delayedCommand = new DelayedCommandImpl(context, inputRequestFactory, delayedFunction);

        delayedCommand.runDelayed(commandSender, structureRetriever);
        Mockito.verify(commandSender, Mockito.times(1))
            .sendMessage(UnitTestUtil.textArgumentMatcher(DelayedCommandImpl.INPUT_REQUEST_MSG));
        Mockito.verify(inputRequestFactory, Mockito.times(1)).create(
            Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(delayedFunction, Mockito.never()).apply(Mockito.any(), Mockito.any(), Mockito.any());

        final Object input = new Object();
        delayedCommand.provideDelayedInput(commandSender, input).join();
        Mockito.verify(delayedFunction, Mockito.times(1)).apply(commandSender, structureRetriever, input);

        delayedCommand.provideDelayedInput(commandSender, new Object());
        Mockito.verify(commandSender, Mockito.times(1))
            .sendMessage(UnitTestUtil.textArgumentMatcher("commands.base.error.not_waiting"));
    }

    @Test
    void notWaiting()
    {
        final DelayedCommandImpl delayedCommand = new DelayedCommandImpl(context, inputRequestFactory, delayedFunction);

        delayedCommand.provideDelayedInput(commandSender, new Object());
        Mockito.verify(commandSender, Mockito.times(1))
            .sendMessage(UnitTestUtil.textArgumentMatcher("commands.base.error.not_waiting"));
    }

    @Test
    void exception(LogCaptor logCaptor)
    {
        Mockito
            .when(delayedFunction.apply(Mockito.any(), Mockito.any(), Mockito.any()))
            .thenThrow(RuntimeException.class);
        final DelayedCommandImpl delayedCommand = new DelayedCommandImpl(context, inputRequestFactory, delayedFunction);
        delayedCommand.runDelayed(commandSender, structureRetriever);
        LogAssertionsUtil.assertThrowingCount(logCaptor, 0);

        Assertions.assertDoesNotThrow(
            () -> delayedCommand.provideDelayedInput(commandSender, new Object()).get(1, TimeUnit.SECONDS)
        );

        LogAssertionsUtil.assertThrowableLogged(logCaptor, -1, null, RuntimeException.class);
        LogAssertionsUtil.assertLogged(
            logCaptor,
            -1,
            "Failed to execute delayed command ",
            LogAssertionsUtil.MessageComparisonMethod.STARTS_WITH
        );
    }

    /**
     * Initializes the input request factory such that the creation method returns a new request using the provided
     * input.
     */
    public static <T> void initInputRequestFactory(
        DelayedCommandInputRequest.IFactory<T> inputRequestFactory, ILocalizer localizer,
        DelayedCommandInputManager delayedCommandInputManager)
    {
        Mockito.when(inputRequestFactory.create(
                Mockito.anyLong(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any()))
            .thenAnswer(invocation -> new DelayedCommandInputRequest<T>(
                invocation.getArgument(0, Long.class),
                invocation.getArgument(1, ICommandSender.class),
                invocation.getArgument(2, CommandDefinition.class),
                invocation.getArgument(3),
                invocation.getArgument(4),
                invocation.getArgument(5),
                localizer,
                ITextFactory.getSimpleTextFactory(),
                delayedCommandInputManager)
            );
    }

    static final class DelayedCommandImpl extends DelayedCommand<Object>
    {
        public static final CommandDefinition COMMAND_DEFINITION =
            new CommandDefinition("DelayedCommandImpl", null, null);

        public static final String INPUT_REQUEST_MSG = "DelayedCommandImpl INPUT REQUEST MSG";

        private final TriFunction<ICommandSender, StructureRetriever, Object, CompletableFuture<Boolean>> delayedFunction;

        DelayedCommandImpl(
            Context context,
            DelayedCommandInputRequest.IFactory<Object> inputRequestFactory,
            TriFunction<ICommandSender, StructureRetriever, Object, CompletableFuture<Boolean>> delayedFunction)
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
            ICommandSender commandSender,
            StructureRetriever structureRetriever,
            Object delayedInput)
        {
            return delayedFunction.apply(commandSender, structureRetriever, delayedInput);
        }

        @Override
        protected String inputRequestMessage(
            ICommandSender commandSender,
            StructureRetriever structureRetriever)
        {
            return INPUT_REQUEST_MSG;
        }
    }
}
