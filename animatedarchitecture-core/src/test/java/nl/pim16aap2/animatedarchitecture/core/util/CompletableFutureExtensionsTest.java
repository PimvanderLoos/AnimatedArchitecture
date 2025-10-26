package nl.pim16aap2.animatedarchitecture.core.util;

import nl.altindag.log.LogCaptor;
import nl.pim16aap2.testing.annotations.WithLogCapture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static nl.pim16aap2.testing.assertions.LogCaptorAssert.assertThatLogCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Timeout(2)
@WithLogCapture
@ExtendWith(MockitoExtension.class)
class CompletableFutureExtensionsTest
{
    @Test
    void withExceptionContext_normalCompletion()
        throws ExecutionException, InterruptedException
    {
        final String testResult = "Test result";
        final Supplier<String> contextSupplier = mock();

        final CompletableFuture<String> baseFuture = CompletableFuture.completedFuture(testResult);

        final CompletableFuture<String> futureWithContext =
            CompletableFutureExtensions.withExceptionContext(baseFuture, contextSupplier);

        assertEquals(testResult, futureWithContext.get());
        verify(contextSupplier, never()).get();
    }

    @Test
    void withExceptionContext_exceptionalCompletion()
    {
        final String testContext = "Test context";
        final Supplier<String> contextSupplier = () -> testContext;
        final IllegalArgumentException baseException = new IllegalArgumentException("Base exception");

        final CompletableFuture<String> baseFuture = new CompletableFuture<>();
        baseFuture.completeExceptionally(baseException);

        final CompletableFuture<String> futureWithContext =
            CompletableFutureExtensions.withExceptionContext(baseFuture, contextSupplier);

        final ExecutionException executionException = assertThrows(
            ExecutionException.class,
            futureWithContext::get
        );

        final Throwable cause = executionException.getCause();
        assertInstanceOf(RuntimeException.class, cause);
        assertEquals(testContext, cause.getMessage());
        assertEquals(baseException, cause.getCause());
    }

    @Test
    void withExceptionContext_contextIsChained()
    {
        final CompletableFuture<String> baseFuture = new CompletableFuture<>();
        baseFuture.completeExceptionally(new IllegalArgumentException("Base exception"));

        final CompletableFuture<String> firstChain =
            CompletableFutureExtensions.withExceptionContext(baseFuture, () -> "First context");

        final CompletableFuture<String> secondChain =
            CompletableFutureExtensions.withExceptionContext(firstChain, () -> "Second context");

        final ExecutionException executionException = assertThrows(
            ExecutionException.class,
            secondChain::get
        );

        final Throwable firstCause = executionException.getCause();
        assertInstanceOf(RuntimeException.class, firstCause);
        assertEquals("Second context", firstCause.getMessage());

        final Throwable secondCause = firstCause.getCause();
        assertInstanceOf(RuntimeException.class, secondCause);
        assertEquals("First context", secondCause.getMessage());

        final Throwable thirdCause = secondCause.getCause();
        assertInstanceOf(IllegalArgumentException.class, thirdCause);
        assertEquals("Base exception", thirdCause.getMessage());
    }

    @Test
    void handleExceptional_exceptionalCompletion()
    {
        final Consumer<Throwable> mockedHandler = mock();
        final RuntimeException exception = new RuntimeException("Test exception");
        final CompletableFuture<String> future = new CompletableFuture<>();

        CompletableFutureExtensions.handleExceptional(future, mockedHandler);

        future.completeExceptionally(exception);

        verify(mockedHandler, timeout(1000)).accept(exception);
    }

    @Test
    void handleExceptional_normalCompletion()
    {
        final Consumer<Throwable> mockedHandler = mock();
        final CompletableFuture<String> future = CompletableFuture.completedFuture("Success");

        CompletableFutureExtensions.handleExceptional(future, mockedHandler);

        verify(mockedHandler, never()).accept(any());
    }

    @Test
    void logExceptions_messageLogged(LogCaptor logCaptor)
    {
        final RuntimeException exception = new RuntimeException("Test exception");
        final CompletableFuture<String> future = new CompletableFuture<>();

        CompletableFutureExtensions.logExceptions(future);

        future.completeExceptionally(exception);

        assertThatLogCaptor(logCaptor)
            .atWarn()
            .singleWithMessageExactly("Exception occurred in CompletableFuture!");
    }
}
