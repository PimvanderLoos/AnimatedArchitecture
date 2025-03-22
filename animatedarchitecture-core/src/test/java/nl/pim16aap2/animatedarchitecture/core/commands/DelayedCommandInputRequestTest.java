package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.exceptions.InvalidCommandInputException;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.DelayedCommandInputManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DelayedCommandInputRequestTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private CommandDefinition commandDefinition;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private ICommandSender commandSender;

    @Mock
    private IExecutor executor;

    private ILocalizer localizer;

    private DelayedCommandInputManager delayedCommandInputManager;

    @BeforeEach
    void init()
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());
        localizer = UnitTestUtil.initLocalizer();
        delayedCommandInputManager = new DelayedCommandInputManager(mock(DebuggableRegistry.class));
        when(commandDefinition.getName()).thenReturn("MockedCommand");
    }

    @Test
    void test()
    {
        final DelayedInput delayedInput = new DelayedInput(UUID.randomUUID(), "Some string");
        final DelayedCommandInputRequest<?> inputRequest = new DelayedCommandInputRequest<>(
            100,
            commandSender,
            commandDefinition,
            input -> verifyInput(delayedInput, input),
            () -> "",
            DelayedInput.class,
            executor,
            localizer,
            ITextFactory.getSimpleTextFactory(),
            delayedCommandInputManager
        );

        final CompletableFuture<?> first = inputRequest.getCommandOutput();
        final CompletableFuture<?> second = inputRequest.provide(delayedInput);

        assertDoesNotThrow(() -> second.get(1, TimeUnit.SECONDS));
        assertEquals(first, second);
    }

    @Test
    void testInvalidInput()
    {
        final DelayedInput delayedInput = new DelayedInput(UUID.randomUUID(), "Some string");
        final DelayedCommandInputRequest<?> inputRequest = new DelayedCommandInputRequest<>(
            100,
            commandSender,
            commandDefinition,
            input -> verifyInput(delayedInput, input),
            () -> "",
            DelayedInput.class,
            executor,
            localizer,
            ITextFactory.getSimpleTextFactory(),
            delayedCommandInputManager
        );

        final CompletableFuture<?> first = inputRequest.getCommandOutput();
        Assertions.assertThrows(InvalidCommandInputException.class, () -> inputRequest.provide("Invalid!"));

        assertFalse(first.isDone());
    }

    @Test
    void testException()
    {
        // Ensure that exceptions are properly propagated.
        final DelayedCommandInputRequest<?> inputRequest = new DelayedCommandInputRequest<>(
            100,
            commandSender,
            commandDefinition,
            input ->
            {
                throw new IllegalArgumentException(input.toString());
            },
            () -> "",
            DelayedInput.class,
            executor,
            localizer,
            ITextFactory.getSimpleTextFactory(),
            delayedCommandInputManager
        );

        final UUID uuid = UUID.randomUUID();
        final String providedInput = UUID.randomUUID().toString();

        final var exception = UnitTestUtil.assertRootCause(
            IllegalArgumentException.class,
            () -> inputRequest.provide(new DelayedInput(uuid, providedInput)).get(1, TimeUnit.SECONDS)
        );

        assertEquals(String.format("DelayedInput[uuid=%s, string=%s]", uuid, providedInput), exception.getMessage());
    }

    private CompletableFuture<Boolean> verifyInput(DelayedInput actualInput, DelayedInput delayedInput)
    {
        assertEquals(actualInput, delayedInput);
        return CompletableFuture.completedFuture(true);
    }

    private record DelayedInput(UUID uuid, String string)
    {
    }
}
