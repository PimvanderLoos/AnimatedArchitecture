package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.exceptions.CommandExecutionException;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.DelayedCommandInputManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.util.functional.TriFunction;
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.animatedarchitecture.core.UnitTestUtil.textArgumentMatcher;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DelayedCommandTest
{
    @Spy
    private DelayedCommandInputManager delayedCommandInputManager =
        new DelayedCommandInputManager(Mockito.mock(DebuggableRegistry.class));

    private final ILocalizer localizer = UnitTestUtil.initLocalizer();

    @Mock
    private DelayedCommandInputRequest.IFactory<Object> inputRequestFactory;

    @SuppressWarnings("unused")
    @Spy
    private ITextFactory textFactory = ITextFactory.getSimpleTextFactory();

    @InjectMocks
    private DelayedCommand.Context context;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private ICommandSender commandSender;

    private StructureRetriever structureRetriever;

    @Mock
    private Structure structure;

    @InjectMocks
    private StructureRetrieverFactory structureRetrieverFactory;

    @Mock
    private TriFunction<ICommandSender, StructureRetriever, Object, CompletableFuture<Boolean>> delayedFunction;

    @Mock
    private IExecutor executor;


    @BeforeEach
    void init()
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());
        initInputRequestFactory(inputRequestFactory, executor, localizer, delayedCommandInputManager);
        structureRetriever = structureRetrieverFactory.of(structure);
    }

    @Test
    void normal()
    {
        final DelayedCommandImpl delayedCommand = new DelayedCommandImpl(context, inputRequestFactory, delayedFunction);

        delayedCommand.runDelayed(commandSender, structureRetriever);
        verify(commandSender, times(1)).sendMessage(textArgumentMatcher(DelayedCommandImpl.INPUT_REQUEST_MSG));
        verify(inputRequestFactory, times(1)).create(anyLong(), any(), any(), any(), any(), any());
        verify(delayedFunction, never()).apply(any(), any(), any());

        final Object input = new Object();
        delayedCommand.provideDelayedInput(commandSender, input).join();
        verify(delayedFunction, times(1)).apply(commandSender, structureRetriever, input);

        delayedCommand.provideDelayedInput(commandSender, new Object());
        verify(commandSender, times(1)).sendMessage(textArgumentMatcher("commands.base.error.not_waiting"));
    }

    @Test
    void notWaiting()
    {
        final DelayedCommandImpl delayedCommand = new DelayedCommandImpl(context, inputRequestFactory, delayedFunction);

        delayedCommand.provideDelayedInput(commandSender, new Object());
        verify(commandSender, times(1)).sendMessage(textArgumentMatcher("commands.base.error.not_waiting"));
    }

    @Test
    void exception()
    {
        when(delayedFunction.apply(any(), any(), any())).thenThrow(RuntimeException.class);

        final DelayedCommandImpl delayedCommand = new DelayedCommandImpl(context, inputRequestFactory, delayedFunction);
        delayedCommand.runDelayed(commandSender, structureRetriever);

        Throwable cause = assertThrows(
            Throwable.class,
            () -> delayedCommand.provideDelayedInput(commandSender, new Object()).get(1, TimeUnit.SECONDS)
        );

        while (!(cause instanceof CommandExecutionException) && cause.getCause() != null)
            cause = cause.getCause();

        assertNotNull(cause);
        assertTrue(cause.getMessage().startsWith("Failed to execute delayed command 'DelayedCommand("));
    }

    /**
     * Initializes the input request factory such that the creation method returns a new request using the provided
     * input.
     */
    public static <T> void initInputRequestFactory(
        DelayedCommandInputRequest.IFactory<T> inputRequestFactory,
        IExecutor executor,
        ILocalizer localizer,
        DelayedCommandInputManager delayedCommandInputManager)
    {
        when(inputRequestFactory
            .create(anyLong(), any(), any(), any(), any(), any()))
            .thenAnswer(invocation -> new DelayedCommandInputRequest<T>(
                invocation.getArgument(0, Long.class),
                invocation.getArgument(1, ICommandSender.class),
                invocation.getArgument(2, CommandDefinition.class),
                invocation.getArgument(3),
                invocation.getArgument(4),
                invocation.getArgument(5),
                executor,
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
