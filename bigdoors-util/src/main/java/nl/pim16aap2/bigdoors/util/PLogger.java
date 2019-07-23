package nl.pim16aap2.bigdoors.util;

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
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * Represents my logger. Logs to the console synchronously and to the log file asynchronously.
 *
 * @author Pim
 */
public class PLogger
{
    /**
     * The format of the date to be used when writing to the log file.
     */
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
    /**
     * The ID of the thread that's processing the queue of messages to write to the log.
     */
    private static AtomicLong queueProcessor = null;
    /**
     * The file to write to.
     */
    private final File logFile;
    /**
     * The queue of {@link LogMessage}s that will be written to the log.
     */
    private final BlockingQueue<LogMessage> messageQueue = new LinkedBlockingQueue<>();
    /**
     * The {@link IMessagingInterface} use for platform-specific messaging. For example writing to console, sending a
     * message to a player.
     */
    private final IMessagingInterface messagingInterface;
    /**
     * Check if the log file could be initialized properly.
     */
    private boolean success = false;
    /**
     * Determine if errors and exceptions should be written to the console or not. They'll always be written to the log
     * file.
     */
    private boolean consoleLogging = false;

    /**
     * Constructs a PLogger
     *
     * @param logFile            The file to write to.
     * @param messagingInterface The implementation of {@link IMessagingInterface} for writing to the console etc.
     * @param name               The name that will be used for logging. For example "BigDoors".
     */
    public PLogger(final File logFile, final IMessagingInterface messagingInterface, final String name)
    {
        this.logFile = logFile;
        this.messagingInterface = messagingInterface;
        prepareLog();
        if (success)
            new Thread(() -> processQueue()).start();
    }

    /**
     * Formats the name properly for logging purposes. For example: '[BigDoors]'
     *
     * @param name The name to be used for logging purposes.
     * @return The name in the proper format.
     */
    public static @NotNull String formatName(@NotNull final String name)
    {
        return "[" + name + "] ";
    }

    /**
     * Enables or disables writing errors and exceptions to the console.
     *
     * @param consoleLogging True will log errors and exception to the console.
     */
    public void setConsoleLogging(boolean consoleLogging)
    {
        this.consoleLogging = consoleLogging;
    }

