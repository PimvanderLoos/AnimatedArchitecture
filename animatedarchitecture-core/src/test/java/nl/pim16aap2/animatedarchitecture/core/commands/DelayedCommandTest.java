package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.exceptions.CommandExecutionException;
import nl.pim16aap2.animatedarchitecture.core.managers.DelayedCommandInputManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.util.functional.TriFunction;
import nl.pim16aap2.testing.AssistedFactoryMocker;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.animatedarchitecture.core.UnitTestUtil.assertThatMessageable;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class DelayedCommandTest
{
    private DelayedCommandInputRequest.IFactory<Object> inputRequestFactory;

    private StructureRetriever structureRetriever;

    @InjectMocks
    private DelayedCommand.Context context;

    @InjectMocks
    private StructureRetrieverFactory structureRetrieverFactory;

    @Spy
    private DelayedCommandInputManager delayedCommandInputManager =
        new DelayedCommandInputManager(Mockito.mock(DebuggableRegistry.class));

    @Mock
    private ICommandSender commandSender;

    @Mock
    private Structure structure;

    @Mock
    private TriFunction<ICommandSender, StructureRetriever, Object, CompletableFuture<Boolean>> delayedFunction;

    @Mock
    private IExecutor executor;

    @BeforeEach
    void init()
    {
        //noinspection unchecked
        inputRequestFactory = AssistedFactoryMocker
            .injectMocksFromTestClass(DelayedCommandInputRequest.IFactory.class, this)
            .injectParameter(delayedCommandInputManager)
            .getFactory();

        structureRetriever = structureRetrieverFactory.of(structure);
    }

    @RepeatedTest(value = 1000, failureThreshold = 1)
    void normal()
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());
        final DelayedCommandImpl delayedCommand = new DelayedCommandImpl(context, inputRequestFactory, delayedFunction);
        UnitTestUtil.initMessageable(commandSender);

        final var result = delayedCommand.runDelayed(commandSender, structureRetriever);
        assertThatMessageable(commandSender).sentMessage(DelayedCommandImpl.INPUT_REQUEST_MSG);
        verify(inputRequestFactory, times(1)).create(anyLong(), any(), any(), any(), any(), any(), any());
        verify(delayedFunction, never()).apply(any(), any(), any());

        final Object input = new Object();
        delayedCommand.provideDelayedInput(commandSender, input).join();
        assertThat(result.isDone()).isTrue();
        verify(delayedFunction, times(1)).apply(commandSender, structureRetriever, input);

        delayedCommand.provideDelayedInput(commandSender, new Object()).join();
        assertThatMessageable(commandSender).sentErrorMessage("commands.base.error.not_waiting");
    }

    @Test
    void notWaiting()
    {
        final DelayedCommandImpl delayedCommand = new DelayedCommandImpl(context, inputRequestFactory, delayedFunction);
        UnitTestUtil.initMessageable(commandSender);

        delayedCommand.provideDelayedInput(commandSender, new Object()).join();
        assertThatMessageable(commandSender).sentErrorMessage("commands.base.error.not_waiting");
    }

    @Test
    void exception()
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());
        when(delayedFunction.apply(any(), any(), any())).thenThrow(RuntimeException.class);
        UnitTestUtil.initMessageable(commandSender);
        final DelayedCommandImpl delayedCommand = new DelayedCommandImpl(context, inputRequestFactory, delayedFunction);
        var result = delayedCommand.runDelayed(commandSender, structureRetriever);

        final var provideException = assertThrows(
            Throwable.class,
            () -> delayedCommand.provideDelayedInput(commandSender, new Object()).get(1, TimeUnit.SECONDS)
        );

        final var resultException = assertThrows(CompletionException.class, result::join);

        assertEquals(provideException.getCause().getCause(), resultException.getCause());

        Throwable cause = resultException.getCause();
        while (!(cause instanceof CommandExecutionException) && cause.getCause() != null)
            cause = cause.getCause();

        assertThat(cause)
            .isNotNull()
            .extracting(Throwable::getMessage, InstanceOfAssertFactories.STRING)
            .startsWith("Failed to execute delayed command 'DelayedCommand(");
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
