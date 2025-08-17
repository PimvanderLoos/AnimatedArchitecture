package nl.pim16aap2.util.logging;

import com.google.common.flogger.FluentLogger;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.slf4j.event.Level;

/**
 * A simple facade for Flogger's {@link FluentLogger} to use slf4j log level names.
 */
@SuppressWarnings("FloggerSplitLogStatement")
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public final class FloggerFacade
{
    private final FluentLogger flogger;

    /**
     * See {@link FluentLogger#at(java.util.logging.Level)}.
     *
     * @param level
     *     The log level to use.
     * @return The next step in the fluent API for logging at the given level.
     */
    public FluentLogger.Api at(Level level)
    {
        return switch (level)
        {
            case ERROR -> atError();
            case WARN -> atWarn();
            case INFO -> atInfo();
            case DEBUG -> atDebug();
            case TRACE -> atTrace();
        };
    }

    /**
     * A convenience method for logging at the ERROR level.
     *
     * @return The next step in the fluent API for logging at the ERROR level.
     */
    public FluentLogger.Api atError()
    {
        return flogger.atSevere();
    }

    /**
     * A convenience method for logging at the WARN level.
     *
     * @return The next step in the fluent API for logging at the WARN level.
     */
    public FluentLogger.Api atWarn()
    {
        return flogger.atWarning();
    }

    /**
     * A convenience method for logging at the INFO level.
     *
     * @return The next step in the fluent API for logging at the INFO level.
     */
    public FluentLogger.Api atInfo()
    {
        return flogger.atInfo();
    }

    /**
     * A convenience method for logging at the DEBUG level.
     *
     * @return The next step in the fluent API for logging at the DEBUG level.
     */
    public FluentLogger.Api atDebug()
    {
        return flogger.atFine();
    }

    /**
     * A convenience method for logging at the TRACE level.
     *
     * @return The next step in the fluent API for logging at the TRACE level.
     */
    public FluentLogger.Api atTrace()
    {
        return flogger.atFinest();
    }
}
