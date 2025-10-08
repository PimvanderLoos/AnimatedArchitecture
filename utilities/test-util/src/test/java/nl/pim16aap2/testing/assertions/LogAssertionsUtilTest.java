package nl.pim16aap2.testing.assertions;

import nl.altindag.log.model.LogEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.Level;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("JavaTimeDefaultTimeZone")
class LogAssertionsUtilTest
{
    @Test
    void constructor_shouldBeSingletonConstructor()
    {
        AssertionsUtil.assertSingletonConstructor(LogAssertionsUtil.class);
    }

    @Test
    void filterByLogLevel_shouldReturnAllEventsWhenLevelIsNull()
    {
        // setup
        final LogEvent event0 = newLogEvent("Test message 0");
        final LogEvent event1 = newLogEvent("Test message 1");
        final LogEvent event2 = newLogEvent("Test message 2");
        final var events = List.of(event0, event1, event2);

        // execute
        final var filteredEvents = LogAssertionsUtil.filterByLogLevel(events, null);

        // verify
        assertThat(filteredEvents)
            .hasSize(3)
            .containsExactly(event0, event1, event2);
    }

    @Test
    void filterByLogLevel_shouldReturnFilteredEvents()
    {
        // setup
        final LogEvent event0 = newLogEvent("INFO", "Test message 0", null);
        final LogEvent event1 = newLogEvent("DEBUG", "Test message 1", null);
        final LogEvent event2 = newLogEvent("INFO", "Test message 2", null);
        final var events = List.of(event0, event1, event2);

        // execute
        final var filteredEvents = LogAssertionsUtil.filterByLogLevel(events, Level.INFO);

        // verify
        assertThat(filteredEvents)
            .hasSize(2)
            .containsExactly(event0, event2);
    }

    @Test
    void testFormatLogEventsPositive()
    {
        final LogEvent event0 = newLogEvent("Test message 0");
        final LogEvent event1 = newLogEvent("Test message 1");
        final LogEvent event2 = newLogEvent("Test message 2");
        final var events = List.of(event0, event1, event2);

        assertThat(LogAssertionsUtil.formatLogEvents(events, 1))
            .isEqualTo(
                """
                    Oldest log messages with their index:
                      [0] [INFO] `Test message 0` from Logger test-logger
                    """
            );

        assertThat(LogAssertionsUtil.formatLogEvents(events, 5))
            .isEqualTo(
                """
                    Oldest log messages with their index:
                      [0] [INFO] `Test message 0` from Logger test-logger
                      [1] [INFO] `Test message 1` from Logger test-logger
                      [2] [INFO] `Test message 2` from Logger test-logger
                    """
            );
    }

    @Test
    void testFormatLogEventsNegative()
    {
        final LogEvent event0 = newLogEvent("Test message 0");
        final LogEvent event1 = newLogEvent("Test message 1");
        final LogEvent event2 = newLogEvent("Test message 2");
        final var events = List.of(event0, event1, event2);

        assertThat(LogAssertionsUtil.formatLogEvents(events, -1))
            .isEqualTo(
                """
                    Most recent log messages with their offset:
                      [-1] [INFO] `Test message 2` from Logger test-logger
                    """
            );

        assertThat(LogAssertionsUtil.formatLogEvents(events, -5))
            .isEqualTo(
                """
                    Most recent log messages with their offset:
                      [-1] [INFO] `Test message 2` from Logger test-logger
                      [-2] [INFO] `Test message 1` from Logger test-logger
                      [-3] [INFO] `Test message 0` from Logger test-logger
                    """
            );
    }

    private LogEvent newLogEvent(@Nullable String level, String message, @Nullable Throwable throwable)
    {
        //noinspection DataFlowIssue
        return new LogEvent(
            message,
            message,
            level,
            "test-logger",
            "TestThread",
            ZonedDateTime.now(),
            List.of(),
            throwable,
            Map.of(),
            List.of(),
            List.of()
        );
    }

    private LogEvent newLogEvent(String message)
    {
        return newLogEvent("INFO", message, null);
    }
}
