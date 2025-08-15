package nl.pim16aap2.testing.assertions;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class LogCaptorAssertTest
{
    private static final Function<LogCaptorAssert, LogCaptor> ACTUAL_EXTRACTOR = LogCaptorAssert::actual;
    private static final Function<LogEventsAssert, List<LogEvent>> LIST_EXTRACTOR = LogEventsAssert::actual;

    private LogCaptor logCaptor;

    @BeforeEach
    void beforeEach()
    {
        logCaptor = spy(LogCaptor.forClass(LogCaptorAssertTest.class));
    }

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
            .hasSize(LevelWrapper.BASE_LOG_LEVELS.size());
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
    @EnumSource(LevelWrapper.class)
    void atLevelX_shouldReturnLogEventsAssertForSpecificLevel(LevelWrapper levelWrapper)
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);
        populateLogCaptorWithEventsForEachLevel(logCaptor);

        // execute
        final LogEventsAssert result = levelWrapper.atLevelMethod().apply(logCaptorAssert);

        // verify
        assertThat(result)
            .isInstanceOf(LogEventsAssert.class)
            .extracting(LIST_EXTRACTOR, LIST)
            .singleElement()
            .asInstanceOf(InstanceOfAssertFactories.type(LogEvent.class))
            .satisfies(logEvent ->
            {
                assertThat(logEvent.getLevel()).isEqualTo(levelWrapper.levelName());
                assertThat(logEvent.getMessage()).contains("Test message for " + levelWrapper.levelName());
            });
    }

    @ParameterizedTest
    @EnumSource(LevelWrapper.class)
    void atLevelX_shouldReturnEmptyLogEventsAssertWhenNoEvents(LevelWrapper levelWrapper)
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);

        when(logCaptor.getLogEvents()).thenReturn(List.of());

        // execute
        final LogEventsAssert result = levelWrapper.atLevelMethod().apply(logCaptorAssert);

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
    @EnumSource(LevelWrapper.class)
    void hasNoLogs_shouldPassWhenNoErrorLogsExist(LevelWrapper levelWrapper)
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);
        when(logCaptor.getLogEvents()).thenReturn(List.of());

        // execute
        final UnaryOperator<LogCaptorAssert> result = levelWrapper.hasNoLogsMethod();

        // verify
        assertThatNoException()
            .isThrownBy(() -> result.apply(logCaptorAssert));
    }

    @Test
    void atSevere_shouldReturnEmptyLogEventsAssertWhenNoErrorEvents()
    {
        // setup
        final LogCaptorAssert logCaptorAssert = new LogCaptorAssert(logCaptor);
        final LogEvent infoEvent = newLogEvent("INFO", "Info message", null);

        when(logCaptor.getLogEvents()).thenReturn(List.of(infoEvent));

        // execute
        final LogEventsAssert result = logCaptorAssert.atSevere();

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
            .isThrownBy(logCaptorAssert::hasNoSevereLogs)
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
        final LogEventsAssert errorResult = logCaptorAssert.atSevere();
        final LogEventsAssert warnResult = logCaptorAssert.atWarning();
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
        final List<LogEvent> logEvents = LevelWrapper.BASE_LOG_LEVELS.stream()
            .map(level -> newLogEvent(level.levelName(), "Test message for " + level.levelName(), null))
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

    @Getter
    @Accessors(fluent = true)
    @RequiredArgsConstructor
    private enum LevelWrapper
    {
        //        // log4j2 levels
//        LOG4J2_FATAL(Level.FATAL.name(), LogCaptorAssert::hasNoSevereLogs, LogCaptorAssert::atFatal),
//        LOG4J2_ERROR(Level.ERROR.name(), LogCaptorAssert::hasNoErrorLogs, LogCaptorAssert::atSevere),
//        LOG4J2_WARN(Level.WARN.name(), LogCaptorAssert::hasNoWarnLogs, LogCaptorAssert::atWarning),
//        LOG4J2_INFO(Level.INFO.name(), LogCaptorAssert::hasNoInfoLogs, LogCaptorAssert::atInfo),
//        LOG4J2_DEBUG(Level.DEBUG.name(), LogCaptorAssert::hasNoDebugLogs, LogCaptorAssert::atDebug),
//        LOG4J2_TRACE(Level.TRACE.name(), LogCaptorAssert::hasNoTraceLogs, LogCaptorAssert::atTrace),
//
//        // custom levels
//        CUSTOM_CONF(CustomLevel.CONF.name(), LogCaptorAssert::hasNoConfLogs, LogCaptorAssert::atConf),
//        CUSTOM_FINER(CustomLevel.FINER.name(), LogCaptorAssert::hasNoFinerLogs, LogCaptorAssert::atFiner),
//
//        // Unmapped Java logging levels
//        // These have their own methods in LogCaptorAssert that basically directly map to the log4j2 levels
//        JUL_SEVERE(Level.ERROR.name(), LogCaptorAssert::hasNoSevereLogs, LogCaptorAssert::atSevere),
//        JUL_FINE(Level.DEBUG.name(), LogCaptorAssert::hasNoFineLogs, LogCaptorAssert::atFine),
//        JUL_FINEST(Level.TRACE.name(), LogCaptorAssert::hasNoFinestLogs, LogCaptorAssert::atFinest),
//        ;
        JUL_SEVERE("SEVERE", LogCaptorAssert::hasNoSevereLogs, LogCaptorAssert::atSevere),
        JUL_WARNING("WARNING", LogCaptorAssert::hasNoWarningLogs, LogCaptorAssert::atWarning),
        JUL_INFO("INFO", LogCaptorAssert::hasNoInfoLogs, LogCaptorAssert::atInfo),
        JUL_CONFIG("CONFIG", LogCaptorAssert::hasNoConfigLogs, LogCaptorAssert::atConfig),
        JUL_FINE("FINE", LogCaptorAssert::hasNoFineLogs, LogCaptorAssert::atFine),
        JUL_FINER("FINER", LogCaptorAssert::hasNoFinerLogs, LogCaptorAssert::atFiner),
        JUL_FINEST("FINEST", LogCaptorAssert::hasNoFinestLogs, LogCaptorAssert::atFinest),
        ;

        public static final List<LevelWrapper> BASE_LOG_LEVELS = List.of(
            JUL_SEVERE, JUL_WARNING, JUL_INFO, JUL_CONFIG, JUL_FINE, JUL_FINER, JUL_FINEST
        );

//        public static final List<LevelWrapper> BASE_LOG_LEVELS = List.of(
//            LOG4J2_FATAL, LOG4J2_ERROR, LOG4J2_WARN, LOG4J2_INFO, LOG4J2_DEBUG, LOG4J2_TRACE,
//            CUSTOM_CONF, CUSTOM_FINER
//        );

        private final String levelName;
        private final UnaryOperator<LogCaptorAssert> hasNoLogsMethod;
        private final Function<LogCaptorAssert, LogEventsAssert> atLevelMethod;
    }
}