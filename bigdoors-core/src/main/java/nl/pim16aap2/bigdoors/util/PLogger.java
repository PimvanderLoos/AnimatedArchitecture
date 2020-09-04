package nl.pim16aap2.bigdoors.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IMessagingInterface;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Represents my logger. Logs to the console synchronously and to the log file asynchronously.
 *
 * @author Pim
 */
public final class PLogger
{
    /**
     * The format of the date to be used when writing to the log file.
     */
    @NotNull
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");

    /**
     * The file to write to.
     */
    @Nullable
    private File logFile = null;

    /**
     * The queue of {@link LogMessage}s that will be written to the log.
     */
    @NotNull
    private final BlockingQueue<LogMessage> messageQueue = new LinkedBlockingQueue<>();

    /**
     * Check if the log file could be initialized properly.
     */
    private boolean success = false;

    /**
     * Determines the log {@link Level} for this logger considering the log file. {@link Level}s with a {@link
     * Level#intValue()} lower than that of the current {@link Level} will be ignored.
     *
     * @param fileLogLevel The new log {@link Level} for logging to the logFile.
     */
    @Getter
    @NotNull
    private Level fileLogLevel = Level.FINER;

    @Getter
    @NotNull
    private Level consoleLogLevel = Level.CONFIG;

    @Getter
    @NotNull
    private Level lowestLevel = Level.CONFIG;


    /**
     * The instance of this {@link PLogger}.
     */
    @NotNull
    private static final PLogger instance = new PLogger();

    private PLogger()
    {
        updateLowestLevel();
    }

    /**
     * Initializes the PLogger. Once initialized, it will start writing any messages to the log file. Until then, it
     * just adds them to the queue.
     *
     * @param logFile The file to write to.
     * @return The PLogger instance.
     */
    public static @NotNull PLogger init(final @NotNull File logFile)
    {
        if (instance.isInitialized())
        {
            instance.logThrowable(new IllegalStateException("Trying to change the log file while it's already set!"));
            return instance;
        }

        instance.logFile = logFile;
        instance.prepareLog();
        if (instance.success)
            new Thread(instance::processQueue).start();

        return instance;
    }

    /**
     * Checks if this {@link PLogger} has been initialized.
     *
     * @return True if this logger has been initialized.
     */
    public boolean isInitialized()
    {
        return logFile != null;
    }

    /**
     * Gets the instance of this PLogger if it has been initiated.
     *
     * @return The instance of this PLogger.
     */
    public static @NotNull PLogger get()
    {
        return instance;
    }

    /**
     * Formats the name properly for logging purposes. For example: '[BigDoors]'
     *
     * @param name The name to be used for logging purposes.
     * @return The name in the proper format.
     */
    public static @NotNull String formatName(final @NotNull String name)
    {
        return "[" + name + "] ";
    }

    /**
     * Checks if the {@link #messageQueue} is empty.
     *
     * @return True if the {@link #messageQueue} is empty.
     */
    public boolean isEmpty()
    {
        return messageQueue.isEmpty();
    }

    public int size()
    {
        return messageQueue.size();
    }

    /**
     * Processes the queue of messages that will be logged to the log file.
     */
    private void processQueue()
    {
        try
        {
            // Keep getting new LogMessages. It's a blocked queue, so the thread
            // will just sleep until there's a new entry if it's currently empty.
            while (true)
                writeToLog(messageQueue.take().toString());
        }
        catch (InterruptedException e)
        {
            // Yes, this can result in garbled text, as it's not run on the main thread.
            // But it shouldn't ever be reached anyway.
            System.out.println("Cannot write to log file! Please contact pim16aap2!");
            e.printStackTrace();
        }
    }

    /**
     * Sends a message to whomever or whatever issued a command at a given level (if applicable).
     *
     * @param target The recipient of this message of unspecified type (console, player, whatever).
     * @param level  The level of the message (info, warn, etc). Does not apply to players.
     * @param str    The message.
     * @see IMessagingInterface#sendMessageToTarget(Object, Level, String)
     */
    public void sendMessageToTarget(final @NotNull Object target, final @NotNull Level level, final @NotNull String str)
    {
        BigDoors.get().getMessagingInterface().sendMessageToTarget(target, level, str);
    }

