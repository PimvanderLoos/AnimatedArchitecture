package nl.pim16aap2.animatedarchitecture.core.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class CompletableFutureExtensionsTest
{
    private static final String TEST_CONTEXT = "Test context";
    private static final Supplier<String> CONTEXT_SUPPLIER = () -> TEST_CONTEXT;
    private static final String TEST_RESULT = "Test result";

    @Test
    void testWithExceptionContextNormalCompletion()
    {
        // Create a future that completes normally
        final CompletableFuture<String> originalFuture = CompletableFuture.completedFuture(TEST_RESULT);

        // Apply the extension method
        final CompletableFuture<String> resultFuture =
            CompletableFutureExtensions.withExceptionContext(originalFuture, CONTEXT_SUPPLIER);

        // Verify that the result future completes normally with the same result
        assertEquals(TEST_RESULT, resultFuture.getNow(null));
    }

    @Test
    void testWithExceptionContextExceptionalCompletion()
    {
        // Create an exception to be thrown
        final IllegalArgumentException originalException = new IllegalArgumentException("Original exception");

        // Create a future that completes exceptionally
        final CompletableFuture<String> originalFuture = new CompletableFuture<>();
        originalFuture.completeExceptionally(originalException);

        // Apply the extension method
        final CompletableFuture<String> resultFuture =
            CompletableFutureExtensions.withExceptionContext(originalFuture, CONTEXT_SUPPLIER);

        // Verify that the result future completes exceptionally
        final ExecutionException executionException = assertThrows(
            ExecutionException.class,
            resultFuture::get
        );

        // Verify that the cause is a RuntimeException with the original exception as its cause
        final Throwable cause = executionException.getCause();
        assertInstanceOf(RuntimeException.class, cause);

        // Verify that the message of the RuntimeException is the context
        assertEquals(TEST_CONTEXT, cause.getMessage());

        // Verify that the cause of the RuntimeException is the original exception
        assertEquals(originalException, cause.getCause());
    }

    @Test
    void testWithExceptionContextChaining()
    {
        // Create a future that completes exceptionally
        final CompletableFuture<String> originalFuture = new CompletableFuture<>();
        originalFuture.completeExceptionally(new IllegalArgumentException("Original exception"));

        // Apply the extension method multiple times with different contexts
        final CompletableFuture<String> firstChain =
            CompletableFutureExtensions.withExceptionContext(originalFuture, () -> "First context");

        final CompletableFuture<String> secondChain =
            CompletableFutureExtensions.withExceptionContext(firstChain, () -> "Second context");

        // Verify that the final future completes exceptionally with the second context
        final ExecutionException executionException = assertThrows(
            ExecutionException.class,
            secondChain::get
        );

        // Verify that the cause chain is preserved
        final Throwable firstCause = executionException.getCause();
        assertInstanceOf(RuntimeException.class, firstCause);
        assertEquals("Second context", firstCause.getMessage());

        final Throwable secondCause = firstCause.getCause();
        assertInstanceOf(RuntimeException.class, secondCause);
        assertEquals("First context", secondCause.getMessage());

        final Throwable originalCause = secondCause.getCause();
        assertInstanceOf(IllegalArgumentException.class, originalCause);
        assertEquals("Original exception", originalCause.getMessage());
    }
}
