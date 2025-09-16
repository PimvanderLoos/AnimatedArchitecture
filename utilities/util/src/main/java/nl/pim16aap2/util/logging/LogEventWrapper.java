package nl.pim16aap2.util.logging;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.jspecify.annotations.Nullable;

@AllArgsConstructor
class LogEventWrapper implements LogEvent
{
    @Delegate
    private final LogEvent logEvent;
    @Getter
    private final @Nullable Marker marker;
}
