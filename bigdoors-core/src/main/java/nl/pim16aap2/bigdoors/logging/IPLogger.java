package nl.pim16aap2.bigdoors.logging;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IMessagingInterface;

import java.text.SimpleDateFormat;
import java.util.function.Supplier;
import java.util.logging.Level;

public interface IPLogger
{
    /**
     * The format of the date to be used when writing to the log file.
     */
    @NonNull SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");

    /**
     * Formats the name properly for logging purposes. For example: '[BigDoors]'
     *
     * @param name The name to be used for logging purposes.
     * @return The name in the proper format.
     */
    static @NonNull String formatName(@NonNull String name)
    {
        return "[" + name + "] ";
    }

    /**
     * Dumps the stack trace to the log file at an arbitrary location.
     *
     * @param message An optional message to be printed along with the stack trace.
     */
    void dumpStackTrace(@NonNull String message);

    /**
     * Dumps the stack trace to the log file at an arbitrary location.
     *
     * @param level   The level at which to log the stacktrace.
     * @param message An optional message to be printed along with the stack trace.
     */
    void dumpStackTrace(@NonNull Level level, @NonNull String message);

    /**
     * Writes a message of a given level to the console.
     *
     * @param level      The level of the message.
     * @param logMessage The message to log
     * @see IMessagingInterface#writeToConsole(Level, String)
     */
    void writeToConsole(@NonNull Level level, @NonNull LogMessage logMessage);

    /**
     * Logs a message to the log file and potentially to the console as well at a given level.
     *
     * @param msg   The message to be logged.
     * @param level The level at which the message is logged (info, warn, etc).
     */
    void logMessage(@NonNull Level level, @NonNull String msg);

    /**
     * Logs a {@link Throwable} without writing it in the console.
     *
     * @param throwable The {@link Throwable} to log.
     * @param message   Message to accompany the exception.
     */
    void logThrowableSilently(@NonNull Throwable throwable, @NonNull String message);

    /**
     * Logs a {@link Throwable} without writing it in the console.
     *
     * @param level     The level at which to log this throwable.
     * @param throwable The {@link Throwable} to log.
     * @param message   Message to accompany the exception.
     */
    void logThrowableSilently(@NonNull Level level, @NonNull Throwable throwable, @NonNull String message);

    /**
     * Logs a {@link Throwable} without writing it in the console.
     *
     * @param throwable The {@link Throwable} to log.
     */
    void logThrowableSilently(@NonNull Throwable throwable);

    /**
     * Logs a {@link Throwable} without writing it in the console.
     *
     * @param level     The level at which to log this throwable.
     * @param throwable The {@link Throwable} to log.
     */
    void logThrowableSilently(@NonNull Level level, @NonNull Throwable throwable);

    /**
     * Logs a {@link Throwable} without writing it in the console.
     *
     * @param level     The level at which to log this throwable.
     * @param throwable The {@link Throwable} to log.
     * @param message   Message to accompany the exception.
     */
    void logThrowable(@NonNull Level level, @NonNull Throwable throwable, @NonNull String message);

    /**
     * Logs a {@link Throwable} without writing it in the console.
     *
     * @param throwable The {@link Throwable} to log.
     * @param message   Message to accompany the exception.
     */
    void logThrowable(@NonNull Throwable throwable, @NonNull String message);

    /**
     * Logs a {@link Throwable} without writing it in the console.
     *
     * @param level     The level at which to log this throwable.
     * @param throwable The {@link Throwable} to log.
     */
    void logThrowable(@NonNull Level level, @NonNull Throwable throwable);

    /**
     * Logs a {@link Throwable} without writing it in the console.
     *
     * @param throwable The {@link Throwable} to log.
     */
    void logThrowable(@NonNull Throwable throwable);

    /**
     * Logs a message at a certain log {@link Level}.
     *
     * @param level           The log {@link Level} to log the message at. If the level is lower than this, it won't be
     *                        logged at all.
     * @param message         The base message.
     * @param messageSupplier The {@link Supplier} to retrieve a message. If this message isn't logged because the
     *                        loglevel doesn't allow to log it at the provided <code>level</code>, this won't be
     *                        retrieved at all.
     */
    void logMessage(@NonNull Level level, @NonNull String message, @NonNull Supplier<String> messageSupplier);

    /**
     * Logs a message at a certain log {@link Level}.
     *
     * @param level           The log {@link Level} to log the message at. If the level is lower than this, it won't be
     *                        logged at all.
     * @param messageSupplier The {@link Supplier} to retrieve a message. If this message isn't logged because the
     *                        loglevel doesn't allow to log it at the provided <code>level</code>, this won't be
     *                        retrieved at all.
     */
    void logMessage(@NonNull Level level, @NonNull Supplier<String> messageSupplier);

    /**
     * Checks if a given {@link Level} is loggable.
     * <p>
     * This may be false when the provided log level (e.g. {@link Level#FINE}) is lower than the selected minimum value
     * (e.g. {@link Level#WARNING}).
     *
     * @param level The {@link Level} to check.
     * @return True if the levels can be logged.
     */
    boolean loggable(@NonNull Level level);

    /**
     * Logs a message at info level.
     *
     * @param str The message to log.
     */
    default void info(@NonNull String str)
    {
        logMessage(Level.INFO, str);
    }

    /**
     * Logs a message at warning level.
     *
     * @param str The message to log.
     */
    default void warn(@NonNull String str)
    {
        logMessage(Level.WARNING, str);
    }

    /**
     * Logs a message at severe level.
     *
     * @param str The message to log.
     */
    default void severe(@NonNull String str)
    {
        logMessage(Level.SEVERE, str);
    }

    /**
     * Logs a message at debug level.
     *
     * @param str The message to log.
     */
    default void debug(@NonNull String str)
    {
        logMessage(Level.FINEST, str);
    }

    /**
     * Determines the log {@link Level} for this logger considering the log file. {@link Level}s with a {@link
     * Level#intValue()} lower than that of the current {@link Level} will be ignored.
     *
     * @param consoleLogLevel The new log {@link Level} for logging to the log file.
     */
    void setConsoleLogLevel(@NonNull Level consoleLogLevel);

    /**
     * Determines the log {@link Level} for this logger considering the console. {@link Level}s with a {@link
     * Level#intValue()} lower than that of the current {@link Level} will be ignored.
     *
     * @param fileLogLevel The new log {@link Level} for logging to the console.
     */
    void setFileLogLevel(@NonNull Level fileLogLevel);
}