    /**
     * Checks if this {@link PLogger} is in a valid state and if a message at a given level can be logged at all (either
     * console or file).
     *
     * @param level The level to compare against the allowed levels.
     * @return True if the provided level can be logged to the {@link PLogger}.
     */
    private boolean canLog(final @NotNull Level level)
    {
        if (isInitialized() && !success)
            throw new IllegalStateException("PLogger was not initialized successfully!");

        return level.intValue() >= lowestLevel.intValue();
    }

    /**
     * Adds a message to the queue of messages that will be written to the log file and the console. The respective log
     * levels for both methods will be checked before logging the {@link LogMessage}.
     *
     * @param logMessageSupplier The {@link Supplier} that will create the {@link LogMessage} that is to be written to
     *                           the log file and the console.
     * @param level              The level of the message (info, warn, etc).
     */
    private void addToMessageQueue(final @NotNull Level level, final @NotNull Supplier<LogMessage> logMessageSupplier)
    {
        if (!canLog(level))
            return;

        final @NotNull LogMessage logMessage = logMessageSupplier.get();
        if (level.intValue() >= consoleLogLevel.intValue())
            writeToConsole(level, logMessage);

        if (level.intValue() >= fileLogLevel.intValue())
            messageQueue.add(logMessage);
    }

    /**
     * Adds a message to the queue of messages that will be written to the log file if {@link #fileLogLevel} permits
     * it.
     * <p>
     * It will <b>not</b> log anything to the console.
     *
     * @param logMessageSupplier The {@link Supplier} that will create the {@link LogMessage} that is to be written to
     *                           the log file.
     * @param level              The level of the message (info, warn, etc).
     */
    private void addToSilentMessageQueue(final @NotNull Level level,
                                         final @NotNull Supplier<LogMessage> logMessageSupplier)
    {
        if (!canLog(level))
            return;

        if (level.intValue() >= fileLogLevel.intValue())
            messageQueue.add(logMessageSupplier.get());
    }

    /**
     * Creates the log file, if it doesn't exist already.
     */
    private void prepareLog()
    {
        if (!logFile.exists())
            try
            {
                if (!logFile.getParentFile().exists())
                    if (!logFile.getParentFile().mkdirs())
                    {
                        writeToConsole(Level.SEVERE,
                                       "Failed to create folder: \"" + logFile.getParentFile().toString() + "\"");
                        return;
                    }
                if (!logFile.createNewFile())
                {
                    writeToConsole(Level.SEVERE, "Failed to create file: \"" + logFile.toString() + "\"");
                    return;
                }
                writeToConsole(Level.INFO, "New file created at " + logFile);
                success = true;
            }
            catch (IOException e)
            {
                writeToConsole(Level.SEVERE, "File write error: " + logFile);
                e.printStackTrace();
                return;
            }
        success = true;
    }

    /**
     * Dumps the stack trace to the log file at an arbitrary location.
     *
     * @param message An optional message to be printed along with the stack trace.
     */
    public void dumpStackTrace(final @NotNull String message)
    {
        dumpBoundedStackTrace(Level.SEVERE, message, 0);
    }

    /**
     * Dumps the stack trace to the log file at an arbitrary location.
     *
     * @param message An optional message to be printed along with the stack trace.
     */
    public void dumpStackTrace(final @NotNull Level level, final @NotNull String message)
    {
        dumpBoundedStackTrace(level, message, 0);
    }

    /**
     * Dumps the stack trace to the log file at an arbitrary location. Only print a given number of lines.
     *
     * @param message       An optional message to be printed along with the stack trace.
     * @param numberOfLines The number of lines to be written to the log.
     */
    public void dumpBoundedStackTrace(final @NotNull Level level, final @NotNull String message,
                                      final int numberOfLines)
    {
        addToMessageQueue(level, () -> new LogMessageThrowable(new Exception(), message, numberOfLines));
    }

    /**
     * Writes a message of a given level to the console.
     *
     * @param level  The level of the message.
     * @param string The message to log
     * @see IMessagingInterface#writeToConsole(Level, String)
     */
    private void writeToConsole(final @NotNull Level level, final @NotNull String string)
    {
        BigDoors.get().getMessagingInterface().writeToConsole(level, string);
    }

