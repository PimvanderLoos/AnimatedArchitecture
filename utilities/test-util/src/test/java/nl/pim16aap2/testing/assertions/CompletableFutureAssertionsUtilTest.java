package nl.pim16aap2.testing.assertions;

import nl.pim16aap2.util.exceptions.ContextualOperationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opentest4j.AssertionFailedError;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import static nl.pim16aap2.testing.assertions.CompletableFutureAssertionsUtil.getNextContextualOperationException;
import static org.assertj.core.api.Assertions.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class CompletableFutureAssertionsUtilTest
{
    @Test
    void getNextContextualOperationException_shouldReturnInputIfContextualOperationException()
    {
        final var exception = new ContextualOperationException("Test message");
        final var result = getNextContextualOperationException(exception);

        assertThat(result).isEqualTo(exception);
    }

    @Test
    void getNextContextualOperationException_shouldFindNestedContextualOperationExceptions()
    {
        final var exception0 = new ContextualOperationException("Test message");
        final var exception1 = new RuntimeException("Another Exception", exception0);
        final var exception2 = new RuntimeException("Yet Another Exception", exception1);

        final var result = getNextContextualOperationException(exception2);

        assertThat(result).isEqualTo(exception0);
    }

    @Test
    void withMessage_shouldSetMessage()
    {
        final var exception = new ContextualOperationException("Test message");
        final var future = CompletableFuture.failedFuture(exception);

        CompletableFutureAssertionsUtil
            .assertHasExceptionContext(future)
            .withMessage("Test message")
            .thenAssert();
    }

    @Test
    void withMessage_shouldSetFormattedMessage()
    {
        final var exception = new ContextualOperationException("Test message with value: 42");
        final var future = CompletableFuture.failedFuture(exception);

        CompletableFutureAssertionsUtil
            .assertHasExceptionContext(future)
            .withMessage("Test message with value: %d", 42)
            .thenAssert();
    }

    @Test
    void withMessage_shouldSetMessageWithMatchType()
    {
        final var exception = new ContextualOperationException("This is a test message with some content");
        final var future = CompletableFuture.failedFuture(exception);

        CompletableFutureAssertionsUtil
            .assertHasExceptionContext(future)
            .withMessage(AssertionsUtil.StringMatchType.CONTAINS, "test message")
            .thenAssert();
    }

    @Test
    void withStringMatchType_shouldSetMatchType()
    {
        final var exception = new ContextualOperationException("This is a test message");
        final var future = CompletableFuture.failedFuture(exception);

        CompletableFutureAssertionsUtil
            .assertHasExceptionContext(future)
            .withMessage("test message")
            .withStringMatchType(AssertionsUtil.StringMatchType.CONTAINS)
            .thenAssert();
    }

    @Test
    void withTimeOut_shouldSetTimeout()
    {
        final var future = new CompletableFuture<>();

        assertThatThrownBy(
            () -> CompletableFutureAssertionsUtil
                .assertHasExceptionContext(future)
                .withTimeOut(10)
                .thenAssert())
            .isInstanceOf(AssertionFailedError.class);

        assertThat(future).isCompletedExceptionally().isNotCancelled();

        assertThatThrownBy(future::join)
            .isExactlyInstanceOf(CompletionException.class)
            .hasCauseExactlyInstanceOf(TimeoutException.class);
    }

    @Test
    void getNextContextualOperationException_shouldReturnExceptionWhenPresent()
    {
        final var innerException = new ContextualOperationException("Inner exception");
        final var outerException = new RuntimeException("Outer exception", innerException);
        final var future = CompletableFuture.failedFuture(outerException);

        final ContextualOperationException result = CompletableFutureAssertionsUtil
            .assertHasExceptionContext(future)
            .withMessage("Inner exception")
            .thenAssert();

        assertThat(result).isNotNull();
        assertThat(result.getMessage()).isEqualTo("Inner exception");
    }

    @Test
    void getNextContextualOperationException_shouldReturnNullWhenNullInput()
    {
        //noinspection DataFlowIssue
        assertThat(getNextContextualOperationException(null)).isNull();
    }

    @Test
    void getNextContextualOperationException_shouldReturnNullWhenNotPresent()
    {
        final var exception = new RuntimeException("Not a contextual exception");
        final var future = CompletableFuture.failedFuture(exception);

        assertThatThrownBy(
            () -> CompletableFutureAssertionsUtil
                .assertHasExceptionContext(future)
                .thenAssert())
            .hasMessageStartingWith("Could not find the expected ContextualOperationException");
    }

    @Test
    void thenAssert_shouldThrowWhenNoMatchingExceptionFound()
    {
        final var exception = new ContextualOperationException("Actual message");
        final var future = CompletableFuture.failedFuture(exception);

        assertThatThrownBy(
            () -> CompletableFutureAssertionsUtil
                .assertHasExceptionContext(future)
                .withMessage("Expected message")
                .thenAssert())
            .hasMessageStartingWith("Could not find the expected ContextualOperationException");
    }

    @Test
    void thenAssert_shouldReturnMatchingException()
    {
        final var exception = new ContextualOperationException("Test message");
        final var future = CompletableFuture.failedFuture(exception);

        ContextualOperationException result = CompletableFutureAssertionsUtil
            .assertHasExceptionContext(future)
            .withMessage("Test message")
            .thenAssert();

        assertThat(result).isEqualTo(exception);
    }

    @Test
    void thenAssert_shouldMatchNullMessage()
    {
        final var exception = new ContextualOperationException();
        final var future = CompletableFuture.failedFuture(exception);

        CompletableFutureAssertionsUtil
            .assertHasExceptionContext(future)
            .thenAssert();
    }

    @Test
    void thenAssert_shouldWorkWithNestedExceptions()
    {
        final var level3 = new ContextualOperationException("Level 3 exception");
        final var level2 = new RuntimeException("Level 2 exception", level3);
        final var level1 = new IllegalArgumentException("Level 1 exception", level2);
        final var future = CompletableFuture.failedFuture(level1);

        ContextualOperationException result = CompletableFutureAssertionsUtil
            .assertHasExceptionContext(future)
            .withMessage("Level 3 exception")
            .thenAssert();

        assertThat(result).isEqualTo(level3);
    }
}
