package nl.pim16aap2.testing.assertions;

import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.opentest4j.AssertionFailedError;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static nl.pim16aap2.testing.assertions.LogAssertionsUtil.MessageComparisonMethod;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LogAssertionsUtilTest
{
    @Mock
    private LogCaptor logCaptor;

    @Test
    void testAssertLoggedWithMessageAndPosition()
    {
        final LogEvent event1 = newLogEvent("Test message 1");
        final LogEvent event2 = newLogEvent("Test message 2");
        final LogEvent event3 = newLogEvent("Test message 3");

        Mockito.when(logCaptor.getLogEvents()).thenReturn(List.of(event1, event2, event3));

        Assertions.assertDoesNotThrow(
            () -> LogAssertionsUtil.assertLogged(logCaptor, 1, "Test message 2"));

        Assertions.assertThrows(
            AssertionFailedError.class,
            () -> LogAssertionsUtil.assertLogged(logCaptor, 1, "Non-existent message")
        );
    }

    @Test
    void testGetLogEventNonNegativePosition()
    {
        final LogEvent event0 = newLogEvent("Test message 0");
        final LogEvent event1 = newLogEvent("Test message 1");
        final LogEvent event2 = newLogEvent("Test message 2");

        Mockito.when(logCaptor.getLogEvents()).thenReturn(List.of(event0, event1, event2));

        Assertions.assertDoesNotThrow(
            () -> LogAssertionsUtil.assertLogged(logCaptor, 0, "Test message 0"));

        Assertions.assertDoesNotThrow(
            () -> LogAssertionsUtil.assertLogged(logCaptor, 1, "Test message 1"));

        Assertions.assertDoesNotThrow(
            () -> LogAssertionsUtil.assertLogged(logCaptor, 2, "Test message 2"));

        // Test out of bounds
        Assertions.assertThrows(
            AssertionFailedError.class,
            () -> LogAssertionsUtil.assertLogged(logCaptor, 3, "Test message 3")
        );
    }

    @Test
    void testFormatLogEventsNonNegative()
    {
        final LogEvent event0 = newLogEvent("Test message 0");
        final LogEvent event1 = newLogEvent("Test message 1");
        final LogEvent event2 = newLogEvent("Test message 2");
        final var events = List.of(event0, event1, event2);

        Assertions.assertEquals(
            """
                Oldest log messages with their index:
                  [0] [INFO] `Test message 0` from Logger null
                """,
            LogAssertionsUtil.formatLogEvents(events, 1)
        );

        Assertions.assertEquals(
            """
                Oldest log messages with their index:
                  [0] [INFO] `Test message 0` from Logger null
                  [1] [INFO] `Test message 1` from Logger null
                  [2] [INFO] `Test message 2` from Logger null
                """,
            LogAssertionsUtil.formatLogEvents(events, 5)
        );
    }

    @Test
    void testFormatLogEventsNegative()
    {
        final LogEvent event0 = newLogEvent("Test message 0");
        final LogEvent event1 = newLogEvent("Test message 1");
        final LogEvent event2 = newLogEvent("Test message 2");
        final var events = List.of(event0, event1, event2);

        Assertions.assertEquals(
            """
                Most recent log messages with their offset:
                  [-1] [INFO] `Test message 2` from Logger null
                """,
            LogAssertionsUtil.formatLogEvents(events, -1)
        );

        Assertions.assertEquals(
            """
                Most recent log messages with their offset:
                  [-1] [INFO] `Test message 2` from Logger null
                  [-2] [INFO] `Test message 1` from Logger null
                  [-3] [INFO] `Test message 0` from Logger null
                """,
            LogAssertionsUtil.formatLogEvents(events, -5)
        );
    }

    @Test
    void testGetLogEventNegativePosition()
    {
        final LogEvent event0 = newLogEvent("Test message 0");
        final LogEvent event1 = newLogEvent("Test message 1");
        final LogEvent event2 = newLogEvent("Test message 2");

        Mockito.when(logCaptor.getLogEvents()).thenReturn(List.of(event0, event1, event2));

        // Implicitly -1
        Assertions.assertDoesNotThrow(
            () -> LogAssertionsUtil.assertLogged(logCaptor, "Test message 2"));

        // Implicitly -1
        Assertions.assertDoesNotThrow(
            () -> LogAssertionsUtil.assertLogged(logCaptor, "Test message 2", MessageComparisonMethod.EQUALS));

        Assertions.assertDoesNotThrow(
            () -> LogAssertionsUtil.assertLogged(logCaptor, -1, "Test message 2"));

        Assertions.assertDoesNotThrow(
            () -> LogAssertionsUtil.assertLogged(logCaptor, -2, "Test message 1"));

        Assertions.assertDoesNotThrow(
            () -> LogAssertionsUtil.assertLogged(logCaptor, -3, "Test message 0"));

        // Test out of bounds
        Assertions.assertThrows(
            AssertionFailedError.class,
            () -> LogAssertionsUtil.assertLogged(logCaptor, -4, "Test message 0")
        );
    }

    @Test
    void testAssertLoggedWithDifferentComparisonMethods()
    {
        final String message = "Test message for comparison";
        final LogEvent event = newLogEvent(message);

        Mockito.when(logCaptor.getLogEvents()).thenReturn(List.of(event));

        Assertions.assertDoesNotThrow(
            () -> LogAssertionsUtil.assertLogged(logCaptor, 0, message, MessageComparisonMethod.EQUALS));

        Assertions.assertDoesNotThrow(
            () -> LogAssertionsUtil.assertLogged(logCaptor, 0, "message for", MessageComparisonMethod.CONTAINS));

        Assertions.assertDoesNotThrow(
            () -> LogAssertionsUtil.assertLogged(logCaptor, 0, "Test message", MessageComparisonMethod.STARTS_WITH));

        Assertions.assertDoesNotThrow(
            () -> LogAssertionsUtil.assertLogged(logCaptor, 0, "comparison", MessageComparisonMethod.ENDS_WITH));

        Assertions.assertThrows(
            AssertionFailedError.class,
            () -> LogAssertionsUtil.assertLogged(logCaptor, 0, "non-existent message", MessageComparisonMethod.EQUALS)
        );
    }

    @Test
    void testAssertThrowableNestingWithNestedExceptions()
    {
        final Throwable nestedException = new IOException("Cause", new NullPointerException("Root cause"));

        Assertions.assertDoesNotThrow(() -> LogAssertionsUtil.assertThrowableNesting(
            nestedException, IOException.class, NullPointerException.class));

        // Incorrect nesting
        Assertions.assertThrows(AssertionFailedError.class, () -> LogAssertionsUtil.assertThrowableNesting(
            nestedException, IOException.class, NullPointerException.class, NullPointerException.class));

        // Incorrect nesting
        Assertions.assertThrows(AssertionFailedError.class, () -> LogAssertionsUtil.assertThrowableNesting(
            nestedException, NullPointerException.class, IOException.class));

        // Incorrect types (second)
        Assertions.assertThrows(AssertionFailedError.class, () -> LogAssertionsUtil.assertThrowableNesting(
            nestedException, IOException.class, IllegalArgumentException.class));

        // Incorrect types (first)
        Assertions.assertThrows(AssertionFailedError.class, () -> LogAssertionsUtil.assertThrowableNesting(
            nestedException, RuntimeException.class, NullPointerException.class));
    }

    @Test
    void assertThrowableNestingIllegalArgument()
    {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> LogAssertionsUtil.assertThrowableNesting(
                new RuntimeException(), new LogAssertionsUtil.ThrowableSpec[0])
        );
    }

    @Test
    void testAssertThrowableLoggedWithSpecificTypesAndMessages()
    {
        final Throwable nestedException = new RuntimeException("Cause", new NullPointerException("Root cause"));

        final LogEvent event = newLogEvent("ERROR", "Exception occurred", nestedException);

        Mockito.when(logCaptor.getLogEvents()).thenReturn(List.of(event));

        Assertions.assertDoesNotThrow(() -> LogAssertionsUtil.assertThrowableLogged(
            logCaptor, -1, "Exception occurred",
            RuntimeException.class, "Cause",
            NullPointerException.class, "Root cause")
        );

        Assertions.assertDoesNotThrow(() -> LogAssertionsUtil.assertThrowableLogged(
            logCaptor, -1, null,
            RuntimeException.class, "Cause",
            NullPointerException.class, "Root cause")
        );

        // Incorrect type
        Assertions.assertThrows(
            AssertionFailedError.class,
            () -> LogAssertionsUtil.assertThrowableLogged(
                logCaptor, -1, "Exception occurred", IOException.class, "Cause")
        );

        // Incorrect message
        Assertions.assertThrows(
            AssertionFailedError.class,
            () -> LogAssertionsUtil.assertThrowableLogged(
                logCaptor, -1, "Exception occurred", RuntimeException.class, "Wrong message")
        );
    }

    @Test
    void getThrowingCountTest()
    {
        final LogEvent event0 = newLogEvent("Test message 0");
        final LogEvent event1 = newLogEvent("Test message 1", IOException.class);
        final LogEvent event2 = newLogEvent("Test message 2", RuntimeException.class);

        Mockito.when(logCaptor.getLogEvents()).thenReturn(List.of(event0, event1, event2));
        Assertions.assertEquals(2, LogAssertionsUtil.getThrowingLogEvents(logCaptor).size());

        Mockito.when(logCaptor.getLogEvents()).thenReturn(List.of(event0, event1));
        Assertions.assertEquals(1, LogAssertionsUtil.getThrowingLogEvents(logCaptor).size());

        Mockito.when(logCaptor.getLogEvents()).thenReturn(List.of(event0));
        Assertions.assertEquals(0, LogAssertionsUtil.getThrowingLogEvents(logCaptor).size());
    }

    private LogEvent newLogEvent(String message, Class<? extends Throwable> throwableType)
    {
        return newLogEvent("INFO", message, throwableType);
    }

    private LogEvent newLogEvent(@Nullable String level, String message, Class<? extends Throwable> throwableType)
    {
        return newLogEvent(level, message, Mockito.mock(throwableType));
    }

    private LogEvent newLogEvent(@Nullable String level, String message, @Nullable Throwable throwable)
    {
        final var ret = Mockito.mock(LogEvent.class);
        Mockito.when(ret.getLevel()).thenReturn(level);
        Mockito.when(ret.getMessage()).thenReturn(message);
        Mockito.when(ret.getThrowable()).thenReturn(Optional.ofNullable(throwable));
        return ret;
    }

    private LogEvent newLogEvent(String message)
    {
        return newLogEvent("INFO", message, (Throwable) null);
    }
}
