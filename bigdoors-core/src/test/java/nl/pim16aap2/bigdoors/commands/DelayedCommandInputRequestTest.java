package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initPlatform;

class DelayedCommandInputRequestTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    CommandDefinition commandDefinition;

    IBigDoorsPlatform platform;

    @Mock
    ICommandSender commandSender;

    @BeforeEach
    void init()
    {
        platform = initPlatform();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @SneakyThrows
    void test()
    {
        val delayedInput = new DelayedInput(UUID.randomUUID(), "Some string");
        val inputRequest = new DelayedCommandInputRequest<>(100, commandSender, commandDefinition,
                                                            input -> verifyInput(delayedInput, input),
                                                            () -> "", DelayedInput.class);

        val first = inputRequest.getOutput();
        val second = inputRequest.provide(delayedInput);

        Assertions.assertTrue(second.get(1, TimeUnit.SECONDS));
        Assertions.assertEquals(first, second);
    }

    @Test
    @SneakyThrows
    void testInvalidInput()
    {
        val delayedInput = new DelayedInput(UUID.randomUUID(), "Some string");
        val inputRequest = new DelayedCommandInputRequest<>(100, commandSender, commandDefinition,
                                                            input -> verifyInput(delayedInput, input),
                                                            () -> "", DelayedInput.class);

        val first = inputRequest.getOutput();
        val second = inputRequest.provide("Invalid!");

        Assertions.assertFalse(second.get(1, TimeUnit.SECONDS));
        Assertions.assertNotEquals(first, second);
    }

    @Test
    @SneakyThrows
    void testException()
    {
        // Ensure that exceptions are properly propagated.
        val inputRequest = new DelayedCommandInputRequest<>(100, commandSender, commandDefinition,
                                                            input ->
                                                            {
                                                                throw new RuntimeException(input.toString());
                                                            },
                                                            () -> "", DelayedInput.class);
        Assertions.assertThrows(ExecutionException.class,
                                () -> inputRequest.provide(new DelayedInput(UUID.randomUUID(), ""))
                                                  .get(1, TimeUnit.SECONDS));
    }

    private @NonNull CompletableFuture<Boolean> verifyInput(final @NonNull DelayedInput actualInput,
                                                            final @NonNull DelayedInput delayedInput)
    {
        Assertions.assertEquals(actualInput, delayedInput);
        return CompletableFuture.completedFuture(true);
    }

    @Value
    private static class DelayedInput
    {
        @NonNull UUID uuid;
        @NonNull String string;
    }
}
