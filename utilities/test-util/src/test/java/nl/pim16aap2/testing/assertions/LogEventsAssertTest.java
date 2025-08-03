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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static nl.pim16aap2.testing.assertions.LogCaptorAssert.assertThatLogCaptor;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class LogEventsAssertTest
{
    @Mock
    private LogCaptor logCaptor;

    private final LogEvent eventA = createMockLogEvent("Message A", null, "LoggerA");
    private final LogEvent eventB = createMockLogEvent("Message B", new IOException("B"), "LoggerB");
    private final LogEvent eventC = createMockLogEvent("Another Message C",
        new UncheckedIOException(new IOException("C")),
        "LoggerC");

    private final List<LogEvent> allEvents = List.of(eventA, eventB, eventC);

    @BeforeEach
    void setUp()
    {
        lenient().when(logCaptor.getLogEvents()).thenReturn(allEvents);
    }

    @Test
    void failWithMessage_shouldAppendFormattedLogEvents()
    {
        // setup
        final LogEvent testEvent = createMockLogEvent("My Test Message", null, "TestLogger");
        when(logCaptor.getLogEvents()).thenReturn(List.of(testEvent));
        final LogEventsAssert logEventsAssert = new LogEventsAssert(List.of(), logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.failWithMessage("Custom error"))
            .withMessageContaining("Custom error")
            .withMessageContaining("[INFO] `My Test Message` from Logger TestLogger");
    }

    @Test
    void filteredByMessage_shouldFilterByCustomPredicate()
    {
        // setup
        final BiPredicate<String, String> matcher = String::startsWith;
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.filteredByMessage(matcher, "Message").hasSize(2)
            .extracting(LogEvent::getMessage).containsExactly("Message A", "Message B");
        logEventsAssert.filteredByMessage(matcher, "Another").hasSize(1)
            .extracting(LogEvent::getMessage).containsExactly("Another Message C");
    }

    @Test
    void hasAnyWithMessage_shouldVerifyAtLeastOneMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.hasAnyWithMessage(String::contains, "Message").hasSize(3);
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.hasAnyWithMessage(String::contains, "NonExistent"));
    }

    @Test
    void singleWithMessage_shouldVerifyExactlyOneMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.singleWithMessage(String::startsWith, "Another")
            .satisfies(event -> assertThat(event.getMessage()).isEqualTo("Another Message C"));
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.singleWithMessage(String::contains, "Message"));
    }

    @Test
    void filteredByMessagesContaining_shouldFilterCorrectly()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.filteredByMessagesContaining("Message").hasSize(3);
        logEventsAssert.filteredByMessagesContaining("A").hasSize(2)
            .extracting(LogEvent::getMessage).containsExactlyInAnyOrder("Message A", "Another Message C");
    }

    @Test
    void filteredByExactMessages_shouldFilterCorrectly()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.filteredByExactMessages("Message A").singleElement()
            .satisfies(event -> assertThat(event.getMessage()).isEqualTo("Message A"));
        logEventsAssert.filteredByExactMessages("NonExistent").isEmpty();
    }

    @Test
    void filteredByMessagesStartingWith_shouldFilterCorrectly()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.filteredByMessagesStartingWith("Message").hasSize(2);
        logEventsAssert.filteredByMessagesStartingWith("Another").hasSize(1);
    }

    @Test
    void filteredByMessagesEndingWith_shouldFilterCorrectly()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.filteredByMessagesEndingWith("A").singleElement()
            .satisfies(event -> assertThat(event.getMessage()).isEqualTo("Message A"));
        logEventsAssert.filteredByMessagesEndingWith("C").singleElement()
            .satisfies(event -> assertThat(event.getMessage()).isEqualTo("Another Message C"));
    }

    @Test
    void filteredByMessagesMatching_shouldFilterByRegex()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.filteredByMessagesMatching("Message [A-Z]").hasSize(2);
        logEventsAssert.filteredByMessagesMatching(".* C").hasSize(1);
    }

    @Test
    void filteredByThrowableOfType_shouldFilterCorrectly()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.filteredByThrowableOfType(IOException.class).hasSize(1)
            .extracting(LogEvent::getMessage).containsExactly("Message B");
        logEventsAssert.filteredByThrowableOfType(UncheckedIOException.class).hasSize(1)
            .extracting(LogEvent::getMessage).containsExactly("Another Message C");
        logEventsAssert.filteredByThrowableOfType(RuntimeException.class).hasSize(1)
            .extracting(LogEvent::getMessage).containsExactly("Another Message C");
    }

    @Test
    void filteredByThrowableExactlyOfType_shouldFilterCorrectly()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.filteredByThrowableExactlyOfType(IOException.class).hasSize(1)
            .extracting(LogEvent::getMessage).containsExactly("Message B");
        logEventsAssert.filteredByThrowableExactlyOfType(UncheckedIOException.class).hasSize(1)
            .extracting(LogEvent::getMessage).containsExactly("Another Message C");
    }

    @Test
    void filteredByHasThrowable_shouldReturnEventsWithThrowables()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.filteredByHasThrowable().hasSize(2)
            .extracting(LogEvent::getMessage).containsExactlyInAnyOrder("Message B", "Another Message C");
    }

    @Test
    void filteredByWithoutThrowable_shouldReturnEventsWithoutThrowables()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.filteredByWithoutThrowable().hasSize(1)
            .extracting(LogEvent::getMessage).containsExactly("Message A");
    }

    @Test
    void filteredBy_shouldFilterByCustomPredicate()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();
        final Predicate<LogEvent> hasIoException =
            event -> event.getThrowable().map(IOException.class::isInstance).orElse(false);

        // execute & verify
        logEventsAssert.filteredBy(hasIoException).hasSize(1)
            .extracting(LogEvent::getMessage).containsExactly("Message B");
    }

    @Test
    void filteredByLoggerName_shouldFilterCorrectly()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.filteredByLoggerName("LoggerA").singleElement()
            .satisfies(event -> assertThat(event.getLoggerName()).isEqualTo("LoggerA"));

        logEventsAssert.filteredByLoggerName("LoggerB").singleElement()
            .satisfies(event -> assertThat(event.getLoggerName()).isEqualTo("LoggerB"));

        logEventsAssert.filteredByLoggerName("NonExistent").isEmpty();
    }

    @Test
    void assertThatLogEvents_shouldReturnLogEventsAssert()
    {
        // setup
        final List<LogEvent> testEvents = List.of(eventA);

        // execute
        final LogEventsAssert result = LogEventsAssert.assertThatLogEvents(testEvents, logCaptor);

        // verify
        assertThat(result).extracting("actual").isSameAs(testEvents);
    }

    @Test
    void filteredByMessagesContaining_shouldHandleEmptyString()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.filteredByMessagesContaining("").hasSize(3);
    }

    @Test
    void filteredByMessagesMatching_shouldHandleComplexRegex()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.filteredByMessagesMatching("^Message\\s[AB]$").hasSize(2)
            .extracting(LogEvent::getMessage).containsExactlyInAnyOrder("Message A", "Message B");
        logEventsAssert.filteredByMessagesMatching(".*\\sC$").hasSize(1)
            .extracting(LogEvent::getMessage).containsExactly("Another Message C");
    }

    @Test
    void hasAnyMessageContaining_shouldVerifyAtLeastOneMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.hasAnyMessageContaining("Message");
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.hasAnyMessageContaining("NonExistent"));
    }

    @Test
    void singleMessageContaining_shouldVerifyExactlyOneMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.singleMessageContaining("Another")
            .satisfies(event -> assertThat(event.getMessage()).contains("Another"));
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.singleMessageContaining("Message"));
    }

    @Test
    void hasAnyExactMessage_shouldVerifyAtLeastOneMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.hasAnyExactMessage("Message A");
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.hasAnyExactMessage("NonExistent"));
    }

    @Test
    void singleExactMessage_shouldVerifyExactlyOneMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.singleExactMessage("Message A")
            .satisfies(event -> assertThat(event.getMessage()).isEqualTo("Message A"));
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.singleExactMessage("Message"));
    }

    @Test
    void hasAnyMessageStartingWith_shouldVerifyAtLeastOneMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.hasAnyMessageStartingWith("Message");
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.hasAnyMessageStartingWith("NonExistent"));
    }

    @Test
    void singleMessageStartingWith_shouldVerifyExactlyOneMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.singleMessageStartingWith("Another")
            .satisfies(event -> assertThat(event.getMessage()).startsWith("Another"));
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.singleMessageStartingWith("Message"));
    }

    @Test
    void hasAnyMessageEndingWith_shouldVerifyAtLeastOneMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.hasAnyMessageEndingWith("A");
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.hasAnyMessageEndingWith("NonExistent"));
    }

    @Test
    void singleMessageEndingWith_shouldVerifyExactlyOneMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.singleMessageEndingWith("A")
            .satisfies(event -> assertThat(event.getMessage()).endsWith("A"));
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.singleMessageEndingWith("Message"));
    }

    @Test
    void hasAnyMessageMatching_shouldVerifyAtLeastOneMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.hasAnyMessageMatching("Message.*");
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.hasAnyMessageMatching("NonExistent.*"));
    }

    @Test
    void singleMessageMatching_shouldVerifyExactlyOneMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.singleMessageMatching("Another.*")
            .satisfies(event -> assertThat(event.getMessage()).matches("Another.*"));
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.singleMessageMatching("Message.*"));
    }

    @Test
    void hasAnyThrowableOfType_shouldVerifyAtLeastOneMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.hasAnyThrowableOfType(Exception.class);
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.hasAnyThrowableOfType(ClassNotFoundException.class));
    }

    @Test
    void singleThrowableOfType_shouldVerifyExactlyOneMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.singleThrowableOfType(UncheckedIOException.class)
            .satisfies(event -> assertThat(event.getThrowable()).isPresent());
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.singleThrowableOfType(Exception.class));
    }

    @Test
    void hasAnyThrowableExactlyOfType_shouldVerifyAtLeastOneMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.hasAnyThrowableExactlyOfType(IOException.class);
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.hasAnyThrowableExactlyOfType(ClassNotFoundException.class));
    }

    @Test
    void singleThrowableExactlyOfType_shouldVerifyExactlyOneMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.singleThrowableExactlyOfType(IOException.class)
            .satisfies(event -> assertThat(event.getThrowable()).isPresent());
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.singleThrowableExactlyOfType(Exception.class));
    }

    @Test
    void anyWithThrowable_shouldVerifyAtLeastOneMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.anyWithThrowable();

        // Test with empty throwables
        when(logCaptor.getLogEvents()).thenReturn(List.of(eventA));
        final LogEventsAssert noThrowableAssert = assertThatLogCaptor(logCaptor).atAllLevels();
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(noThrowableAssert::anyWithThrowable);
    }

    @Test
    void singleWithThrowable_shouldVerifyExactlyOneMatch()
    {
        // setup
        when(logCaptor.getLogEvents()).thenReturn(List.of(eventB));
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.singleWithThrowable()
            .satisfies(event -> assertThat(event.getThrowable()).isPresent());

        when(logCaptor.getLogEvents()).thenReturn(allEvents);
        final LogEventsAssert multipleThrowableAssert = assertThatLogCaptor(logCaptor).atAllLevels();
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(multipleThrowableAssert::singleWithThrowable);
    }

    @Test
    void anyWithoutThrowable_shouldVerifyAtLeastOneMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.anyWithoutThrowable();

        // Test with all events having throwables
        when(logCaptor.getLogEvents()).thenReturn(List.of(eventB, eventC));
        final LogEventsAssert allThrowableAssert = assertThatLogCaptor(logCaptor).atAllLevels();
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(allThrowableAssert::anyWithoutThrowable);
    }

    @Test
    void singleWithoutThrowable_shouldVerifyExactlyOneMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.singleWithoutThrowable()
            .satisfies(event -> assertThat(event.getThrowable()).isEmpty());

        when(logCaptor.getLogEvents()).thenReturn(List.of(eventA, eventA));
        final LogEventsAssert multipleNoThrowableAssert = assertThatLogCaptor(logCaptor).atAllLevels();
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(multipleNoThrowableAssert::singleWithoutThrowable);
    }

    @Test
    void hasAnyFilteredBy_shouldVerifyAtLeastOneMatchWithPredicate()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();
        final Predicate<LogEvent> hasMessageA = event -> "Message A".equals(event.getMessage());

        // execute & verify
        logEventsAssert.hasAnyFilteredBy(hasMessageA);
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.hasAnyFilteredBy(event -> false));
    }

    @Test
    void singleFilteredBy_shouldVerifyExactlyOneMatchWithPredicate()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();
        final Predicate<LogEvent> hasMessageA = event -> "Message A".equals(event.getMessage());

        // execute & verify
        logEventsAssert.singleFilteredBy(hasMessageA)
            .satisfies(event -> assertThat(event.getMessage()).isEqualTo("Message A"));
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.singleFilteredBy(event -> event.getMessage().startsWith("Message")));
    }

    @Test
    void filteredByLoggerName_shouldHandleEmptyLoggerName()
    {
        // setup
        final LogEvent emptyLoggerEvent = createMockLogEvent("Test", null, "");
        when(logCaptor.getLogEvents()).thenReturn(List.of(emptyLoggerEvent));
        final LogEventsAssert logEventsAssert = assertThatLogCaptor(logCaptor).atAllLevels();

        // execute & verify
        logEventsAssert.filteredByLoggerName("").hasSize(1);
    }

    private LogEvent createMockLogEvent(String message, @Nullable Throwable throwable, String loggerName)
    {
        final LogEvent mockEvent = mock(LogEvent.class);
        lenient().when(mockEvent.getLevel()).thenReturn("INFO");
        lenient().when(mockEvent.getMessage()).thenReturn(message);
        lenient().when(mockEvent.getFormattedMessage()).thenReturn("[INFO] " + message);
        lenient().when(mockEvent.getThrowable()).thenReturn(Optional.ofNullable(throwable));
        lenient().when(mockEvent.getLoggerName()).thenReturn(loggerName);
        return mockEvent;
    }
}