    /**
     * Processes the queue of messages that will be logged to the log file. It cannot run on the main thread and only a
     * single instance is possible. The thread is blocked while it waits for new messages.
     */
    private void processQueue()
    {
        // It is not allowed to run this method on the main thread.
        // Also, only a single instance is allowed
        if (Thread.currentThread().getId() == 1 || queueProcessor != null)
            throw new IllegalStateException("Trying to instantiate processQueue on thread "
                                                    + Thread.currentThread().getId());
        queueProcessor = new AtomicLong(Thread.currentThread().getId());

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
    public void sendMessageToTarget(@NotNull final Object target, @NotNull Level level, @NotNull final String str)
    {
        if (messagingInterface != null)
            messagingInterface.sendMessageToTarget(target, level, str);
    }

    /**
     * Adds a message to the queue of messages that will be written to the log file.
     *
     * @param logMessage The {@link LogMessage} to be written to the log file.
     */
    private void addToMessageQueue(@NotNull final LogMessage logMessage)
    {
        messageQueue.add(logMessage);
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
    public void dumpStackTrace(@NotNull final String message)
    {
        dumpBoundedStackTrace(message, 0);
    }

    /**
     * Dumps the stack trace to the log file at an arbitrary location. Only print a given number of lines.
     *
     * @param message       An optional message to be printed along with the stack trace.
     * @param numberOfLines The number of lines to be written to the log.
     */
    public void dumpBoundedStackTrace(@NotNull final String message, final int numberOfLines)
    {
        addToMessageQueue(this.new LogMessageException(message + "\n", new Exception(), numberOfLines));
    }

    /**
     * Writes a message of a given level to the console.
     *
     * @param level   The level of the message.
     * @param message The message.
     * @see IMessagingInterface#writeToConsole(Level, String)
     */
    public void writeToConsole(@NotNull final Level level, @NotNull final String message)
    {
        if (messagingInterface != null)
            messagingInterface.writeToConsole(level, message);
    }

    /**
     * Logs a message to the log file and potentially to the console as well at a given level.
     *
     * @param msg            The message to be logged.
     * @param level          The level at which the message is logged (info, warn, etc).
     * @param printToConsole If the message should be written to the console.
     */
    private void logMessage(@NotNull final String msg, @NotNull final Level level, final boolean printToConsole)
    {
        if (printToConsole)
            writeToConsole(level, msg);
        addToMessageQueue(this.new LogMessageString(msg));
    }

    /**
     * Writes a message to the log file.
     *
     * @param msg The message to be written.
     */
    private void writeToLog(@NotNull final String msg)
    {
        try
        {
            BufferedWriter bw = new BufferedWriter(new FileWriter(logFile, true));
            Date now = new Date();
            bw.write("[" + dateFormat.format(now) + "] " + msg);
            bw.flush();
            bw.close();
        }
        catch (IOException e)
        {
            writeToConsole(Level.SEVERE, "Logging error! Could not log to logFile!");
            e.printStackTrace();
        }
    }

    /**
     * Logs an exception to the log file.
     *
     * @param exception Exception to log.
     */
    public void logException(@NotNull final Exception exception)
    {
        addToMessageQueue(this.new LogMessageException(exception.getMessage(), exception));
        if (consoleLogging)
            writeToConsole(Level.SEVERE, limitStackTraceLength(exception.getStackTrace(), 0));
    }

    /**
     * Logs an exception to the log file.
     *
     * @param exception Exception to log.
     * @param message   Message to accompany the exception.
     */
    public void logException(@NotNull final Exception exception, @NotNull String message)
    {
        message += "\n";
        addToMessageQueue(this.new LogMessageException(message, exception));
        writeToConsole(Level.SEVERE, message);
        if (consoleLogging)
            writeToConsole(Level.SEVERE, limitStackTraceLength(exception.getStackTrace(), 0));
    }

    /**
     * Logs an error to the log file.
     *
     * @param error Error to log.
     */
    public void logError(@NotNull final Error error)
    {
        addToMessageQueue(this.new LogMessageError(error.getMessage(), error));
        if (consoleLogging)
            writeToConsole(Level.SEVERE, limitStackTraceLength(error.getStackTrace(), 0));
    }

    /**
     * Logs an error to the log file.
     *
     * @param error   Error to log.
     * @param message Message to accompany the error.
     */
    public void logError(@NotNull final Error error, @NotNull String message)
    {
        message += "\n";
        addToMessageQueue(this.new LogMessageError(message, error));
        writeToConsole(Level.SEVERE, message);
        if (consoleLogging)
            writeToConsole(Level.SEVERE, limitStackTraceLength(error.getStackTrace(), 0));
    }

    /**
     * Logs a message to the log file.
     *
     * @param message The message to log.
     */
    public void logMessage(@NotNull final String message)
    {
        addToMessageQueue(this.new LogMessageString(message));
    }

    /**
     * Logs a message at info level.
     *
     * @param str The message to log.
     */
    public void info(@NotNull final String str)
    {
        logMessage(str, Level.INFO, true);
    }

    /**
     * Logs a message at warning level.
     *
     * @param str The message to log.
     */
    public void warn(@NotNull final String str)
    {
        logMessage(str, Level.WARNING, true);
    }

    /**
     * Logs a message at severe level.
     *
     * @param str The message to log.
     */
    public void severe(@NotNull final String str)
    {
        logMessage(str, Level.SEVERE, true);
    }

    /**
     * Limits the length of a stack trace to a provided number of lines. If the provided number of lines is less than 1
     * or exceeds the number of elements, all existing elements will get printed.
     *
     * @param stackTrace    The stack trace to be limited.
     * @param numberOfLines The number of lines to limit it to.
     * @return A string of the stack trace for at most numberOfLines lines if numberOfLines > 0.
     */
    private @NotNull String limitStackTraceLength(@NotNull final StackTraceElement[] stackTrace,
                                                  final int numberOfLines)
    {
        int linesToWrite = numberOfLines > 0 ? Math.min(numberOfLines, stackTrace.length) : stackTrace.length;
        StringBuilder sb = new StringBuilder();
        for (int idx = 0; idx < linesToWrite; ++idx)
            sb.append("    ").append(stackTrace[idx]).append("\n");
        return sb.toString();
    }

    /**
     * Represents base class of the logMessage types.
     *
     * @author Pim
     */
    private abstract class LogMessage
    {
        protected final String message;

        LogMessage(final @Nullable String message)
        {
            this.message = message;
        }

        @Override
        public String toString()
        {
            return message == null ? "" : (message + "\n");
        }
    }

    /**
     * Represents a logMessage that logs an exception.
     *
     * @author Pim
     */
    private class LogMessageException extends LogMessage
    {
        private final Exception exception;
        private final int numberOfLines;

        LogMessageException(@NotNull final String message, @NotNull final Exception exception,
                            final int numberOfLines)
        {
            super(message);
            this.exception = exception;
            this.numberOfLines = numberOfLines;
        }

        LogMessageException(@NotNull final String message, @NotNull final Exception exception)
        {
            this(message, exception, 0);
        }

        @Override
        public @NotNull String toString()
        {
            System.out.println("Writing exception!");
            return super.message + limitStackTraceLength(exception.getStackTrace(), numberOfLines);
        }
    }

    /**
     * Represents a logMessage that logs an error.
     *
     * @author Pim
     */
    private class LogMessageError extends LogMessage
    {
        private final Error error;
        private final int numberOfLines;

        LogMessageError(@NotNull final String message, @NotNull final Error error, final int numberOfLines)
        {
            super(message);
            this.error = error;
            this.numberOfLines = numberOfLines;
        }

        LogMessageError(@NotNull final String message, @NotNull final Error error)
        {
            this(message, error, 0);
        }

        @Override
        public @NotNull String toString()
        {
            return super.toString() + limitStackTraceLength(error.getStackTrace(), numberOfLines);
        }
    }

    /**
     * Represents a logMessage that logs a string.
     *
     * @author Pim
     */
    private class LogMessageString extends LogMessage
    {
        LogMessageString(@NotNull final String message)
        {
            super(message);
        }

        @Override
        public @NotNull String toString()
        {
            return super.toString();
        }
    }
}
