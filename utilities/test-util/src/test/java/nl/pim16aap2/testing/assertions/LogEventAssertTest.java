package nl.pim16aap2.testing.assertions;

import nl.altindag.log.model.LogEvent;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static nl.pim16aap2.testing.assertions.LogEventAssert.assertThatLogEvent;
import static org.assertj.core.api.Assertions.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("JavaTimeDefaultTimeZone")
class LogEventAssertTest
{
    private static final Function<LogEventAssert, LogEvent> ACTUAL_EXTRACTOR = LogEventAssert::actual;

    @Test
    void assertThatLogEvent_shouldCreateAssertForLogEvent()
    {
        // setup
        final LogEvent testEvent = newLogEvent(null);

        // execute
        final LogEventAssert result = assertThatLogEvent(testEvent);

        // verify
        assertThat(result)
            .extracting(ACTUAL_EXTRACTOR)
            .isSameAs(testEvent);
    }

    @Test
    void assertThatLogEvent_shouldAcceptNullLogEvent()
    {
        // execute & verify
        assertThat(assertThatLogEvent(null))
            .extracting(ACTUAL_EXTRACTOR)
            .isNull();
    }

    @Test
    void hasThrowable_shouldReturnThrowableAssertWhenPresent()
    {
        // setup
        final IOException exception = new IOException("Test exception");
        final LogEvent testEvent = newLogEvent(exception);

        // execute & verify
        assertThatLogEvent(testEvent)
            .hasThrowable()
            .isSameAs(exception);
    }

    @Test
    void hasThrowable_shouldFailWhenThrowableIsAbsent()
    {
        // setup
        final LogEvent testEvent = newLogEvent(null);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThatLogEvent(testEvent).hasThrowable())
            .withMessage("Expected log event to have a throwable but found none.");
    }

    @Test
    void hasThrowable_shouldFailWhenLogEventIsNull()
    {
        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThatLogEvent(null).hasThrowable())
            .withMessageContaining("Expecting actual not to be null");
    }

    @ParameterizedTest
    @ValueSource(classes = {IOException.class, Exception.class})
    void hasThrowableOfType_shouldPassForTypeAndSupertypes(Class<? extends Throwable> type)
    {
        // setup
        final IOException exception = new IOException("Test exception");
        final LogEvent testEvent = newLogEvent(exception);

        // execute & verify
        assertThatLogEvent(testEvent)
            .hasThrowableOfType(type)
            .isSameAs(exception);
    }

    @Test
    void hasThrowableOfType_shouldFailForDifferentType()
    {
        // setup
        final IllegalArgumentException exception = new IllegalArgumentException("Test exception");
        final LogEvent testEvent = newLogEvent(exception);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThatLogEvent(testEvent).hasThrowableOfType(IOException.class))
            .withMessageContaining("to be an instance of")
            .withMessageContaining("java.io.IOException");
    }

    @Test
    void hasThrowableOfType_shouldFailWhenThrowableIsAbsent()
    {
        // setup
        final LogEvent testEvent = newLogEvent(null);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThatLogEvent(testEvent).hasThrowableOfType(IOException.class))
            .withMessage("Expected log event to have a throwable of type class java.io.IOException but found none.");
    }

    @Test
    void hasThrowableOfType_shouldFailWhenLogEventIsNull()
    {
        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThatLogEvent(null).hasThrowableOfType(IOException.class))
            .withMessageContaining("Expecting actual not to be null");
    }

    @Test
    void hasThrowableExactlyOfType_shouldPassForExactType()
    {
        // setup
        final IOException exception = new IOException("Test exception");
        final LogEvent testEvent = newLogEvent(exception);

        // execute & verify
        assertThatLogEvent(testEvent)
            .hasThrowableExactlyOfType(IOException.class)
            .isSameAs(exception);
    }

    @Test
    void hasThrowableExactlyOfType_shouldFailForSubtype()
    {
        // setup
        final UncheckedIOException exception = new UncheckedIOException(new IOException("Test exception"));
        final LogEvent testEvent = newLogEvent(exception);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThatLogEvent(testEvent).hasThrowableExactlyOfType(IOException.class))
            .withMessageContaining("to be exactly an instance of")
            .withMessageContaining("java.io.IOException");
    }

    @Test
    void hasThrowableExactlyOfType_shouldFailForSupertype()
    {
        // setup
        final IOException exception = new IOException("Test exception");
        final LogEvent testEvent = newLogEvent(exception);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThatLogEvent(testEvent).hasThrowableExactlyOfType(Exception.class))
            .withMessageContaining("to be exactly an instance of")
            .withMessageContaining("java.lang.Exception");
    }

    @Test
    void hasThrowableExactlyOfType_shouldFailWhenThrowableIsAbsent()
    {
        // setup
        final LogEvent testEvent = newLogEvent(null);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThatLogEvent(testEvent).hasThrowableExactlyOfType(IOException.class))
            .withMessage("Expected log event to have a throwable of type class java.io.IOException but found none.");
    }

    @Test
    void hasThrowableExactlyOfType_shouldFailWhenLogEventIsNull()
    {
        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThatLogEvent(null).hasThrowableExactlyOfType(IOException.class))
            .withMessageContaining("Expecting actual not to be null");
    }

    private LogEvent newLogEvent(@Nullable Throwable throwable)
    {
        return new LogEvent(
            "message",
            "message",
            "INFO",
            "TestLogger",
            "TestThread",
            ZonedDateTime.now(),
            List.of(),
            throwable,
            Map.of(),
            List.of(),
            List.of()
        );
    }
}
