package nl.pim16aap2.bigdoors.util.delayedinput;

import lombok.NonNull;
import lombok.val;
import nl.pim16aap2.bigdoors.util.Util;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

class DelayedInputRequestTest
{
    /**
     * Makes sure invalid operations properly throw errors.
     */
    @Test
    public void testFailure()
    {
        Assertions.assertThrows(Exception.class, () -> new DelayedInputRequestImpl(-1));
        Assertions.assertThrows(Exception.class, () -> new DelayedInputRequestImpl(0));
        Assertions.assertDoesNotThrow(() -> new DelayedInputRequestImpl(1));

        val request = Assertions.assertDoesNotThrow(() -> new DelayedInputRequestImpl(100));
        Assertions.assertDoesNotThrow(request::waitForInput);
        Assertions.assertThrows(IllegalStateException.class, request::waitForInput);
    }

    /**
     * Makes sure that delayed input requests time out properly after the defined timeout period.
     * <p>
     * Once timed out, the 'input' should be empty.
     */
    @Test
    public void testTimeout()
    {
        val request = Assertions.assertDoesNotThrow(() -> new DelayedInputRequestImpl(100));
        val futureResult = CompletableFuture.supplyAsync(() -> waitForInput(request)).exceptionally(Assertions::fail);
        sleep(10);
        Assertions.assertFalse(futureResult.isDone());

        sleep(200);

        Assertions.assertTrue(futureResult.isDone());
        Assertions.assertFalse(futureResult.join().isPresent());
    }

    /**
     * Makes sure that providing input works properly and returns it as intended.`
     */
    @Test
    public void testInput()
    {
        final @NonNull String inputString = Util.randomInsecureString(10);

        val request = Assertions.assertDoesNotThrow(() -> new DelayedInputRequestImpl(1000));
        val result = CompletableFuture.supplyAsync(() -> waitForInput(request)).exceptionally(Assertions::fail);
        sleep(5); // Give it a bit of time to make sure the request is properly in its waiting state.
        request.set(inputString);
        sleep(5); // GIve it a bit more time to ensure the CompletableFuture realizes it's done.
        Assertions.assertTrue(result.isDone());

        Optional<String> outputOpt = result.join();
        Assertions.assertTrue(outputOpt.isPresent());
        Assertions.assertEquals(inputString, outputOpt.get());
    }

    /**
     * Remaps {@link DelayedInputRequest#waitForInput()} from a method to throws a checked exception to one that throws
     * an unchecked one.
     *
     * @param delayedInputRequest The {@link DelayedInputRequestImpl} that will wait for input.
     * @return The result of the input.
     *
     * @throws RuntimeException Thrown when {@link DelayedInputRequest#waitForInput()} threw a checked exception.
     */
    private @NonNull Optional<String> waitForInput(@NonNull DelayedInputRequestImpl delayedInputRequest)
        throws RuntimeException
    {
        try
        {
            return delayedInputRequest.waitForInput();
        }
        catch (Throwable t)
        {
            throw new RuntimeException(t);
        }
    }

    /**
     * Makes sure that the status of the request is applied properly at each stage.
     */
    @Test
    public void testStatusWaiting()
    {
        val request = Assertions.assertDoesNotThrow(() -> new DelayedInputRequestImpl(1000));
        Assertions.assertEquals(request.getStatus(), DelayedInputRequest.Status.INACTIVE);
        CompletableFuture.runAsync(() -> waitForInput(request)).exceptionally(Assertions::fail);

        sleep(5);
        Assertions.assertEquals(DelayedInputRequest.Status.WAITING, request.getStatus());
        request.set("");
        sleep(5);
        Assertions.assertEquals(DelayedInputRequest.Status.COMPLETED, request.getStatus());
    }

    @Test
    public void testStatusTimedOut()
    {
        val request = Assertions.assertDoesNotThrow(() -> new DelayedInputRequestImpl(10));
        CompletableFuture.runAsync(() -> waitForInput(request)).exceptionally(Assertions::fail);
        sleep(50);
        Assertions.assertEquals(DelayedInputRequest.Status.TIMED_OUT, request.getStatus());
    }

    @Test
    public void testStatusCancelled()
    {
        val request = Assertions.assertDoesNotThrow(() -> new DelayedInputRequestImpl(1000));
        val futureResult = CompletableFuture.supplyAsync(() -> waitForInput(request)).exceptionally(Assertions::fail);
        sleep(5);
        request.cancel();
        sleep(5);
        Assertions.assertEquals(DelayedInputRequest.Status.CANCELLED, request.getStatus());
        Assertions.assertFalse(futureResult.join().isPresent());
    }

    private void sleep(final long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    private static class DelayedInputRequestImpl extends DelayedInputRequest<String>
    {
        public DelayedInputRequestImpl(final long timeout)
            throws Exception
        {
            super(timeout);
        }

        @Override
        protected void init()
        {

        }
    }
}
