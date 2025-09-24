package nl.pim16aap2.testing.assertions;

import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.assertj.core.api.ListAssert;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import static nl.pim16aap2.testing.assertions.LogEventsAssert.assertThatLogEvents;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Timeout(1)
@ExtendWith(MockitoExtension.class)
class LogEventsAssertTest
{
    private static final Function<LogEventsAssert, List<LogEvent>> ACTUAL_EXTRACTOR = AbstractAssert::actual;
    private static final Function<LogEventAssert, LogEvent> LOG_EVENT_EXTRACTOR = LogEventAssert::actual;
    @SuppressWarnings("rawtypes")
    private static final InstanceOfAssertFactory<List, ListAssert<LogEvent>> INSTANCE_OF_LOG_EVENT_LIST =
        InstanceOfAssertFactories.list(LogEvent.class);

    @Mock
    private LogCaptor logCaptor;

    /**
     * Log event with message 'Message A', no throwable, and logger name 'LoggerA'.
     */
    private final LogEvent eventA = createLogEvent("Message A", null, "LoggerA");

    /**
     * Log event with message 'Message B', an IOException, and logger name 'LoggerB'.
     */
    private final LogEvent eventB = createLogEvent("Message B", new IOException("B"), "LoggerB");

    /**
     * Log event with message 'Another Message C', an UncheckedIOException, and logger name 'LoggerC'.
     */
    private final LogEvent eventC = createLogEvent("Another Message C",
        new UncheckedIOException(new IOException("C")),
        "LoggerC");

    /**
     * List of all default log events used in tests.
     */
    private final List<LogEvent> allEvents = List.of(eventA, eventB, eventC);

    @Test
    void failWithMessage_shouldAppendFormattedLogEvents()
    {
        // setup
        final LogEvent testEvent = createLogEvent("My Test Message", null, "TestLogger");
        when(logCaptor.getLogEvents()).thenReturn(List.of(testEvent));
        final LogEventsAssert logEventsAssert = assertThatLogEvents(List.of(), logCaptor);

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
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventsAssert messageFiltered = logEventsAssert.filteredByMessage(matcher, "Message");
        final LogEventsAssert anotherFiltered = logEventsAssert.filteredByMessage(matcher, "Another");

        // verify
        assertThat(messageFiltered)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(2)
            .containsExactly(eventA, eventB);
        assertThat(anotherFiltered)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(1)
            .containsExactly(eventC);
    }


    @Test
    void hasAnyWithMessage_shouldSucceedWithMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventsAssert result = logEventsAssert.hasAnyWithMessage(String::contains, "Message");

