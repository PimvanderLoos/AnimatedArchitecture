package nl.pim16aap2.testing.assertions;

import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class LogCaptorAssertTest
{
    @Mock
    private LogCaptor logCaptor;

    @BeforeEach
    void setUp()
    {
        lenient().when(logCaptor.getLogEvents()).thenReturn(List.of());
        lenient().when(logCaptor.getLogs()).thenReturn(List.of());
    }

    @Test
    void assertThatLogCaptor_shouldReturnLogCaptorAssertInstance()
    {
        // execute
        final LogCaptorAssert result = LogCaptorAssert.assertThatLogCaptor(logCaptor);

        // verify
        assertThat(result).isInstanceOf(LogCaptorAssert.class);
        assertThat(result).extracting("actual").isSameAs(logCaptor);
    }

    @Test
    void constructor_shouldInitializeWithLogCaptor()
    {
        // execute
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);

        // verify
        assertThat(logCaptorAssert).extracting("actual").isSameAs(logCaptor);
    }

    @Test
    void failWithMessage_shouldAppendFormattedLogEvents()
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);
        final LogEvent mockEvent = createMockLogEvent("ERROR", "Test error message", null);

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
        final LogEvent errorEvent = createMockLogEvent("ERROR", "Error message", null);
        final LogEvent infoEvent = createMockLogEvent("INFO", "Info message", null);

        when(logCaptor.getLogEvents()).thenReturn(List.of(errorEvent, infoEvent));

        // execute
        final LogEventsAssert result = logCaptorAssert.atAllLevels();

        // verify
        assertThat(result).isInstanceOf(LogEventsAssert.class);
        assertThat(result).extracting("actual").asInstanceOf(LIST).hasSize(2);
    }

    @Test
    void atError_shouldReturnLogEventsAssertWithErrorEventsOnly()
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);
        final LogEvent errorEvent = createMockLogEvent("ERROR", "Error message", null);
        final LogEvent infoEvent = createMockLogEvent("INFO", "Info message", null);

        when(logCaptor.getLogEvents()).thenReturn(List.of(errorEvent, infoEvent));

        // execute
        final LogEventsAssert result = logCaptorAssert.atError();

        // verify
        assertThat(result).isInstanceOf(LogEventsAssert.class);
        assertThat(result).extracting("actual").asInstanceOf(LIST).hasSize(1);
    }

    @Test
    void atWarn_shouldReturnLogEventsAssertWithWarnEventsOnly()
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);
        final LogEvent warnEvent = createMockLogEvent("WARN", "Warn message", null);
        final LogEvent infoEvent = createMockLogEvent("INFO", "Info message", null);

        when(logCaptor.getLogEvents()).thenReturn(List.of(warnEvent, infoEvent));

        // execute
        final LogEventsAssert result = logCaptorAssert.atWarn();

        // verify
        assertThat(result).isInstanceOf(LogEventsAssert.class);
        assertThat(result).extracting("actual").asInstanceOf(LIST).hasSize(1);
    }

    @Test
    void atInfo_shouldReturnLogEventsAssertWithInfoEventsOnly()
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);
        final LogEvent infoEvent = createMockLogEvent("INFO", "Info message", null);
        final LogEvent debugEvent = createMockLogEvent("DEBUG", "Debug message", null);
        when(logCaptor.getLogEvents()).thenReturn(List.of(infoEvent, debugEvent));

        // execute
        final LogEventsAssert result = logCaptorAssert.atInfo();

        // verify
        assertThat(result).isInstanceOf(LogEventsAssert.class);
        assertThat(result).extracting("actual").asInstanceOf(LIST).hasSize(1);
    }

    @Test
    void atDebug_shouldReturnLogEventsAssertWithDebugEventsOnly()
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);
        final LogEvent debugEvent = createMockLogEvent("DEBUG", "Debug message", null);
        final LogEvent traceEvent = createMockLogEvent("TRACE", "Trace message", null);

        when(logCaptor.getLogEvents()).thenReturn(List.of(debugEvent, traceEvent));

        // execute
        final LogEventsAssert result = logCaptorAssert.atDebug();

        // verify
        assertThat(result).isInstanceOf(LogEventsAssert.class);
        assertThat(result).extracting("actual").asInstanceOf(LIST).hasSize(1);
    }

    @Test
    void atTrace_shouldReturnLogEventsAssertWithTraceEventsOnly()
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);
        final LogEvent traceEvent = createMockLogEvent("TRACE", "Trace message", null);
        final LogEvent debugEvent = createMockLogEvent("DEBUG", "Debug message", null);

        when(logCaptor.getLogEvents()).thenReturn(List.of(traceEvent, debugEvent));

        // execute
        final LogEventsAssert result = logCaptorAssert.atTrace();

        // verify
        assertThat(result).isInstanceOf(LogEventsAssert.class);
        assertThat(result).extracting("actual").asInstanceOf(LIST).hasSize(1);
    }

    @Test
    void hasNoLogs_shouldPassWhenNoLogsExist()
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);

        when(logCaptor.getLogs()).thenReturn(List.of());

        // execute & verify
        assertThatNoException().isThrownBy(logCaptorAssert::hasNoLogs);
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

    @Test
    void hasNoErrorLogs_shouldPassWhenNoErrorLogsExist()
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);
        final LogEvent infoEvent = createMockLogEvent("INFO", "Info message", null);

        when(logCaptor.getLogEvents()).thenReturn(List.of(infoEvent));

        // execute & verify
        assertThatNoException().isThrownBy(logCaptorAssert::hasNoErrorLogs);
    }

    @Test
    void hasNoErrorLogs_shouldFailWhenErrorLogsExist()
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);
        final LogEvent errorEvent = createMockLogEvent("ERROR", "Error message", null);

        when(logCaptor.getLogEvents()).thenReturn(List.of(errorEvent));

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(logCaptorAssert::hasNoErrorLogs)
            .withMessageContaining("Expected no logs of level 'ERROR', but found 1");
    }

    @Test
    void hasNoWarnLogs_shouldPassWhenNoWarnLogsExist()
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);
        final LogEvent infoEvent = createMockLogEvent("INFO", "Info message", null);

        when(logCaptor.getLogEvents()).thenReturn(List.of(infoEvent));

        // execute & verify
        assertThatNoException().isThrownBy(logCaptorAssert::hasNoWarnLogs);
    }

    @Test
    void hasNoWarnLogs_shouldFailWhenWarnLogsExist()
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);
        final LogEvent warnEvent = createMockLogEvent("WARN", "Warn message", null);

        when(logCaptor.getLogEvents()).thenReturn(List.of(warnEvent));

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(logCaptorAssert::hasNoWarnLogs)
            .withMessageContaining("Expected no logs of level 'WARN', but found 1");
    }

    @Test
    void hasNoInfoLogs_shouldPassWhenNoInfoLogsExist()
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);
        final LogEvent debugEvent = createMockLogEvent("DEBUG", "Debug message", null);

        when(logCaptor.getLogEvents()).thenReturn(List.of(debugEvent));

        // execute & verify
        assertThatNoException().isThrownBy(logCaptorAssert::hasNoInfoLogs);
    }

    @Test
    void hasNoInfoLogs_shouldFailWhenInfoLogsExist()
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);
        final LogEvent infoEvent = createMockLogEvent("INFO", "Info message", null);

        when(logCaptor.getLogEvents()).thenReturn(List.of(infoEvent));

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(logCaptorAssert::hasNoInfoLogs)
            .withMessageContaining("Expected no logs of level 'INFO', but found 1");
    }

    @Test
    void hasNoDebugLogs_shouldPassWhenNoDebugLogsExist()
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);
        final LogEvent infoEvent = createMockLogEvent("INFO", "Info message", null);

        when(logCaptor.getLogEvents()).thenReturn(List.of(infoEvent));

        // execute & verify
        assertThatNoException().isThrownBy(logCaptorAssert::hasNoDebugLogs);
    }

    @Test
    void hasNoDebugLogs_shouldFailWhenDebugLogsExist()
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);
        final LogEvent debugEvent = createMockLogEvent("DEBUG", "Debug message", null);

        when(logCaptor.getLogEvents()).thenReturn(List.of(debugEvent));

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(logCaptorAssert::hasNoDebugLogs)
            .withMessageContaining("Expected no logs of level 'DEBUG', but found 1");
    }

    @Test
    void hasNoTraceLogs_shouldPassWhenNoTraceLogsExist()
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);
        final LogEvent debugEvent = createMockLogEvent("DEBUG", "Debug message", null);

        when(logCaptor.getLogEvents()).thenReturn(List.of(debugEvent));

        // execute & verify
        assertThatNoException().isThrownBy(logCaptorAssert::hasNoTraceLogs);
    }

    @Test
    void hasNoTraceLogs_shouldFailWhenTraceLogsExist()
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);
        final LogEvent traceEvent = createMockLogEvent("TRACE", "Trace message", null);

        when(logCaptor.getLogEvents()).thenReturn(List.of(traceEvent));

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(logCaptorAssert::hasNoTraceLogs)
            .withMessageContaining("Expected no logs of level 'TRACE', but found 1");
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
        assertThat(result).isInstanceOf(LogEventsAssert.class);
        assertThat(result).extracting("actual").asInstanceOf(LIST).isEmpty();
    }

    @Test
    void atError_shouldReturnEmptyLogEventsAssertWhenNoErrorEvents()
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);
        final LogEvent infoEvent = createMockLogEvent("INFO", "Info message", null);

        when(logCaptor.getLogEvents()).thenReturn(List.of(infoEvent));

        // execute
        final LogEventsAssert result = logCaptorAssert.atError();

        // verify
        assertThat(result).isInstanceOf(LogEventsAssert.class);
        assertThat(result).extracting("actual").asInstanceOf(LIST).isEmpty();
    }

    @Test
    void hasNoErrorLogs_shouldReturnSelfForChaining()
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);

        when(logCaptor.getLogEvents()).thenReturn(List.of());

        // execute
        final LogCaptorAssert result = logCaptorAssert.hasNoErrorLogs();

        // verify
        assertThat(result).isSameAs(logCaptorAssert);
    }

    @Test
    void hasNoLogs_shouldReturnSelfForChaining()
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);

        when(logCaptor.getLogs()).thenReturn(List.of());

        // execute
        final LogCaptorAssert result = logCaptorAssert.hasNoLogs();

        // verify
        assertThat(result).isSameAs(logCaptorAssert);
    }

    @Test
    void hasNoLogsOfLevel_shouldHandleMultipleLogsOfSameLevel()
    {
        // setup
        final LogEvent errorEvent1 = createMockLogEvent("ERROR", "Error message 1", null);
        final LogEvent errorEvent2 = createMockLogEvent("ERROR", "Error message 2", null);
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
        final LogEvent errorEvent1 = createMockLogEvent("ERROR", "Error message 1", null);
        final LogEvent infoEvent = createMockLogEvent("INFO", "Info message", null);
        final LogEvent errorEvent2 = createMockLogEvent("ERROR", "Error message 2", null);
        final LogEvent warnEvent = createMockLogEvent("WARN", "Warn message", null);

        when(logCaptor.getLogEvents()).thenReturn(List.of(errorEvent1, infoEvent, errorEvent2, warnEvent));

        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);

        // execute
        final LogEventsAssert errorResult = logCaptorAssert.atError();
        final LogEventsAssert warnResult = logCaptorAssert.atWarn();
        final LogEventsAssert infoResult = logCaptorAssert.atInfo();

        // verify
        assertThat(errorResult).extracting("actual").asInstanceOf(LIST).hasSize(2);
        assertThat(warnResult).extracting("actual").asInstanceOf(LIST).hasSize(1);
        assertThat(infoResult).extracting("actual").asInstanceOf(LIST).hasSize(1);
    }

    /**
     * Creates a mock LogEvent with the specified properties.
     *
     * @param level
     *     The log level (e.g., "ERROR", "INFO", "DEBUG", etc.)
     * @param message
     *     The log message
     * @param throwable
     *     Optional throwable associated with the log event
     * @return A mocked LogEvent with the specified properties
     */
    private LogEvent createMockLogEvent(String level, String message, @Nullable Throwable throwable)
    {
        LogEvent mockEvent = mock(LogEvent.class);
        lenient().when(mockEvent.getLevel()).thenReturn(level);
        lenient().when(mockEvent.getMessage()).thenReturn(message);
        lenient().when(mockEvent.getFormattedMessage()).thenReturn(message);
        lenient().when(mockEvent.getThrowable()).thenReturn(Optional.ofNullable(throwable));
        lenient().when(mockEvent.getLoggerName()).thenReturn("TestLogger");
        return mockEvent;
    }
}