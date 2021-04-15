package nl.pim16aap2.bigdoors.logging;

import lombok.NonNull;
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
    protected final @NonNull String message;

    protected final @NonNull Level logLevel;

    /**
     * The id of the thread from which this messages was created.
     */
    private final long threadID;

    protected LogMessage(final @NonNull String message, final @NonNull Level logLevel)
    {
        this.message = message;
        this.logLevel = logLevel;
        threadID = Thread.currentThread().getId();
    }

    @Override
    public @NonNull String toString()
    {
        return getFormattedLevel() + getFormattedThreadID() + message;
    }

    /**
     * Formats a message. If it's null or empty, it'll return an empty String.
     *
     * @param str The message to format.
     * @return The formatted message.
     */
    protected static @NonNull String checkMessage(final @Nullable String str)
    {
        if (str == null || str.equals("\n"))
            return "";
        return str + "\n";
    }

    private @NonNull String getFormattedThreadID()
    {
        return String.format("Thread [%d] ", threadID);
    }

    private @NonNull String getFormattedLevel()
    {
        return String.format("(%s) ", logLevel.toString());
    }

    /**
     * Converts a stacktrace to a string.
     *
     * @param throwable The {@link Throwable} whose stacktrace to get.
     * @return A string of the stack trace.
     */
    private static @NonNull String stacktraceToString(final @NonNull Throwable throwable)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Represents a logMessage that logs a {@link Throwable}.
     *
     * @author Pim
     */
    public static class LogMessageThrowable extends LogMessage
    {
        LogMessageThrowable(final @NonNull Throwable throwable, final @NonNull String message,
                            final @NonNull Level logLevel)
        {
            super(checkMessage(message) + checkMessage(stacktraceToString(throwable)), logLevel);
        }
    }

    /**
     * Represents a logMessage that logs a string.
     *
     * @author Pim
     */
    public static class LogMessageString extends LogMessage
    {
        LogMessageString(final @NonNull String message, final @NonNull Level logLevel)
        {
            super(checkMessage(message), logLevel);
        }
    }

    public static class LogMessageStringSupplier extends LogMessage
    {
        LogMessageStringSupplier(final @NonNull String message, final @NonNull Supplier<String> stringSupplier,
                                 final @NonNull Level logLevel)
        {
            super(checkMessage(message) + checkMessage(stringSupplier.get()), logLevel);
        }
    }
}
