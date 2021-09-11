package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.managers.DelayedCommandInputManager;
import nl.pim16aap2.bigdoors.util.CompletableFutureHandler;
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

class DelayedCommandInputRequestTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private CommandDefinition commandDefinition;

    @Mock
    private ICommandSender commandSender;

    private IPLogger logger;

    private CompletableFutureHandler handler;

    private ILocalizer localizer;

    private DelayedCommandInputManager delayedCommandInputManager;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        logger = new BasicPLogger();
        handler = new CompletableFutureHandler(logger);
        localizer = UnitTestUtil.initLocalizer();
        delayedCommandInputManager = new DelayedCommandInputManager();
    }

    @Test
    @SneakyThrows
    void test()
    {
        final var delayedInput = new DelayedInput(UUID.randomUUID(), "Some string");
        final var inputRequest = new DelayedCommandInputRequest<>(100, commandSender, commandDefinition,
                                                                  input -> verifyInput(delayedInput, input),
                                                                  () -> "", DelayedInput.class,
                                                                  logger, localizer, delayedCommandInputManager,
                                                                  handler);

        final var first = inputRequest.getCommandOutput();
        final var second = inputRequest.provide(delayedInput);

        Assertions.assertTrue(second.get(1, TimeUnit.SECONDS));
        Assertions.assertEquals(first, second);
    }

    @Test
    @SneakyThrows
    void testInvalidInput()
    {
        final var delayedInput = new DelayedInput(UUID.randomUUID(), "Some string");
        final var inputRequest = new DelayedCommandInputRequest<>(100, commandSender, commandDefinition,
                                                                  input -> verifyInput(delayedInput, input),
                                                                  () -> "", DelayedInput.class, logger, localizer,
                                                                  delayedCommandInputManager, handler);

        final var first = inputRequest.getCommandOutput();
        final var second = inputRequest.provide("Invalid!");

        Assertions.assertFalse(second.get(1, TimeUnit.SECONDS));
        Assertions.assertNotEquals(first, second);
    }

    @Test
    @SneakyThrows
    void testException()
    {
        // Ensure that exceptions are properly propagated.
        final var inputRequest = new DelayedCommandInputRequest<>(100, commandSender, commandDefinition,
                                                                  input ->
                                                                  {
                                                                      throw new RuntimeException(input.toString());
                                                                  },
                                                                  () -> "", DelayedInput.class, logger, localizer,
                                                                  delayedCommandInputManager, handler);
        Assertions.assertThrows(ExecutionException.class,
                                () -> inputRequest.provide(new DelayedInput(UUID.randomUUID(), ""))
                                                  .get(1, TimeUnit.SECONDS));
    }

    private CompletableFuture<Boolean> verifyInput(DelayedInput actualInput, DelayedInput delayedInput)
    {
        Assertions.assertEquals(actualInput, delayedInput);
        return CompletableFuture.completedFuture(true);
    }

    private record DelayedInput(UUID uuid, String string)
    {
    }
}
