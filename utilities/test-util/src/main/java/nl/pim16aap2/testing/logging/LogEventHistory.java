package nl.pim16aap2.testing.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import lombok.Getter;
import lombok.ToString;
import org.checkerframework.dataflow.qual.Pure;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@NotThreadSafe //
final class LogEventHistory
{
    private final LogEvents history = new LogEvents(Logger.ROOT_LOGGER_NAME);
    private final HashMap<String, LogEvents> historyPerClass = new HashMap<>();

    @Pure
    public List<ILoggingEvent> getSelection(@Nullable Class<?> source, boolean onlyThrowing,
                                            Level level, boolean includeHigherLevels)
    {
        final LogEvents selected =
            source == null ? history : historyPerClass.getOrDefault(getNameForSourceClass(source), LogEvents.EMPTY);

        return selected.getSelection(onlyThrowing, level, includeHigherLevels);
    }

    @Pure
    public int getSize(@Nullable Class<?> source, boolean onlyThrowing, Level level, boolean includeHigherLevels)
    {
        return findLogEvents(source).size(onlyThrowing, level, includeHigherLevels);
    }

    @Pure
    public Optional<Throwable> getLastThrowable(@Nullable Class<?> source, Level level, boolean includeHigherLevels)
    {
        return findLogEvents(source).getLastThrowable(level, includeHigherLevels);
    }

    /**
     * Tries to find the {@link LogEvents}.
     * <p>
     * If none could be found, {@link LogEvents#EMPTY} is return; this method never creates a new object.
     *
     * @param source
     *     The source class to find. If this is null, it'll look at {@link #history}.
     * @return The LogEvents instances for the provided class.
     */
    @Pure
    private LogEvents findLogEvents(@Nullable Class<?> source)
    {
        return source == null ? history : historyPerClass.getOrDefault(getNameForSourceClass(source), LogEvents.EMPTY);
    }

    public void add(ILoggingEvent loggingEvent)
    {
        history.add(loggingEvent);
        historyPerClass.computeIfAbsent(loggingEvent.getLoggerName(), LogEvents::new).add(loggingEvent);
    }

    public void clear()
    {
        history.clear();
        historyPerClass.values().forEach(LogEvents::clear);
        historyPerClass.clear();
    }

    @Pure
    private static String getNameForSourceClass(@Nullable Class<?> source)
    {
        return source == null ? Logger.ROOT_LOGGER_NAME : source.getName();
    }

    @NotThreadSafe
    @ToString
    private static final class LogEvents
    {
        public static final LogEvents EMPTY = new LogEvents("EMPTY", Collections.emptyList(), Collections.emptyList());

        @Getter
        private final String sourceName;
        private final List<ILoggingEvent> events;
        private final List<ILoggingEvent> throwingEvents;

        public LogEvents(String sourceName)
        {
            this.sourceName = sourceName;
            events = new ArrayList<>();
            throwingEvents = new ArrayList<>();
        }

        private LogEvents(String sourceName, List<ILoggingEvent> events, List<ILoggingEvent> throwingEvents)
        {
            this.sourceName = sourceName;
            this.events = new ArrayList<>(events);
            this.throwingEvents = new ArrayList<>(throwingEvents);
        }

        private static Predicate<ILoggingEvent> getPredicate(Level level, boolean includeHigherLevels)
        {
            return includeHigherLevels ?
                   loggingEvent -> loggingEvent.getLevel().isGreaterOrEqual(level) :
                   loggingEvent -> loggingEvent.getLevel().equals(level);
        }

        public List<ILoggingEvent> getSelection(boolean onlyThrowing, Level level, boolean includeHigherLevels)
        {
            final List<ILoggingEvent> selected = onlyThrowing ? throwingEvents : events;

            if (level.isGreaterOrEqual(Level.ALL) && includeHigherLevels)
                return new ArrayList<>(selected);

            return selected.stream().filter(getPredicate(level, includeHigherLevels)).toList();
        }

        public void add(ILoggingEvent event)
        {
            events.add(event);
            if (getThrowable(event) != null)
                throwingEvents.add(event);
        }

        public void clear()
        {
            events.clear();
            throwingEvents.clear();
        }

        public int size(boolean onlyThrowing, Level level, boolean includeHigherLevels)
        {
            final List<ILoggingEvent> selected = onlyThrowing ? throwingEvents : events;

            if (includeHigherLevels && level.isGreaterOrEqual(Level.ALL))
                return selected.size();

            return Math.toIntExact(selected.stream().filter(getPredicate(level, includeHigherLevels)).count());
        }

        public Optional<Throwable> getLastThrowable(Level level, boolean includeHigherLevels)
        {
            if (throwingEvents.isEmpty())
                return Optional.empty();
            if (includeHigherLevels && level.isGreaterOrEqual(Level.ALL))
            {
                final @Nullable Throwable throwable = getThrowable(throwingEvents.get(throwingEvents.size() - 1));
                if (throwable != null)
                    return Optional.of(throwable);
            }

            for (int idx = throwingEvents.size() - 1; idx >= 0; --idx)
            {
                final @Nullable Throwable throwable = getThrowable(throwingEvents.get(idx));
                if (throwable != null)
                    return Optional.of(throwable);
            }
            return Optional.empty();
        }

        private static @Nullable Throwable getThrowable(ILoggingEvent loggingEvent)
        {
            if (loggingEvent.getThrowableProxy() instanceof ThrowableProxy throwableProxy)
                return throwableProxy.getThrowable();
            return null;
        }
    }
}