        // verify
        assertThat(result)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(3);
    }

    @Test
    void hasAnyWithMessage_shouldFailWhenNoMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.hasAnyWithMessage(String::contains, "NonExistent"))
            .withMessageContaining(
                "Expected at least one log event with a message matching 'NonExistent' but found none"
            );
    }


    @Test
    void singleWithMessage_shouldReturnSingleMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventAssert result = logEventsAssert.singleWithMessage(String::startsWith, "Another");

        // verify
        assertThat(result)
            .extracting(LOG_EVENT_EXTRACTOR)
            .isSameAs(eventC);
    }

    @Test
    void singleWithMessage_shouldFailWhenNoMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.singleWithMessage(String::contains, "NonExistent"))
            .withMessageContaining("Expected exactly one log event with a message matching 'NonExistent' but found 0");
    }

    @Test
    void singleWithMessage_shouldFailWhenMultipleMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.singleWithMessage(String::contains, "Message"))
            .withMessageContaining("Expected exactly one log event with a message matching 'Message' but found 3");
    }


    @Test
    void hasNoneWithMessage_shouldSucceedWhenNoMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify - should not throw
        logEventsAssert.hasNoneWithMessage(String::contains, "NonExistent");
    }

    @Test
    void hasNoneWithMessage_shouldFailWhenMatchesExist()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.hasNoneWithMessage(String::contains, "Message"))
            .withMessageContaining("Expected no log events with a message matching 'Message' but found 3");
    }


    @Test
    void filteredByMessagesContaining_shouldFilterForMatchingMessages()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventsAssert messageFiltered = logEventsAssert.filteredByMessagesContaining("Message");
        final LogEventsAssert aFiltered = logEventsAssert.filteredByMessagesContaining("A");

        // verify
        assertThat(messageFiltered)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(3);
        assertThat(aFiltered)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(2)
            .containsExactlyInAnyOrder(eventA, eventC);
    }


    @Test
    void hasAnyWithMessageContaining_shouldSucceedWithMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventsAssert result = logEventsAssert.hasAnyWithMessageContaining("Message");

        // verify
        assertThat(result)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(3);
    }

    @Test
    void hasAnyWithMessageContaining_shouldFailWhenNoMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.hasAnyWithMessageContaining("NonExistent"))
            .withMessageContaining(
                "Expected at least one log event with a message containing 'NonExistent' but found none"
            );
    }


    @Test
    void singleMessageContaining_shouldReturnSingleWithMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventAssert result = logEventsAssert.singleWithMessageContaining("Another");

        // verify
        assertThat(result)
            .extracting(LOG_EVENT_EXTRACTOR)
            .isSameAs(eventC);
    }

    @Test
    void singleWithMessageContaining_shouldFailWhenNoMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.singleWithMessageContaining("NonExistent"))
            .withMessageContaining(
                "Expected exactly one log event with a message containing 'NonExistent' but found 0"
            );
    }

    @Test
    void singleWithMessageContaining_shouldFailWhenMultipleMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.singleWithMessageContaining("Message"))
            .withMessageContaining("Expected exactly one log event with a message containing 'Message' but found 3");
    }


    @Test
    void hasNoneWithMessageContaining_shouldSucceedWhenNoMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify - should not throw
        logEventsAssert.hasNoneWithMessageContaining("NonExistent");
    }

    @Test
    void hasNoneWithMessageContaining_shouldFailWhenMatchesExist()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.hasNoneWithMessageContaining("Message"))
            .withMessageContaining("Expected no log events with a message containing 'Message' but found 3");
    }


    @Test
    void filteredByMessagesExactly_shouldFilterByExactMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventsAssert exactMatch = logEventsAssert.filteredByMessagesExactly("Message A");

        // verify
        assertThat(exactMatch)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(1)
            .containsExactly(eventA);
    }

    @Test
    void filteredByMessagesExactly_shouldReturnEmptyWhenNoMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventsAssert noMatch = logEventsAssert.filteredByMessagesExactly("NonExistent");

        // verify
        assertThat(noMatch)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .isEmpty();
    }


    @Test
    void hasAnyWithMessageExactly_shouldSucceedWithMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventsAssert result = logEventsAssert.hasAnyWithMessageExactly("Message A");

        // verify
        assertThat(result)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(1)
            .containsExactly(eventA);
    }

    @Test
    void hasAnyWithMessageExactly_shouldFailWhenNoMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.hasAnyWithMessageExactly("NonExistent"))
            .withMessageContaining("Expected at least one log event with exact message 'NonExistent' but found none");
    }


    @Test
    void singleMessageExactly_shouldReturnSingleWithMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventAssert result = logEventsAssert.singleWithMessageExactly("Message A");

        // verify
        assertThat(result)
            .extracting(LOG_EVENT_EXTRACTOR)
            .isSameAs(eventA);
    }

    @Test
    void singleWithMessageExactly_shouldFailWhenNoMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.singleWithMessageExactly("NonExistent"))
            .withMessageContaining("Expected exactly one log event with exact message 'NonExistent' but found 0");
    }

    @Test
    void singleWithMessageExactly_shouldFailWhenMultipleMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(List.of(eventA, eventA), logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.singleWithMessageExactly("Message A"))
            .withMessageContaining("Expected exactly one log event with exact message 'Message A' but found 2");
    }


    @Test
    void hasNoneWithMessageExactly_shouldSucceedWhenNoMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify - should not throw
        logEventsAssert.hasNoneWithMessageExactly("NonExistent");
    }

    @Test
    void hasNoneWithMessageExactly_shouldFailWhenMatchesExist()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.hasNoneWithMessageExactly("Message A"))
            .withMessageContaining("Expected no log events with exact message 'Message A' but found 1");
    }


    @Test
    void filteredByMessagesStartingWith_shouldFilterCorrectly()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventsAssert messageStarting = logEventsAssert.filteredByMessagesStartingWith("Message");
        final LogEventsAssert anotherStarting = logEventsAssert.filteredByMessagesStartingWith("Another");

        // verify
        assertThat(messageStarting)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(2)
            .containsExactly(eventA, eventB);
        assertThat(anotherStarting)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(1)
            .containsExactly(eventC);
    }


    @Test
    void hasAnyWithMessageStartingWith_shouldSucceedWithMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventsAssert result = logEventsAssert.hasAnyWithMessageStartingWith("Message");

        // verify
        assertThat(result)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(2);
    }

    @Test
    void hasAnyWithMessageStartingWith_shouldFailWhenNoMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.hasAnyWithMessageStartingWith("NonExistent"))
            .withMessageContaining(
                "Expected at least one log event with a message starting with 'NonExistent' but found none"
            );
    }


    @Test
    void singleMessageStartingWith_shouldReturnSingleWithMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventAssert result = logEventsAssert.singleWithMessageStartingWith("Another");

        // verify
        assertThat(result)
            .extracting(LOG_EVENT_EXTRACTOR)
            .isSameAs(eventC);
    }

    @Test
    void singleWithMessageStartingWith_shouldFailWhenNoMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.singleWithMessageStartingWith("NonExistent"))
            .withMessageContaining(
                "Expected exactly one log event with a message starting with 'NonExistent' but found 0"
            );
    }

    @Test
    void singleWithMessageStartingWith_shouldFailWhenMultipleMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.singleWithMessageStartingWith("Message"))
            .withMessageContaining("Expected exactly one log event with a message starting with 'Message' but found 2");
    }


    @Test
    void hasNoneWithMessageStartingWith_shouldSucceedWhenNoMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify - should not throw
        logEventsAssert.hasNoneWithMessageStartingWith("NonExistent");
    }

    @Test
    void hasNoneWithMessageStartingWith_shouldFailWhenMatchesExist()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.hasNoneWithMessageStartingWith("Message"))
            .withMessageContaining("Expected no log events with a message starting with 'Message' but found 2");
    }


    @Test
    void filteredByMessagesEndingWith_shouldFilterCorrectly()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventsAssert endingWithA = logEventsAssert.filteredByMessagesEndingWith("A");
        final LogEventsAssert endingWithC = logEventsAssert.filteredByMessagesEndingWith("C");

        // verify
        assertThat(endingWithA)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(1)
            .containsExactly(eventA);
        assertThat(endingWithC)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(1)
            .containsExactly(eventC);
    }


    @Test
    void hasAnyWithMessageEndingWith_shouldSucceedWithMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventsAssert result = logEventsAssert.hasAnyWithMessageEndingWith("A");

        // verify
        assertThat(result)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(1);
    }

    @Test
    void hasAnyWithMessageEndingWith_shouldFailWhenNoMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.hasAnyWithMessageEndingWith("NonExistent"))
            .withMessageContaining(
                "Expected at least one log event with a message ending with 'NonExistent' but found none"
            );
    }


    @Test
    void singleMessageEndingWith_shouldReturnSingleWithMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventAssert result = logEventsAssert.singleWithMessageEndingWith("A");

        // verify
        assertThat(result)
            .extracting(LOG_EVENT_EXTRACTOR)
            .isSameAs(eventA);
    }

    @Test
    void singleWithMessageEndingWith_shouldFailWhenNoMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.singleWithMessageEndingWith("NonExistent"))
            .withMessageContaining(
                "Expected exactly one log event with a message ending with 'NonExistent' but found 0"
            );
    }

    @Test
    void singleWithMessageEndingWith_shouldFailWhenMultipleMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(List.of(eventA, eventA), logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.singleWithMessageEndingWith("A"))
            .withMessageContaining("Expected exactly one log event with a message ending with 'A' but found 2");
    }


    @Test
    void hasNoneWithMessageEndingWith_shouldSucceedWhenNoMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify - should not throw
        logEventsAssert.hasNoneWithMessageEndingWith("NonExistent");
    }

    @Test
    void hasNoneWithMessageEndingWith_shouldFailWhenMatchesExist()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.hasNoneWithMessageEndingWith("A"))
            .withMessageContaining("Expected no log events with a message ending with 'A' but found 1");
    }


    @Test
    void filteredByMessagesMatching_shouldFilterBySimpleRegex()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventsAssert messagePattern = logEventsAssert.filteredByMessagesMatching("Message [A-Z]");
        final LogEventsAssert endingWithC = logEventsAssert.filteredByMessagesMatching(".* C");

        // verify
        assertThat(messagePattern)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(2)
            .containsExactly(eventA, eventB);
        assertThat(endingWithC)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(1)
            .containsExactly(eventC);
    }

    @Test
    void filteredByMessagesMatching_shouldFilterByComplexRegex()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventsAssert complex = logEventsAssert.filteredByMessagesMatching("^Message\\s[AB]$");

        // verify
        assertThat(complex)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(2)
            .containsExactlyInAnyOrder(eventA, eventB);
    }


    @Test
    void hasAnyWithMessageMatching_shouldSucceedWithMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventsAssert result = logEventsAssert.hasAnyWithMessageMatching("Message.*");

        // verify
        assertThat(result)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(2);
    }

    @Test
    void hasAnyWithMessageMatching_shouldFailWhenNoMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.hasAnyWithMessageMatching("NonExistent.*"))
            .withMessageContaining(
                "Expected at least one log event with a message matching 'NonExistent.*' but found none"
            );
    }


    @Test
    void singleMessageMatching_shouldReturnSingleWithMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventAssert result = logEventsAssert.singleWithMessageMatching("Another.*");

        // verify
        assertThat(result)
            .extracting(LOG_EVENT_EXTRACTOR)
            .isSameAs(eventC);
    }

    @Test
    void singleWithMessageMatching_shouldFailWhenNoMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.singleWithMessageMatching("NonExistent.*"))
            .withMessageContaining(
                "Expected exactly one log event with a message matching 'NonExistent.*' but found 0"
            );
    }

    @Test
    void singleWithMessageMatching_shouldFailWhenMultipleMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.singleWithMessageMatching("Message.*"))
            .withMessageContaining("Expected exactly one log event with a message matching 'Message.*' but found 2");
    }


    @Test
    void hasNoneWithMessageMatching_shouldSucceedWhenNoMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify - should not throw
        logEventsAssert.hasNoneWithMessageMatching("NonExistent.*");
    }

    @Test
    void hasNoneWithMessageMatching_shouldFailWhenMatchesExist()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.hasNoneWithMessageMatching("Message.*"))
            .withMessageContaining("Expected no log events with a message matching 'Message.*' but found 2");
    }


    @Test
    void filteredBy_shouldFilterByCustomPredicate()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);
        final Predicate<LogEvent> hasIoException =
            event -> event.getThrowable().map(IOException.class::isInstance).orElse(false);

        // execute
        final LogEventsAssert result = logEventsAssert.filteredBy(hasIoException);

        // verify
        assertThat(result)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(1)
            .containsExactly(eventB);
    }


    @Test
    void hasAnyFilteredBy_shouldSucceedWithMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);
        final Predicate<LogEvent> hasMessageA = event -> "Message A".equals(event.getMessage());

        // execute
        final LogEventsAssert result = logEventsAssert.hasAnyFilteredBy(hasMessageA);

        // verify
        assertThat(result)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(1)
            .containsExactly(eventA);
    }

    @Test
    void hasAnyFilteredBy_shouldFailWhenNoMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);
        final Predicate<LogEvent> noMatchPredicate = event -> "NonExistent".equals(event.getMessage());

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.hasAnyFilteredBy(noMatchPredicate))
            .withMessageContaining("Expected at least one log event matching the predicate but found none");
    }


    @Test
    void singleFilteredBy_shouldReturnSingleMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);
        final Predicate<LogEvent> hasMessageA = event -> "Message A".equals(event.getMessage());

        // execute
        final LogEventAssert result = logEventsAssert.singleFilteredBy(hasMessageA);

        // verify
        assertThat(result)
            .extracting(LOG_EVENT_EXTRACTOR)
            .isSameAs(eventA);
    }

    @Test
    void singleFilteredBy_shouldFailWhenNoMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);
        final Predicate<LogEvent> noMatchPredicate = event -> "NonExistent".equals(event.getMessage());

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.singleFilteredBy(noMatchPredicate))
            .withMessageContaining("Expected exactly one log event matching the predicate but found 0");
    }

    @Test
    void singleFilteredBy_shouldFailWhenMultipleMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(List.of(eventA, eventA), logCaptor);
        final Predicate<LogEvent> hasMessageA = event -> "Message A".equals(event.getMessage());

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.singleFilteredBy(hasMessageA))
            .withMessageContaining("Expected exactly one log event matching the predicate but found 2");
    }


    @Test
    void filteredByThrowableOfType_shouldFilterByInstanceOfCheck()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventsAssert ioException =
            logEventsAssert.filteredByThrowableOfType(IOException.class);

        final LogEventsAssert uncheckedIoException =
            logEventsAssert.filteredByThrowableOfType(UncheckedIOException.class);

        // verify
        assertThat(ioException)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(1)
            .containsExactly(eventB);

        assertThat(uncheckedIoException)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(1)
            .containsExactly(eventC);
    }

    @Test
    void filteredByThrowableOfType_shouldFilterByInheritance()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventsAssert runtimeException = logEventsAssert.filteredByThrowableOfType(RuntimeException.class);

        // verify
        assertThat(runtimeException)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(1)
            .containsExactly(eventC);
    }


    @Test
    void hasAnyWithThrowableOfType_shouldSucceedWithMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventsAssert result = logEventsAssert.hasAnyWithThrowableOfType(Exception.class);

        // verify
        assertThat(result)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(2);
    }

    @Test
    void hasAnyWithThrowableOfType_shouldFailWhenNoMatches()
    {
        // setup
        final var throwableType = ClassNotFoundException.class;
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.hasAnyWithThrowableOfType(throwableType))
            .withMessageContaining(
                "Expected at least one log event with a throwable of type %s but found none",
                throwableType.getName()
            );
    }


    @Test
    void singleThrowableOfType_shouldReturnSingleWithMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventAssert result = logEventsAssert.singleWithThrowableOfType(UncheckedIOException.class);

        // verify
        assertThat(result)
            .extracting(LOG_EVENT_EXTRACTOR)
            .isSameAs(eventC);
    }

    @Test
    void singleWithThrowableOfType_shouldFailWhenNoMatches()
    {
        // setup
        final var throwableType = IOException.class;
        final LogEventsAssert logEventsAssert = assertThatLogEvents(List.of(eventA), logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.singleWithThrowableOfType(throwableType))
            .withMessageContaining(
                "Expected exactly one log event with a throwable of type %s but found 0",
                throwableType.getName()
            );
    }

    @Test
    void singleWithThrowableOfType_shouldFailWhenMultipleMatches()
    {
        // setup
        final var throwableType = Exception.class;
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.singleWithThrowableOfType(throwableType))
            .withMessageContaining(
                "Expected exactly one log event with a throwable of type %s but found 2",
                throwableType.getName()
            );
    }


    @Test
    void filteredByThrowableExactlyOfType_shouldFilterCorrectly()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventsAssert ioException = logEventsAssert.filteredByThrowableExactlyOfType(IOException.class);
        final LogEventsAssert uncheckedIoException =
            logEventsAssert.filteredByThrowableExactlyOfType(UncheckedIOException.class);

        // verify
        assertThat(ioException)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(1)
            .containsExactly(eventB);
        assertThat(uncheckedIoException)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(1)
            .containsExactly(eventC);
    }


    @Test
    void hasAnyWithThrowableExactlyOfType_shouldSucceedWithMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventsAssert result = logEventsAssert.hasAnyWithThrowableExactlyOfType(IOException.class);

        // verify
        assertThat(result)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(1);
    }

    @Test
    void hasAnyWithThrowableExactlyOfType_shouldFailWhenNoMatches()
    {
        // setup
        final Class<? extends Throwable> nonExistentThrowable = ClassNotFoundException.class;
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.hasAnyWithThrowableExactlyOfType(nonExistentThrowable))
            .withMessageContaining(
                "Expected at least one log event with a throwable exactly of type %s but found none",
                nonExistentThrowable.getName()
            );
    }

    @Test
    void singleThrowableExactlyOfType_shouldReturnSingleWithMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventAssert result = logEventsAssert.singleWithThrowableExactlyOfType(IOException.class);

        // verify
        assertThat(result)
            .extracting(LOG_EVENT_EXTRACTOR)
            .isSameAs(eventB);
    }

    @Test
    void singleWithThrowableExactlyOfType_shouldFailWhenNoMatches()
    {
        // setup
        final var throwableType = IOException.class;
        final LogEventsAssert logEventsAssert = assertThatLogEvents(List.of(eventA), logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.singleWithThrowableExactlyOfType(throwableType))
            .withMessageContaining(
                "Expected exactly one log event with a throwable exactly of type %s but found 0",
                throwableType.getName()
            );
    }

    @Test
    void singleWithThrowableExactlyOfType_shouldFailWhenMultipleMatches()
    {
        // setup
        final var throwableType = IOException.class;
        final LogEventsAssert logEventsAssert = assertThatLogEvents(List.of(eventB, eventB), logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> logEventsAssert.singleWithThrowableExactlyOfType(throwableType))
            .withMessageContaining(
                "Expected exactly one log event with a throwable exactly of type %s but found 2",
                throwableType.getName()
            );
    }


    @Test
    void filteredByHasThrowable_shouldReturnEventsWithThrowables()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventsAssert result = logEventsAssert.filteredByHasThrowable();

        // verify
        assertThat(result)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(2)
            .containsExactlyInAnyOrder(eventB, eventC);
    }


    @Test
    void anyWithThrowable_shouldSucceedWithMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventsAssert result = logEventsAssert.anyWithThrowable();

        // verify
        assertThat(result)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(2);
    }

    @Test
    void anyWithThrowable_shouldFailWhenNoMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(List.of(eventA), logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(logEventsAssert::anyWithThrowable)
            .withMessageContaining("Expected at least one log event with a throwable but found none");
    }


    @Test
    void singleWithThrowable_shouldReturnSingleMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(List.of(eventA, eventB), logCaptor);

        // execute
        final LogEventAssert result = logEventsAssert.singleWithThrowable();

        // verify
        assertThat(result)
            .extracting(LOG_EVENT_EXTRACTOR)
            .isSameAs(eventB);
    }

    @Test
    void singleWithThrowable_shouldFailWhenNoMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(List.of(eventA), logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(logEventsAssert::singleWithThrowable)
            .withMessageContaining("Expected exactly one log event with a throwable but found 0");
    }

    @Test
    void singleWithThrowable_shouldFailWhenMultipleMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(List.of(eventB, eventC), logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(logEventsAssert::singleWithThrowable)
            .withMessageContaining("Expected exactly one log event with a throwable but found 2");
    }


    @Test
    void filteredByWithoutThrowable_shouldReturnEventsWithoutThrowables()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventsAssert result = logEventsAssert.filteredByWithoutThrowable();

        // verify
        assertThat(result)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(1)
            .containsExactly(eventA);
    }


    @Test
    void anyWithoutThrowable_shouldSucceedWithMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventsAssert result = logEventsAssert.anyWithoutThrowable();

        // verify
        assertThat(result)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(1);
    }

    @Test
    void anyWithoutThrowable_shouldFailWhenNoMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(List.of(eventB, eventC), logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(logEventsAssert::anyWithoutThrowable)
            .withMessageContaining("Expected at least one log event without a throwable but found none");
    }


    @Test
    void singleWithoutThrowable_shouldReturnSingleMatch()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventAssert result = logEventsAssert.singleWithoutThrowable();

        // verify
        assertThat(result)
            .extracting(LOG_EVENT_EXTRACTOR)
            .isSameAs(eventA);
    }

    @Test
    void singleWithoutThrowable_shouldFailWhenNoMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(List.of(eventB, eventC), logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(logEventsAssert::singleWithoutThrowable)
            .withMessageContaining("Expected exactly one log event without a throwable but found 0");
    }

    @Test
    void singleWithoutThrowable_shouldFailWhenMultipleMatches()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(List.of(eventA, eventA), logCaptor);

        // execute & verify
        assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(logEventsAssert::singleWithoutThrowable)
            .withMessageContaining("Expected exactly one log event without a throwable but found 2");
    }


    @Test
    void filteredByLoggerName_shouldFilterByExistingLoggerNames()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventsAssert loggerA = logEventsAssert.filteredByLoggerName("LoggerA");
        final LogEventsAssert loggerB = logEventsAssert.filteredByLoggerName("LoggerB");

        // verify
        assertThat(loggerA)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(1)
            .containsExactly(eventA);
        assertThat(loggerB)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(1)
            .containsExactly(eventB);
    }

    @Test
    void filteredByLoggerName_shouldReturnEmptyForNonExistentLogger()
    {
        // setup
        final LogEventsAssert logEventsAssert = assertThatLogEvents(allEvents, logCaptor);

        // execute
        final LogEventsAssert nonExistent = logEventsAssert.filteredByLoggerName("NonExistent");

        // verify
        assertThat(nonExistent)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .isEmpty();
    }

    @Test
    void filteredByLoggerName_shouldHandleEmptyLoggerName()
    {
        // setup
        final LogEvent emptyLoggerEvent = createLogEvent("Test", null, "");
        final LogEventsAssert emptyLoggerAssert = assertThatLogEvents(List.of(emptyLoggerEvent), logCaptor);

        // execute
        final LogEventsAssert emptyLogger = emptyLoggerAssert.filteredByLoggerName("");

        // verify
        assertThat(emptyLogger)
            .extracting(ACTUAL_EXTRACTOR, INSTANCE_OF_LOG_EVENT_LIST)
            .hasSize(1)
            .containsExactly(emptyLoggerEvent);
    }

    private LogEvent createLogEvent(String message, @Nullable Throwable throwable, String loggerName)
    {
        return new LogEvent(
            message,
            message,
            "INFO",
            loggerName,
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