    /**
     * Writes a message of a given level to the console.
     *
     * @param level      The level of the message.
     * @param logMessage The message to log
     * @see IMessagingInterface#writeToConsole(Level, String)
     */
    public void writeToConsole(final @NotNull Level level, final @NotNull LogMessage logMessage)
    {
        writeToConsole(level, logMessage.toString());
    }

    /**
     * Logs a message to the log file and potentially to the console as well at a given level.
     *
     * @param msg   The message to be logged.
     * @param level The level at which the message is logged (info, warn, etc).
     */
    public void logMessage(final @NotNull Level level, final @NotNull String msg)
    {
        addToMessageQueue(level, () -> new LogMessageString(msg));
    }

    /**
     * Writes a message to the log file.
     *
     * @param msg The message to be written.
     */
    private void writeToLog(final @NotNull String msg)
    {
        try (final BufferedWriter bw = new BufferedWriter(new FileWriter(logFile, true)))
        {
            bw.write("[" + dateFormat.format(new Date()) + "] " + msg);
            bw.flush();
        }
        catch (IOException e)
        {
            writeToConsole(Level.SEVERE, "Logging error! Could not log to logFile!");
            e.printStackTrace();
        }
    }

    private void addThrowableToQueue(final @NotNull Level level, final @NotNull Throwable throwable,
                                     final @NotNull String message)
    {
        addToMessageQueue(level, () -> new LogMessageThrowable(throwable, message));
    }

    /**
     * Logs a {@link Throwable} without writing it in the console.
     *
     * @param throwable The {@link Throwable} to log.
     * @param message   Message to accompany the exception.
     */
    public void logThrowableSilently(final @NotNull Throwable throwable, final @NotNull String message)
    {
        addToSilentMessageQueue(Level.SEVERE, () -> new LogMessageThrowable(throwable, message));
    }

    /**
     * Logs a {@link Throwable} without writing it in the console.
     *
     * @param level     The level at which to log this throwable.
     * @param throwable The {@link Throwable} to log.
     * @param message   Message to accompany the exception.
     */
    public void logThrowableSilently(final @NotNull Level level, final @NotNull Throwable throwable,
                                     final @NotNull String message)
    {
        addToSilentMessageQueue(level, () -> new LogMessageThrowable(throwable, message));
    }

    /**
     * Logs a {@link Throwable} without writing it in the console.
     *
     * @param throwable The {@link Throwable} to log.
     */
    public void logThrowableSilently(final @NotNull Throwable throwable)
    {
        logThrowableSilently(throwable, "");
    }

    /**
     * Logs a {@link Throwable} without writing it in the console.
     *
     * @param level     The level at which to log this throwable.
     * @param throwable The {@link Throwable} to log.
     */
    public void logThrowableSilently(final @NotNull Level level, final @NotNull Throwable throwable)
    {
        logThrowableSilently(level, throwable, "");
    }

    /**
     * Logs a {@link Throwable} without writing it in the console.
     *
     * @param throwable The {@link Throwable} to log.
     * @param message   Message to accompany the exception.
     */
    public void logThrowable(final @NotNull Throwable throwable, final @NotNull String message)
    {
        addThrowableToQueue(Level.SEVERE, throwable, message);

        if (consoleLogLevel.intValue() == Level.OFF.intValue())
            writeToConsole(Level.OFF, throwable.toString());

        if (fileLogLevel.intValue() == Level.OFF.intValue())
            addToMessageQueue(Level.OFF, () -> new LogMessageString(throwable.toString()));
    }

    /**
     * Logs a {@link Throwable} without writing it in the console.
     *
     * @param throwable The {@link Throwable} to log.
     */
    public void logThrowable(final @NotNull Throwable throwable)
    {
        logThrowable(throwable, "");
    }

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
    public void logMessage(final @NotNull Level level, final @NotNull String message,
                           final @NotNull Supplier<String> messageSupplier)
    {
        addToMessageQueue(level, () -> new LogMessageStringSupplier(message, messageSupplier));
    }

    /**
     * Logs a message at a certain log {@link Level}.
     *
     * @param level           The log {@link Level} to log the message at. If the level is lower than this, it won't be
     *                        logged at all.
     * @param messageSupplier The {@link Supplier} to retrieve a message. If this message isn't logged because the
     *                        loglevel doesn't allow to log it at the provided <code>level</code>, this won't be
     *                        retrieved at all.
     */
    public void logMessage(final @NotNull Level level, final @NotNull Supplier<String> messageSupplier)
    {
        logMessage(level, "", messageSupplier);
    }

