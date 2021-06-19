package nl.pim16aap2.bigdoors.logging;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Represents base class of the logMessage types.
 *
 * @author Pim
 */
public abstract class LogMessage
{
    protected final @NotNull String message;

    protected final @NotNull Level logLevel;

    /**
     * The id of the thread from which this messages was created.
     */
    private final long threadID;

    protected LogMessage(final @NotNull String message, final @NotNull Level logLevel)
    {
        this.message = message;
        this.logLevel = logLevel;
        threadID = Thread.currentThread().getId();
    }

    @Override
    public @NotNull String toString()
    {
        return getFormattedLevel() + getFormattedThreadID() + message;
    }

    /**
     * Formats a message. If it's null or empty, it'll return an empty String.
     *
     * @param str The message to format.
     * @return The formatted message.
     */
    protected static @NotNull String checkMessage(final @Nullable String str)
    {
        if (str == null || str.equals("\n"))
            return "";
        return str + "\n";
    }

    private @NotNull String getFormattedThreadID()
    {
        return String.format("Thread [%d] ", threadID);
    }

    private @NotNull String getFormattedLevel()
    {
        return String.format("(%s) ", logLevel.toString());
    }

    /**
     * Converts the stack trace of a {@link Throwable} to a string.
     *
     * @param throwable The {@link Throwable} whose stack trace to retrieve as String.
     * @return A string of the stack trace.
     */
    private static @NotNull String throwableStackTraceToString(final @NotNull Throwable throwable)
    {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    private static @NotNull String stackTraceToString(final @NotNull StackTraceElement[] stackTrace, final int skip)
    {
        final StringBuilder sb = new StringBuilder();
        for (int idx = skip; idx < stackTrace.length; ++idx)
            sb.append("\tat ").append(stackTrace[idx]).append("\n");
        return sb.toString();
    }

    /**
     * Represents a logMessage that logs a {@link Throwable}.
     *
     * @author Pim
     */
    public static class LogMessageThrowable extends LogMessage
    {
        LogMessageThrowable(final @NotNull Throwable throwable, final @NotNull String message,
                            final @NotNull Level logLevel)
        {
            super(checkMessage(message) + checkMessage(throwableStackTraceToString(throwable)), logLevel);
        }
    }

    /**
     * Represents a {@link LogMessage} that logs a stack trace.
     *
     * @author Pim
     */
    public static class LogMessageStackTrace extends LogMessage
    {
        /**
         * Logs a new stack trace.
         *
         * @param stackTrace The stack trace to log.
         * @param message    The message to log as the header.
         * @param logLevel   The level at which to log the resulting message.
         * @param skip       The number of elements in the stack trace to skip.
         */
        LogMessageStackTrace(final @NotNull StackTraceElement[] stackTrace, final @NotNull String message,
                             final @NotNull Level logLevel, final int skip)
        {
            super(checkMessage(message) + checkMessage(stackTraceToString(stackTrace, skip)), logLevel);
        }

        LogMessageStackTrace(final @NotNull StackTraceElement[] stackTrace, final @NotNull String message,
                             final @NotNull Level logLevel)
        {
            this(stackTrace, message, logLevel, 0);
        }
    }

    /**
     * Represents a logMessage that logs a string.
     *
     * @author Pim
     */
    public static class LogMessageString extends LogMessage
    {
        LogMessageString(final @NotNull String message, final @NotNull Level logLevel)
        {
            super(checkMessage(message), logLevel);
        }
    }

    public static class LogMessageStringSupplier extends LogMessage
    {
        LogMessageStringSupplier(final @NotNull String message, final @NotNull Supplier<String> stringSupplier,
                                 final @NotNull Level logLevel)
        {
            super(checkMessage(message) + checkMessage(stringSupplier.get()), logLevel);
        }
    }
}
