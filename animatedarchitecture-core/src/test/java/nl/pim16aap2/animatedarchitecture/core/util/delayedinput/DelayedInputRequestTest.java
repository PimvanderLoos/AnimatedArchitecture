package nl.pim16aap2.animatedarchitecture.core.util.delayedinput;

import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.util.StringUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class DelayedInputRequestTest
{
    @Mock
    private IExecutor executor;

    @BeforeEach
    void init()
    {
        when(executor.getVirtualExecutor()).thenReturn(Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Makes sure that delayed input requests time out properly after the defined timeout period.
     * <p>
     * Once timed out, the 'input' should be empty.
     */
    @Test
    void testTimeout()
        throws Exception
    {
        final DelayedInputRequestImpl request = new DelayedInputRequestImpl(100, executor);
        final CompletableFuture<Optional<String>> output = request.getInputResult();
        final Optional<String> result = output.get(1000, TimeUnit.MILLISECONDS);

        assertTrue(request.timedOut());
        assertTrue(result.isEmpty());
    }

    @Test
    void testDoubleInput()
        throws Exception
    {
        final String firstInput = StringUtil.randomString(10);
        final String secondInput = StringUtil.randomString(10);

        final DelayedInputRequestImpl request = new DelayedInputRequestImpl(5, TimeUnit.SECONDS, executor);
        final CompletableFuture<Optional<String>> output = request.getInputResult();

        request.set(firstInput);
        assertThrows(IllegalStateException.class, () -> request.set(secondInput));

        final Optional<String> result = output.get(100, TimeUnit.MILLISECONDS);

        assertTrue(request.success());
        assertTrue(result.isPresent());
        assertEquals(firstInput, result.get());
        assertNotEquals(secondInput, result.get());
    }

    /**
     * Makes sure that providing input works properly and returns it as intended.`
     */
    @Test
    void testInput()
        throws Exception
    {
        final String inputString = StringUtil.randomString(10);

        final DelayedInputRequestImpl request = new DelayedInputRequestImpl(5, TimeUnit.SECONDS, executor);
        final CompletableFuture<Optional<String>> output = request.getInputResult();

        request.set(inputString);

        final Optional<String> result = output.get(100, TimeUnit.MILLISECONDS);

        assertTrue(request.success());
        assertTrue(result.isPresent());
        assertEquals(inputString, result.get());
    }

    @Test
    void testStatusCancelled()
    {
        final DelayedInputRequestImpl request = new DelayedInputRequestImpl(5, TimeUnit.SECONDS, executor);
        assertEquals(DelayedInputRequest.Status.WAITING, request.getStatus());
        assertFalse(request.success());
        assertFalse(request.cancelled());
        assertFalse(request.timedOut());
        assertFalse(request.exceptionally());
        assertFalse(request.completed());

        request.cancel();
        assertEquals(DelayedInputRequest.Status.CANCELLED, request.getStatus());
        assertFalse(request.success());
        assertTrue(request.cancelled());
        assertFalse(request.timedOut());
        assertFalse(request.exceptionally());
        assertTrue(request.completed());
    }

    @Test
    void testStatusTimedOut()
        throws Exception
    {
        final DelayedInputRequestImpl request = new DelayedInputRequestImpl(1, TimeUnit.MILLISECONDS, executor);
        final long startTime = System.nanoTime();
        request.getInputResult().get(2, TimeUnit.SECONDS);
        final long duration = System.nanoTime() - startTime;
        // Ensure it took Much less than the 'allowed' 2 seconds.
        assertTrue(duration < Duration.ofSeconds(1).toNanos());

        assertEquals(DelayedInputRequest.Status.TIMED_OUT, request.getStatus());
        assertFalse(request.success());
        assertFalse(request.cancelled());
        assertTrue(request.timedOut());
        assertFalse(request.exceptionally());
        assertTrue(request.completed());
    }

    @Test
    void testStatusSuccess()
        throws Exception
    {
        final DelayedInputRequestImpl request = new DelayedInputRequestImpl(1, TimeUnit.SECONDS, executor);
        request.set("VALUE");
        request.getInputResult().get(1, TimeUnit.SECONDS);
        assertEquals(DelayedInputRequest.Status.COMPLETED, request.getStatus());
        assertTrue(request.success());
        assertFalse(request.cancelled());
        assertFalse(request.timedOut());
        assertFalse(request.exceptionally());
        assertTrue(request.completed());
    }

    private static class DelayedInputRequestImpl extends DelayedInputRequest<String>
    {
        public DelayedInputRequestImpl(long timeout, TimeUnit timeUnit, IExecutor executor)
        {
            super(timeout, timeUnit, executor);
        }

        public DelayedInputRequestImpl(long timeout, IExecutor executor)
        {
            this(timeout, TimeUnit.MILLISECONDS, executor);
        }
    }
}
