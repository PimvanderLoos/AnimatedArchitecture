package nl.pim16aap2.bigdoors.logging;

import nl.pim16aap2.bigdoors.api.IMessagingInterface;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.function.Supplier;
import java.util.logging.Level;

public interface IPLogger
{
    /**
     * The format of the date to be used when writing to the log file.
     */
    @NotNull SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");

    /**
     * Formats the name properly for logging purposes. For example: '[BigDoors]'
     *
     * @param name The name to be used for logging purposes.
     * @return The name in the proper format.
     */
    static @NotNull String formatName(@NotNull String name)
    {
        return "[" + name + "] ";
    }

    /**
     * Sends a message to whomever or whatever issued a command at a given level (if applicable).
     *
     * @param target The recipient of this message of unspecified type (console, player, whatever).
     * @param level  The level of the message (info, warn, etc). Does not apply to players.
     * @param str    The message.
     * @see IMessagingInterface#sendMessageToTarget(Object, Level, String)
     */
    void sendMessageToTarget(final @NotNull Object target, final @NotNull Level level, final @NotNull String str);

    /**
     * Dumps the stack trace to the log file at an arbitrary location.
     *
     * @param message An optional message to be printed along with the stack trace.
     */
    void dumpStackTrace(@NotNull String message);

    /**
     * Dumps the stack trace to the log file at an arbitrary location.
     *
     * @param message An optional message to be printed along with the stack trace.
     */
    void dumpStackTrace(@NotNull Level level, @NotNull String message);

    /**
     * Writes a message of a given level to the console.
     *
     * @param level      The level of the message.
     * @param logMessage The message to log
     * @see IMessagingInterface#writeToConsole(Level, String)
     */
    void writeToConsole(@NotNull Level level, @NotNull LogMessage logMessage);

    /**
     * Logs a message to the log file and potentially to the console as well at a given level.
     *
     * @param msg   The message to be logged.
     * @param level The level at which the message is logged (info, warn, etc).
     */
    void logMessage(@NotNull Level level, @NotNull String msg);

    /**
     * Logs a {@link Throwable} without writing it in the console.
     *
     * @param throwable The {@link Throwable} to log.
     * @param message   Message to accompany the exception.
     */
    void logThrowableSilently(@NotNull Throwable throwable, @NotNull String message);

    /**
     * Logs a {@link Throwable} without writing it in the console.
     *
     * @param level     The level at which to log this throwable.
     * @param throwable The {@link Throwable} to log.
     * @param message   Message to accompany the exception.
     */
    void logThrowableSilently(@NotNull Level level, @NotNull Throwable throwable,
                              @NotNull String message);

    /**
     * Logs a {@link Throwable} without writing it in the console.
     *
     * @param throwable The {@link Throwable} to log.
     */
    void logThrowableSilently(@NotNull Throwable throwable);

    /**
     * Logs a {@link Throwable} without writing it in the console.
     *
     * @param level     The level at which to log this throwable.
     * @param throwable The {@link Throwable} to log.
     */
    void logThrowableSilently(@NotNull Level level, @NotNull Throwable throwable);

    /**
     * Logs a {@link Throwable} without writing it in the console.
     *
     * @param level     The level at which to log this throwable.
     * @param throwable The {@link Throwable} to log.
     * @param message   Message to accompany the exception.
     */
    void logThrowable(@NotNull Level level, @NotNull Throwable throwable,
                      @NotNull String message);

    /**
     * Logs a {@link Throwable} without writing it in the console.
     *
     * @param throwable The {@link Throwable} to log.
     * @param message   Message to accompany the exception.
     */
    void logThrowable(@NotNull Throwable throwable, @NotNull String message);

    /**
     * Logs a {@link Throwable} without writing it in the console.
     *
     * @param level     The level at which to log this throwable.
     * @param throwable The {@link Throwable} to log.
     */
    void logThrowable(@NotNull Level level, @NotNull Throwable throwable);

    /**
     * Logs a {@link Throwable} without writing it in the console.
     *
     * @param throwable The {@link Throwable} to log.
     */
    void logThrowable(@NotNull Throwable throwable);

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
    void logMessage(@NotNull Level level, @NotNull String message,
                    @NotNull Supplier<String> messageSupplier);

    /**
     * Logs a message at a certain log {@link Level}.
     *
     * @param level           The log {@link Level} to log the message at. If the level is lower than this, it won't be
     *                        logged at all.
     * @param messageSupplier The {@link Supplier} to retrieve a message. If this message isn't logged because the
     *                        loglevel doesn't allow to log it at the provided <code>level</code>, this won't be
     *                        retrieved at all.
     */
    void logMessage(@NotNull Level level, @NotNull Supplier<String> messageSupplier);

    /**
     * Logs a message at info level.
     *
     * @param str The message to log.
     */
    void info(@NotNull String str);

    /**
     * Logs a message at warning level.
     *
     * @param str The message to log.
     */
    void warn(@NotNull String str);

    /**
     * Logs a message at severe level.
     *
     * @param str The message to log.
     */
    void severe(@NotNull String str);

    /**
     * Logs a message at debug level.
     *
     * @param str The message to log.
     */
    void debug(@NotNull String str);

    /**
     * Determines the log {@link Level} for this logger considering the log file. {@link Level}s with a {@link
     * Level#intValue()} lower than that of the current {@link Level} will be ignored.
     *
     * @param consoleLogLevel The new log {@link Level} for logging to the log file.
     */
    void setConsoleLogLevel(@NotNull Level consoleLogLevel);

    /**
     * Determines the log {@link Level} for this logger considering the console. {@link Level}s with a {@link
     * Level#intValue()} lower than that of the current {@link Level} will be ignored.
     *
     * @param fileLogLevel The new log {@link Level} for logging to the console.
     */
    void setFileLogLevel(@NotNull Level fileLogLevel);
}
