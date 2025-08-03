package nl.pim16aap2.testing.assertions;

import nl.altindag.log.model.LogEvent;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

import static nl.pim16aap2.testing.assertions.LogEventAssert.assertThatLogEvent;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class LogEventAssertTest
{
    @Test
    void assertThatLogEvent_shouldReturnLogEventAssert()
    {
        // setup
        final LogEvent testEvent = createMockLogEvent(null);

        // execute
        final LogEventAssert result = assertThatLogEvent(testEvent);

        // verify
        assertThat(result).extracting("actual").isSameAs(testEvent);
    }

    @Test
    void hasThrowable_shouldPassWhenThrowableIsPresent()
    {
        // setup
        final IOException exception = new IOException("Test exception");
        final LogEvent testEvent = createMockLogEvent(exception);
        final LogEventAssert logEventAssert = assertThatLogEvent(testEvent);

        // execute & verify
        logEventAssert.hasThrowable().isSameAs(exception);
    }

    @Test
    void hasThrowable_shouldFailWhenThrowableIsAbsent()
    {
        // setup
        final LogEvent testEvent = createMockLogEvent(null);
        final LogEventAssert logEventAssert = assertThatLogEvent(testEvent);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(logEventAssert::hasThrowable)
            .withMessage("Expected log event to have a throwable but found none.");
    }

    @Test
    void hasThrowableOfType_shouldPassForSameType()
    {
        // setup
        final IOException exception = new IOException("Test exception");
        final LogEvent testEvent = createMockLogEvent(exception);
        final LogEventAssert logEventAssert = assertThatLogEvent(testEvent);

        // execute & verify
        logEventAssert.hasThrowableOfType(IOException.class).isSameAs(exception);
    }

    @Test
    void hasThrowableOfType_shouldPassForSubtype()
    {
        // setup
        final UncheckedIOException exception = new UncheckedIOException(new IOException("Test exception"));
        final LogEvent testEvent = createMockLogEvent(exception);
        final LogEventAssert logEventAssert = assertThatLogEvent(testEvent);

        // execute & verify
        logEventAssert.hasThrowableOfType(RuntimeException.class).isSameAs(exception);
    }

    @Test
    void hasThrowableOfType_shouldFailForDifferentType()
    {
        // setup
        final IllegalArgumentException exception = new IllegalArgumentException("Test exception");
        final LogEvent testEvent = createMockLogEvent(exception);
        final LogEventAssert logEventAssert = assertThatLogEvent(testEvent);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventAssert.hasThrowableOfType(IOException.class));
    }

    @Test
    void hasThrowableOfType_shouldFailWhenThrowableIsAbsent()
    {
        // setup
        final LogEvent testEvent = createMockLogEvent(null);
        final LogEventAssert logEventAssert = assertThatLogEvent(testEvent);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventAssert.hasThrowableOfType(IOException.class))
            .withMessage("Expected log event to have a throwable of type java.io.IOException but found none.");
    }

    @Test
    void hasThrowableExactlyOfType_shouldPassForSameType()
    {
        // setup
        final IOException exception = new IOException("Test exception");
        final LogEvent testEvent = createMockLogEvent(exception);
        final LogEventAssert logEventAssert = assertThatLogEvent(testEvent);

        // execute & verify
        logEventAssert.hasThrowableExactlyOfType(IOException.class);
    }

    @Test
    void hasThrowableExactlyOfType_shouldFailForSubtype()
    {
        // setup
        final UncheckedIOException exception = new UncheckedIOException(new IOException("Test exception"));
        final LogEvent testEvent = createMockLogEvent(exception);
        final LogEventAssert logEventAssert = assertThatLogEvent(testEvent);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventAssert.hasThrowableExactlyOfType(IOException.class));
    }

    @Test
    void hasThrowableExactlyOfType_shouldFailForDifferentType()
    {
        // setup
        final IllegalArgumentException exception = new IllegalArgumentException("Test exception");
        final LogEvent testEvent = createMockLogEvent(exception);
        final LogEventAssert logEventAssert = assertThatLogEvent(testEvent);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventAssert.hasThrowableExactlyOfType(IOException.class));
    }

    @Test
    void hasThrowableExactlyOfType_shouldFailWhenThrowableIsAbsent()
    {
        // setup
        final LogEvent testEvent = createMockLogEvent(null);
        final LogEventAssert logEventAssert = assertThatLogEvent(testEvent);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventAssert.hasThrowableExactlyOfType(IOException.class))
            .withMessage("Expected log event to have a throwable of type java.io.IOException but found none.");
    }

    @Test
    void hasThrowableOfType_shouldHandleNullClass()
    {
        // setup
        final IOException exception = new IOException("Test exception");
        final LogEvent testEvent = createMockLogEvent(exception);
        final LogEventAssert logEventAssert = assertThatLogEvent(testEvent);

        // execute & verify
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> logEventAssert.hasThrowableOfType(null));
    }

    @Test
    void hasThrowableExactlyOfType_shouldHandleNullClass()
    {
        // setup
        final IOException exception = new IOException("Test exception");
        final LogEvent testEvent = createMockLogEvent(exception);
        final LogEventAssert logEventAssert = assertThatLogEvent(testEvent);

        // execute & verify
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> logEventAssert.hasThrowableExactlyOfType(null));
    }

    @Test
    void hasThrowableOfType_shouldPassForRuntimeExceptionSubtype()
    {
        // setup
        final IllegalArgumentException exception = new IllegalArgumentException("Test exception");
        final LogEvent testEvent = createMockLogEvent(exception);
        final LogEventAssert logEventAssert = assertThatLogEvent(testEvent);

        // execute & verify
        logEventAssert.hasThrowableOfType(RuntimeException.class).isSameAs(exception);
    }

    private LogEvent createMockLogEvent(@Nullable Throwable throwable)
    {
        final LogEvent mockEvent = mock(LogEvent.class);
        lenient().when(mockEvent.getThrowable()).thenReturn(Optional.ofNullable(throwable));
        return mockEvent;
    }
}