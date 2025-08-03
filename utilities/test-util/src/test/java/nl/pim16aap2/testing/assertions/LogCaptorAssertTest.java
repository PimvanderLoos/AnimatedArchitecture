package nl.pim16aap2.testing.assertions;

import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.spi.StandardLevel;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class LogCaptorAssertTest
{
    private static final Function<LogCaptorAssert, LogCaptor> ACTUAL_EXTRACTOR = LogCaptorAssert::actual;
    private static final Function<LogEventsAssert, List<LogEvent>> LIST_EXTRACTOR = LogEventsAssert::actual;

    @Mock
    private LogCaptor logCaptor;

    @Test
    void assertThatLogCaptor_shouldReturnLogCaptorAssertInstance()
    {
        // execute
        final LogCaptorAssert result = LogCaptorAssert.assertThatLogCaptor(logCaptor);

        // verify
        assertThat(result)
            .extracting(ACTUAL_EXTRACTOR)
            .isSameAs(logCaptor);
    }

    @Test
    void constructor_shouldInitializeWithLogCaptor()
    {
        // execute
        final LogCaptorAssert result = new LogCaptorAssert(logCaptor);

        // verify
        assertThat(result)
            .extracting(ACTUAL_EXTRACTOR)
            .isSameAs(logCaptor);
    }

    @Test
    void failWithMessage_shouldAppendFormattedLogEvents()
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);
        final LogEvent mockEvent = newLogEvent("ERROR", "Test error message", null);

        when(logCaptor.getLogEvents()).thenReturn(List.of(mockEvent));

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logCaptorAssert.failWithMessage("Custom error"))
            .withMessageContaining("Custom error")
            .withMessageContaining("Test error message")
            .withMessageContaining("[ERROR]");
    }

    @Test
    void atAllLevels_shouldReturnLogEventsAssertWithAllEvents()
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);
        populateLogCaptorWithEventsForEachLevel(logCaptor);

        // execute
        final LogEventsAssert result = logCaptorAssert.atAllLevels();

        // verify
        assertThat(result)
            .isInstanceOf(LogEventsAssert.class)
            .extracting(LIST_EXTRACTOR, LIST)
            .hasSize(Level.values().length);
    }

    @Test
    void atAllLevels_shouldReturnEmptyLogEventsAssertWhenNoEvents()
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);

        when(logCaptor.getLogEvents()).thenReturn(List.of());

        // execute
        final LogEventsAssert result = logCaptorAssert.atAllLevels();

        // verify
        assertThat(result)
            .isInstanceOf(LogEventsAssert.class)
            .extracting(LIST_EXTRACTOR, LIST)
            .isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = StandardLevel.class, mode = EnumSource.Mode.EXCLUDE, names = {"OFF", "ALL"})
    void atLevelX_shouldReturnLogEventsAssertForSpecificLevel(StandardLevel standardLevel)
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);
        populateLogCaptorWithEventsForEachLevel(logCaptor);

        // execute
        final LogEventsAssert result = switch (standardLevel)
        {
            case FATAL -> logCaptorAssert.atFatal();
            case ERROR -> logCaptorAssert.atError();
            case WARN -> logCaptorAssert.atWarn();
            case INFO -> logCaptorAssert.atInfo();
            case DEBUG -> logCaptorAssert.atDebug();
            case TRACE -> logCaptorAssert.atTrace();
            default -> throw new IllegalArgumentException("Unsupported log level: " + standardLevel);
        };

        // verify
        assertThat(result)
            .isInstanceOf(LogEventsAssert.class)
            .extracting(LIST_EXTRACTOR, LIST)
            .singleElement()
            .asInstanceOf(InstanceOfAssertFactories.type(LogEvent.class))
            .satisfies(logEvent ->
            {
                assertThat(logEvent.getLevel()).isEqualTo(standardLevel.name());
                assertThat(logEvent.getMessage()).contains("Test message for " + standardLevel.name());
            });
    }

    @ParameterizedTest
    @EnumSource(value = StandardLevel.class, mode = EnumSource.Mode.EXCLUDE, names = {"OFF", "ALL"})
    void atLevelX_shouldReturnEmptyLogEventsAssertWhenNoEvents(StandardLevel standardLevel)
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);

        when(logCaptor.getLogEvents()).thenReturn(List.of());

        // execute
        final LogEventsAssert result = switch (standardLevel)
        {
            case FATAL -> logCaptorAssert.atFatal();
            case ERROR -> logCaptorAssert.atError();
            case WARN -> logCaptorAssert.atWarn();
            case INFO -> logCaptorAssert.atInfo();
            case DEBUG -> logCaptorAssert.atDebug();
            case TRACE -> logCaptorAssert.atTrace();
            default -> throw new IllegalArgumentException("Unsupported log level: " + standardLevel);
        };

        // verify
        assertThat(result)
            .isInstanceOf(LogEventsAssert.class)
            .extracting(LIST_EXTRACTOR, LIST)
            .isEmpty();
    }

    @Test
    void hasNoLogs_shouldPassWhenNoLogsExist()
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);

        when(logCaptor.getLogs()).thenReturn(List.of());

        // execute & verify
        assertThatNoException()
            .isThrownBy(logCaptorAssert::hasNoLogs);
    }

    @Test
    void hasNoLogs_shouldFailWhenLogsExist()
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);

        when(logCaptor.getLogs()).thenReturn(List.of("Log message"));

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(logCaptorAssert::hasNoLogs)
            .withMessageContaining("Expected no logs, but found:");
    }

    @ParameterizedTest
    @EnumSource(value = StandardLevel.class, mode = EnumSource.Mode.EXCLUDE, names = {"OFF", "ALL"})
    void hasNoLogs_shouldPassWhenNoErrorLogsExist(StandardLevel standardLevel)
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);
        when(logCaptor.getLogEvents()).thenReturn(List.of());

        // execute
        final Supplier<LogCaptorAssert> result = getHasNoLogsOfLevelMethod(logCaptorAssert, standardLevel);

        // verify
        assertThatNoException()
            .isThrownBy(result::get);
    }

    @Test
    void atError_shouldReturnEmptyLogEventsAssertWhenNoErrorEvents()
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);
        final LogEvent infoEvent = newLogEvent("INFO", "Info message", null);

        when(logCaptor.getLogEvents()).thenReturn(List.of(infoEvent));

        // execute
        final LogEventsAssert result = logCaptorAssert.atError();

        // verify
        assertThat(result)
            .isInstanceOf(LogEventsAssert.class)
            .extracting(LIST_EXTRACTOR, LIST)
            .isEmpty();
    }

    @Test
    void hasNoLogsOfLevel_shouldHandleMultipleLogsOfSameLevel()
    {
        // setup
        final LogEvent errorEvent1 = newLogEvent("ERROR", "Error message 1", null);
        final LogEvent errorEvent2 = newLogEvent("ERROR", "Error message 2", null);
        when(logCaptor.getLogEvents()).thenReturn(List.of(errorEvent1, errorEvent2));

        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(logCaptorAssert::hasNoErrorLogs)
            .withMessageContaining("Expected no logs of level 'ERROR', but found 2");
    }

    @Test
    void atLevel_shouldFilterCorrectlyWithMixedEvents()
    {
        // setup
        final LogEvent errorEvent1 = newLogEvent("ERROR", "Error message 1", null);
        final LogEvent infoEvent = newLogEvent("INFO", "Info message", null);
        final LogEvent errorEvent2 = newLogEvent("ERROR", "Error message 2", null);
        final LogEvent warnEvent = newLogEvent("WARN", "Warn message", null);

        when(logCaptor.getLogEvents()).thenReturn(List.of(errorEvent1, infoEvent, errorEvent2, warnEvent));

        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);

        // execute
        final LogEventsAssert errorResult = logCaptorAssert.atError();
        final LogEventsAssert warnResult = logCaptorAssert.atWarn();
        final LogEventsAssert infoResult = logCaptorAssert.atInfo();

        // verify
        assertThat(errorResult)
            .extracting(LIST_EXTRACTOR, LIST)
            .hasSize(2);

        assertThat(warnResult)
            .extracting(LIST_EXTRACTOR, LIST)
            .hasSize(1);

        assertThat(infoResult)
            .extracting(LIST_EXTRACTOR, LIST)
            .hasSize(1);
    }

    private void populateLogCaptorWithEventsForEachLevel(LogCaptor logCaptor)
    {
        final List<LogEvent> logEvents = Stream.of(Level.values())
            .map(level -> newLogEvent(level.name(), "Test message for " + level.name(), null))
            .toList();

        when(logCaptor.getLogEvents()).thenReturn(logEvents);
    }

    private LogEvent newLogEvent(String level, String message, @Nullable Throwable throwable)
    {
        return new LogEvent(
            message,
            message,
            level,
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

    private Supplier<LogCaptorAssert> getHasNoLogsOfLevelMethod(
        LogCaptorAssert logCaptorAssert,
        StandardLevel standardLevel)
    {
        return switch (standardLevel)
        {
            case FATAL -> logCaptorAssert::hasNoFatalLogs;
            case ERROR -> logCaptorAssert::hasNoErrorLogs;
            case WARN -> logCaptorAssert::hasNoWarnLogs;
            case INFO -> logCaptorAssert::hasNoInfoLogs;
            case DEBUG -> logCaptorAssert::hasNoDebugLogs;
            case TRACE -> logCaptorAssert::hasNoTraceLogs;
            default -> throw new IllegalArgumentException("Unsupported log level: " + standardLevel);
        };
    }
}