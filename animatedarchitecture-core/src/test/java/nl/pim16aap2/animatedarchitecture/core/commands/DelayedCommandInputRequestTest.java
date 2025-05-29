package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.exceptions.InvalidCommandInputException;
import nl.pim16aap2.animatedarchitecture.core.managers.DelayedCommandInputManager;
import nl.pim16aap2.testing.AssistedFactoryMocker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class DelayedCommandInputRequestTest
{
    @Mock
    private CommandDefinition commandDefinition;

    @Mock
    private ICommandSender commandSender;

    @Mock
    private IExecutor executor;

    private DelayedCommandInputRequest.IFactory<DelayedInput> inputRequestFactory;

    @BeforeEach
    void init()
    {
        final DelayedCommandInputManager delayedCommandInputManager = new DelayedCommandInputManager(mock());

        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());

        //noinspection unchecked
        inputRequestFactory = AssistedFactoryMocker
            .injectMocksFromTestClass(DelayedCommandInputRequest.IFactory.class, this)
            .injectParameter(delayedCommandInputManager)
            .getFactory();
    }

    @Test
    void test()
    {
        final DelayedInput delayedInput = new DelayedInput(UUID.randomUUID(), "Some string");
        final String resultValue = "Result value";
        final var inputRequest = inputRequestFactory.create(
            100,
            commandSender,
            commandDefinition,
            input ->
            {
                verifyInput(delayedInput, input).join();
                return CompletableFuture.completedFuture(resultValue);
            },
            () -> "",
            DelayedInput.class,
            null
        );

        final CompletableFuture<?> first = inputRequest.getCommandOutput();
        final CompletableFuture<?> second = inputRequest.provide(delayedInput);

        second.join();

        assertThat(first.isDone()).isTrue();
        assertThat(first.join()).isEqualTo(second.join()).isEqualTo(resultValue);
    }

    @Test
    void testInvalidInput()
    {
        final DelayedInput delayedInput = new DelayedInput(UUID.randomUUID(), "Some string");
        final var inputRequest = inputRequestFactory.create(
            100,
            commandSender,
            commandDefinition,
            input -> verifyInput(delayedInput, input),
            () -> "",
            DelayedInput.class,
            null
        );
        UnitTestUtil.initMessageable(commandSender);

        final CompletableFuture<?> first = inputRequest.getCommandOutput();
        Assertions.assertThrows(InvalidCommandInputException.class, () -> inputRequest.provide("Invalid!"));

        assertFalse(first.isDone());
    }

    @Test
    void testException()
    {
        final var inputRequest = inputRequestFactory.create(
            100,
            commandSender,
            commandDefinition,
            input ->
            {
                throw new IllegalArgumentException(input.toString());
            },
            () -> "",
            DelayedInput.class,
            null
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
