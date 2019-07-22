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
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
    private static AtomicLong queueProcessor = null;
    private final File logFile;
    private final BlockingQueue<LogMessage> messageQueue = new LinkedBlockingQueue<>();
    private final IMessagingInterface messagingInterface;
    private boolean success = false;
    private boolean debug = false;

    /**
     * Constructor of PLogger
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
     * Format the name properly for logging purposes. For example: '[BigDoors]'
     *
     * @param name The name to be used for logging purposes.
     * @return The name in the proper format.
     */
    public static @NotNull String formatName(@NotNull final String name)
    {
        return "[" + name + "] ";
    }

    /**
     * Change debugging status.
     *
     * @param debug True to enable debugging.
     */
    public void setDebug(boolean debug)
    {
        this.debug = debug;
    }

    /**
     * Process the queue of messages that will be logged to the log file. It cannot run on the main thread and only a
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
     * Send a message to whomever or whatever issued a command at a given level (if applicable).
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
     * Add a message to the queue of messages that will be written to the log file.
     *
     * @param logMessage The {@link LogMessage} to be written to the log file.
     */
    private void addToMessageQueue(@NotNull final LogMessage logMessage)
    {
        messageQueue.add(logMessage);
    }

    // Initialize log
    private void prepareLog()
    {
        if (!logFile.exists())
            try
            {
                if (!logFile.getParentFile().exists())
                    logFile.getParentFile().mkdirs();
                logFile.createNewFile();
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
     * Dump the stack trace to the log file at an arbitrary location.
     *
     * @param message An optional message to be printed along with the stack trace.
     */
    public void dumpStackTrace(@NotNull final String message)
    {
        dumpBoundedStackTrace(message, 0);
    }

    /**
     * Dump the stack trace to the log file at an arbitrary location. Only print a given number of lines.
     *
     * @param message       An optional message to be printed along with the stack trace.
     * @param numberOfLines The number of lines to be written to the log.
     */
    public void dumpBoundedStackTrace(@NotNull final String message, final int numberOfLines)
    {
        addToMessageQueue(this.new LogMessageException(message + "\n", new Exception(), numberOfLines));
    }

    /**
     * Write a message of a given level to the console.
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
     * Log a message to the log file and potentially to the console as well at a given level.
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
     * Write a message to the log file.
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
     * Log an exception to the log file.
     *
     * @param exception Exception to log.
     */
    public void logException(@NotNull final Exception exception)
    {
        addToMessageQueue(this.new LogMessageException(exception.getMessage(), exception));
        exception.printStackTrace();
        if (debug)
            exception.printStackTrace();
    }

    /**
     * Log an exception to the log file.
     *
     * @param exception Exception to log.
     * @param message   Message to accompany the exception.
     */
    public void logException(@NotNull final Exception exception, @NotNull String message)
    {
        message += "\n";
        addToMessageQueue(this.new LogMessageException(message, exception));
        writeToConsole(Level.SEVERE, message);
        exception.printStackTrace();
        if (debug)
            exception.printStackTrace();
    }

    /**
     * Log an error to the log file.
     *
     * @param error Error to log.
     */
    public void logError(@NotNull final Error error)
    {
        addToMessageQueue(this.new LogMessageError(error.getMessage(), error));
        error.printStackTrace();
        if (debug)
            error.printStackTrace();
    }

    /**
     * Log an error to the log file.
     *
     * @param error   Error to log.
     * @param message Message to accompany the error.
     */
    public void logError(@NotNull final Error error, @NotNull String message)
    {
        message += "\n";
        addToMessageQueue(this.new LogMessageError(message, error));
        writeToConsole(Level.SEVERE, message);
        error.printStackTrace();
        if (debug)
            error.printStackTrace();
    }

    /**
     * Log a message to the log file.
     *
     * @param message The message to log.
     */
    public void logMessage(@NotNull final String message)
    {
        addToMessageQueue(this.new LogMessageString(message));
    }

    /**
     * Log a message at info level.
     *
     * @param str The message to log.
     */
    public void info(@NotNull final String str)
    {
        logMessage(str, Level.INFO, true);
    }

    /**
     * Log a message at warning level.
     *
     * @param str The message to log.
     */
    public void warn(@NotNull final String str)
    {
        logMessage(str, Level.WARNING, true);
    }

    /**
     * Log a message at severe level.
     *
     * @param str The message to log.
     */
    public void severe(@NotNull final String str)
    {
        logMessage(str, Level.SEVERE, true);
    }

    /**
     * Limit the length of a stack trace to a provided number of lines. If the provided number of lines is less than 1
     * or exceeds the number of elements, all existing elements will get printed.
     *
     * @param stackTrace    The stack trace to be limited.
     * @param numberOfLines The number of lines to limit it to.
     * @return A string of the stack trace for at most numberOfLines lines if lines > 0.
     */
    private @NotNull String limitStackTraceLength(@NotNull final StackTraceElement[] stackTrace, int numberOfLines)
    {
        if (numberOfLines < 0)
            numberOfLines = 0;
        StringBuilder sb = new StringBuilder();
        for (int idx = 1; idx < (numberOfLines + 1) && idx < stackTrace.length; ++idx)
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

        public LogMessageException(@NotNull final String message, @NotNull final Exception exception,
                                   final int numberOfLines)
        {
            super(message);
            this.exception = exception;
            this.numberOfLines = numberOfLines;
        }

        public LogMessageException(@NotNull final String message, @NotNull final Exception exception)
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

        public LogMessageError(@NotNull final String message, @NotNull final Error error, final int numberOfLines)
        {
            super(message);
            this.error = error;
            this.numberOfLines = numberOfLines;
        }

        public LogMessageError(@NotNull final String message, @NotNull final Error error)
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
        public LogMessageString(@NotNull final String message)
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