    /**
     * Logs a message at info level.
     *
     * @param str The message to log.
     */
    public void info(final @NotNull String str)
    {
        logMessage(Level.INFO, str);
    }

    /**
     * Logs a message at warning level.
     *
     * @param str The message to log.
     */
    public void warn(final @NotNull String str)
    {
        logMessage(Level.WARNING, str);
    }

    /**
     * Logs a message at severe level.
     *
     * @param str The message to log.
     */
    public void severe(final @NotNull String str)
    {
        logMessage(Level.SEVERE, str);
    }

    /**
     * Logs a message at debug level.
     *
     * @param str The message to log.
     */
    public void debug(final @NotNull String str)
    {
        logMessage(Level.FINEST, str);
    }

    /**
     * Limits the length of a stack trace to a provided number of lines. If the provided number of lines is less than 1
     * or exceeds the number of elements, all existing elements will get printed.
     *
     * @param throwable     The {@link Throwable} whose stacktrace to get.
     * @param numberOfLines The number of lines to limit it to.
     * @return A string of the stack trace for at most numberOfLines lines if numberOfLines > 0.
     */
    private static @NotNull String limitStackTraceLength(final @NotNull Throwable throwable, final int numberOfLines)
    {
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        int linesToWrite = numberOfLines > 0 ? Math.min(numberOfLines, stackTrace.length) : stackTrace.length;

        StringBuilder sb = new StringBuilder().append(throwable).append("\n");
        for (int idx = 0; idx < linesToWrite; ++idx)
            sb.append("    at ").append(stackTrace[idx]).append("\n");
        // If any lines were omitted, make sure to log that too.
        if (linesToWrite < stackTrace.length)
            sb.append((stackTrace.length - linesToWrite)).append(" more lines omitted...\n\n");
        return sb.toString();
    }

    /**
     * Determines the log {@link Level} for this logger considering the log file. {@link Level}s with a {@link
     * Level#intValue()} lower than that of the current {@link Level} will be ignored.
     *
     * @param consoleLogLevel The new log {@link Level} for logging to the log file.
     */
    public void setConsoleLogLevel(final @NotNull Level consoleLogLevel)
    {
        this.consoleLogLevel = consoleLogLevel;
        updateLowestLevel();
    }

    /**
     * Determines the log {@link Level} for this logger considering the console. {@link Level}s with a {@link
     * Level#intValue()} lower than that of the current {@link Level} will be ignored.
     *
     * @param fileLogLevel The new log {@link Level} for logging to the console.
     */
    public void setFileLogLevel(final @NotNull Level fileLogLevel)
    {
        this.fileLogLevel = fileLogLevel;
        updateLowestLevel();
    }

    private void updateLowestLevel()
    {
        if (fileLogLevel.intValue() > consoleLogLevel.intValue())
            lowestLevel = consoleLogLevel;
        else
            lowestLevel = fileLogLevel;
    }

    /**
     * Represents base class of the logMessage types.
     *
     * @author Pim
     */
    @AllArgsConstructor
    private static abstract class LogMessage
    {
        @NotNull
        protected final String message;

        @Override
        public @NotNull String toString()
        {
            return message;
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
    }

    /**
     * Represents a logMessage that logs a {@link Throwable}.
     *
     * @author Pim
     */
    private static class LogMessageThrowable extends LogMessage
    {
        LogMessageThrowable(final @NotNull Throwable throwable, final @NotNull String message, final int numberOfLines)
        {
            super(checkMessage(message) + limitStackTraceLength(throwable, numberOfLines));
        }

        LogMessageThrowable(final @NotNull Throwable throwable, final @NotNull String message)
        {
            this(throwable, message, 0);
        }
    }

    /**
     * Represents a logMessage that logs a string.
     *
     * @author Pim
     */
    private static class LogMessageString extends LogMessage
    {
        LogMessageString(final @NotNull String message)
        {
            super(checkMessage(message));
        }
    }

    private static class LogMessageStringSupplier extends LogMessage
    {
        LogMessageStringSupplier(final @NotNull String message, final @NotNull Supplier<String> stringSupplier)
        {
            super(checkMessage(message) + stringSupplier.get());
        }
    }
}
