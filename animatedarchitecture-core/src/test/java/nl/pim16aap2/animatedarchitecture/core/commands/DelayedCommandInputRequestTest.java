package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.altindag.log.LogCaptor;
import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.DelayedCommandInputManager;
import nl.pim16aap2.testing.logging.LogAssertionsUtil;
import nl.pim16aap2.testing.logging.WithLogCapture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Timeout(1)
@WithLogCapture
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DelayedCommandInputRequestTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private CommandDefinition commandDefinition;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private ICommandSender commandSender;

    private ILocalizer localizer;

    private DelayedCommandInputManager delayedCommandInputManager;

    @BeforeEach
    void init()
    {
        localizer = UnitTestUtil.initLocalizer();
        delayedCommandInputManager = new DelayedCommandInputManager(Mockito.mock(DebuggableRegistry.class));
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
            () -> "", DelayedInput.class,
            localizer,
            ITextFactory.getSimpleTextFactory(),
            delayedCommandInputManager
        );

        final CompletableFuture<?> first = inputRequest.getCommandOutput();
        final CompletableFuture<?> second = inputRequest.provide(delayedInput);

        Assertions.assertDoesNotThrow(() -> second.get(1, TimeUnit.SECONDS));
        Assertions.assertEquals(first, second);
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
            localizer,
            ITextFactory.getSimpleTextFactory(),
            delayedCommandInputManager
        );

        final CompletableFuture<?> first = inputRequest.getCommandOutput();
        final CompletableFuture<?> second = inputRequest.provide("Invalid!");

        Assertions.assertDoesNotThrow(() -> second.get(1, TimeUnit.SECONDS));
        Assertions.assertNotEquals(first, second);
    }

    @Test
    void testException(LogCaptor logCaptor)
        throws ExecutionException, InterruptedException, TimeoutException
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
            localizer,
            ITextFactory.getSimpleTextFactory(),
            delayedCommandInputManager
        );

        final UUID uuid = UUID.randomUUID();
        final String providedInput = UUID.randomUUID().toString();

        inputRequest.provide(new DelayedInput(uuid, providedInput)).get(1, TimeUnit.SECONDS);

        LogAssertionsUtil.assertThrowableLogged(
            logCaptor,
            -1,
            null,
            CompletionException.class, null,
            IllegalArgumentException.class,
            String.format("DelayedInput[uuid=%s, string=%s]", uuid, providedInput)
        );
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
