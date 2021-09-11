package nl.pim16aap2.bigdoors.util.delayedinput;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.util.Util;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

class DelayedInputRequestTest
{
    /**
     * Makes sure that delayed input requests time out properly after the defined timeout period.
     * <p>
     * Once timed out, the 'input' should be empty.
     */
    @Test
    @SneakyThrows
    void testTimeout()
    {
        final var request = new DelayedInputRequestImpl(100);
        final var output = request.getInputResult();
        final var result = output.get(1000, TimeUnit.MILLISECONDS);

        Assertions.assertTrue(request.timedOut());
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    @SneakyThrows
    void testDoubleInput()
    {
        final String firstInput = Util.randomInsecureString(10);
        final String secondInput = Util.randomInsecureString(10);

        final var request = new DelayedInputRequestImpl(5, TimeUnit.SECONDS);
        final var output = request.getInputResult();

        request.set(firstInput);
        request.set(secondInput);

        final var result = output.get(100, TimeUnit.MILLISECONDS);

        Assertions.assertTrue(request.success());
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(firstInput, result.get());
        Assertions.assertNotEquals(secondInput, result.get());
    }

    /**
     * Makes sure that providing input works properly and returns it as intended.`
     */
    @Test
    @SneakyThrows
    void testInput()
    {
        final String inputString = Util.randomInsecureString(10);

        final var request = new DelayedInputRequestImpl(5, TimeUnit.SECONDS);
        final var output = request.getInputResult();

        request.set(inputString);

        final var result = output.get(100, TimeUnit.MILLISECONDS);

        Assertions.assertTrue(request.success());
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(inputString, result.get());
    }

    @Test
    @SneakyThrows
    void testStatusCancelled()
    {
        final var request = new DelayedInputRequestImpl(5, TimeUnit.SECONDS);
        Assertions.assertEquals(DelayedInputRequest.Status.WAITING, request.getStatus());
        Assertions.assertFalse(request.success());
        Assertions.assertFalse(request.cancelled());
        Assertions.assertFalse(request.timedOut());
        Assertions.assertFalse(request.exceptionally());
        Assertions.assertFalse(request.completed());

        request.cancel();
        Assertions.assertEquals(DelayedInputRequest.Status.CANCELLED, request.getStatus());
        Assertions.assertFalse(request.success());
        Assertions.assertTrue(request.cancelled());
        Assertions.assertFalse(request.timedOut());
        Assertions.assertFalse(request.exceptionally());
        Assertions.assertTrue(request.completed());
    }

    @Test
    @SneakyThrows
    void testStatusTimedOut()
    {
        final var request = new DelayedInputRequestImpl(1, TimeUnit.MILLISECONDS);
        request.getInputResult().get(1, TimeUnit.SECONDS);
        Assertions.assertEquals(DelayedInputRequest.Status.TIMED_OUT, request.getStatus());
        Assertions.assertFalse(request.success());
        Assertions.assertFalse(request.cancelled());
        Assertions.assertTrue(request.timedOut());
        Assertions.assertFalse(request.exceptionally());
        Assertions.assertTrue(request.completed());
    }

    @Test
    @SneakyThrows
    void testStatusSuccess()
    {
        final var request = new DelayedInputRequestImpl(1, TimeUnit.SECONDS);
        request.set("VALUE");
        request.getInputResult().get(1, TimeUnit.SECONDS);
        Assertions.assertEquals(DelayedInputRequest.Status.COMPLETED, request.getStatus());
        Assertions.assertTrue(request.success());
        Assertions.assertFalse(request.cancelled());
        Assertions.assertFalse(request.timedOut());
        Assertions.assertFalse(request.exceptionally());
        Assertions.assertTrue(request.completed());
    }

    @Test
    @SneakyThrows
    void testStatusException()
    {
        final var f = DelayedInputRequest.class.getDeclaredField("input");
        f.setAccessible(true);
        final var request = new DelayedInputRequestImpl(1, TimeUnit.SECONDS);

        @SuppressWarnings("unchecked") final var input = (CompletableFuture<String>) f.get(request);

        input.completeExceptionally(new RuntimeException("ExceptionTest!"));

        Assertions.assertThrows(ExecutionException.class, () -> request.getInputResult().get(1, TimeUnit.SECONDS));

        Assertions.assertEquals(DelayedInputRequest.Status.EXCEPTION, request.getStatus());
        Assertions.assertFalse(request.success());
        Assertions.assertFalse(request.cancelled());
        Assertions.assertFalse(request.timedOut());
        Assertions.assertTrue(request.exceptionally());
        Assertions.assertTrue(request.completed());
    }

    private static class DelayedInputRequestImpl extends DelayedInputRequest<String>
    {
        public DelayedInputRequestImpl(IPLogger logger, long timeout, TimeUnit timeUnit)
        {
            super(logger, timeout, timeUnit);
        }

        public DelayedInputRequestImpl(IPLogger logger, long timeout)
        {
            this(logger, timeout, TimeUnit.MILLISECONDS);
        }

        public DelayedInputRequestImpl(long timeout, TimeUnit timeUnit)
        {
            this(new BasicPLogger(), timeout, timeUnit);
        }

        public DelayedInputRequestImpl(long timeout)
        {
            this(timeout, TimeUnit.MILLISECONDS);
        }
    }
}
