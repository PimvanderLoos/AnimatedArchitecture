package nl.pim16aap2.bigdoors.util;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IMessagingInterface;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");

    /**
     * The file to write to.
     */
    private File logFile = null;

    /**
     * The queue of {@link LogMessage}s that will be written to the log.
     */
    private final BlockingQueue<LogMessage> messageQueue = new LinkedBlockingQueue<>();

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
     * The instance of this {@link PLogger}.
     */
    private static PLogger instance = new PLogger();

    /**
     * Controls what is written to the log file. When enabled, only exceptions and errors are written to the log file.
     */
    private AtomicBoolean onlyLogExceptions = new AtomicBoolean(false);

    private PLogger()
    {
    }

    /**
     * Initializes the PLogger. Once initialized, it will start writing any messages to the log file. Until then, it
     * just adds them to the queue.
     *
     * @param logFile The file to write to.
     * @return The PLogger instance.
     */
    @NotNull
    public static PLogger init(final @NotNull File logFile)
    {
        if (instance.isInitialized())
        {
            instance.logException(new IllegalStateException("Trying to change the log file while it's already set!"));
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
    private boolean isInitialized()
    {
        return logFile != null;
    }

    /**
     * Gets the instance of this PLogger if it has been initiated.
     *
     * @return The instance of this PLogger.
     */
    @NotNull
    public static PLogger get()
    {
        return instance;
    }

    /**
     * Formats the name properly for logging purposes. For example: '[BigDoors]'
     *
     * @param name The name to be used for logging purposes.
     * @return The name in the proper format.
     */
    @NotNull
    public static String formatName(final @NotNull String name)
    {
        return "[" + name + "] ";
    }

    /**
     * Enables or disables writing errors and exceptions to the console.
     *
     * @param consoleLogging True will log errors and exception to the console.
     */
    public void setConsoleLogging(final boolean consoleLogging)
    {
        this.consoleLogging = consoleLogging;
    }

    /**
     * Changes whether non-errors and non-exceptions are written to the log file.
     *
     * @param onlyLogExceptions True will only log errors and exception to the log file.
     */
    public void setOnlyLogExceptions(final boolean onlyLogExceptions)
    {
        this.onlyLogExceptions.set(onlyLogExceptions);
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
            {
                LogMessage msg = messageQueue.take();
                if (onlyLogExceptions.get() && !(msg instanceof LogMessageError || msg instanceof LogMessageException))
                    continue;
                writeToLog(msg.toString());
            }
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
     * Adds a message to the queue of messages that will be written to the log file.
     *
     * @param logMessage The {@link LogMessage} to be written to the log file.
     */
    private void addToMessageQueue(final @NotNull LogMessage logMessage)
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
    public void dumpStackTrace(final @NotNull String message)
    {
        dumpBoundedStackTrace(message, 0);
    }

    /**
     * Dumps the stack trace to the log file at an arbitrary location. Only print a given number of lines.
     *
     * @param message       An optional message to be printed along with the stack trace.
     * @param numberOfLines The number of lines to be written to the log.
     */
    public void dumpBoundedStackTrace(final @NotNull String message, final int numberOfLines)
    {
        addToMessageQueue(new LogMessageException(message + "\n", new Exception(), numberOfLines));
    }

    /**
     * Writes a message of a given level to the console.
     *
     * @param level   The level of the message.
     * @param message The message.
     * @see IMessagingInterface#writeToConsole(Level, String)
     */
    public void writeToConsole(final @NotNull Level level, final @NotNull String message)
    {
        BigDoors.get().getMessagingInterface().writeToConsole(level, message);
    }

    /**
     * Logs a message to the log file and potentially to the console as well at a given level.
     *
     * @param msg            The message to be logged.
     * @param level          The level at which the message is logged (info, warn, etc).
     * @param printToConsole If the message should be written to the console.
     */
    private void logMessage(final @NotNull String msg, final @NotNull Level level, final boolean printToConsole)
    {
        if (printToConsole)
            writeToConsole(level, msg);
        addToMessageQueue(new LogMessageString(msg));
    }

    /**
     * Logs a message to the log file and potentially to the console as well at a given level.
     *
     * @param msg   The message to be logged.
     * @param level The level at which the message is logged (info, warn, etc).
     */
    public void logMessage(final @NotNull String msg, final @NotNull Level level)
    {
        logMessage(msg, level, true);
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

    /**
     * Logs an exception to the log file.
     *
     * @param exception Exception to log.
     */
    public void logException(final @NotNull Exception exception)
    {
        addToMessageQueue(new LogMessageException(exception.getMessage() + "\n" + exception.getClass().getName() +
                                                      "\n", exception));
        if (consoleLogging)
            writeToConsole(Level.SEVERE, "\n" + exception.getClass().getName() + ": " + exception.getMessage() + "\n" +
                limitStackTraceLength(exception.getStackTrace(), 0));
        else
            writeToConsole(Level.SEVERE, exception.getMessage());
    }

    /**
     * Logs an exception to the log file.
     *
     * @param exception Exception to log.
     * @param message   Message to accompany the exception.
     */
    public void logException(final @NotNull Exception exception, final @NotNull String message)
    {
        addToMessageQueue(new LogMessageException(message + "\n" + exception.getClass().getName() + "\n", exception));
        if (consoleLogging)
            writeToConsole(Level.SEVERE, "\n" + exception.getClass().getName() + ": " + message + "\n" +
                limitStackTraceLength(exception.getStackTrace(), 0));
        else
            writeToConsole(Level.SEVERE, message);
    }

    /**
     * Logs an error to the log file.
     *
     * @param error Error to log.
     */
    public void logError(final @NotNull Error error)
    {
        addToMessageQueue(new LogMessageError(error.getMessage() + "\n", error));
        if (consoleLogging)
            writeToConsole(Level.SEVERE, "\n" + error.getClass().getName() + ": " + error.getMessage() + "\n" +
                limitStackTraceLength(error.getStackTrace(), 0));
        else
            writeToConsole(Level.SEVERE, error.getMessage());
    }

    /**
     * Logs an error to the log file.
     *
     * @param error   Error to log.
     * @param message Message to accompany the error.
     */
    public void logError(final @NotNull Error error, final @NotNull String message)
    {
        addToMessageQueue(new LogMessageError(message + "\n", error));
        if (consoleLogging)
            writeToConsole(Level.SEVERE, "\n" + error.getClass().getName() + ": " + message + "\n" +
                limitStackTraceLength(error.getStackTrace(), 0));
        else
            writeToConsole(Level.SEVERE, message);
    }

    /**
     * Logs a message to the log file.
     *
     * @param message The message to log.
     */
    public void logMessage(final @NotNull String message)
    {
        addToMessageQueue(new LogMessageString(message));
    }

    /**
     * Logs a message at info level.
     *
     * @param str The message to log.
     */
    public void info(final @NotNull String str)
    {
        logMessage(str, Level.INFO, true);
    }

    /**
     * Logs a message at warning level.
     *
     * @param str The message to log.
     */
    public void warn(final @NotNull String str)
    {
        logMessage(str, Level.WARNING, true);
    }

    /**
     * Logs a message at severe level.
     *
     * @param str The message to log.
     */
    public void severe(final @NotNull String str)
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
    @NotNull
    private static String limitStackTraceLength(final @NotNull StackTraceElement[] stackTrace, final int numberOfLines)
    {
        int linesToWrite = numberOfLines > 0 ? Math.min(numberOfLines, stackTrace.length) : stackTrace.length;
        StringBuilder sb = new StringBuilder();
        for (int idx = 0; idx < linesToWrite; ++idx)
            sb.append("    at ").append(stackTrace[idx]).append("\n");
        // If any lines were omitted, make sure to log that too.
        if (linesToWrite < stackTrace.length)
            sb.append((stackTrace.length - linesToWrite)).append(" more lines omitted...\n\n");
        return sb.toString();
    }

    /**
     * Represents base class of the logMessage types.
     *
     * @author Pim
     */
    private static abstract class LogMessage
    {
        protected final String message;

        LogMessage(final @NotNull String message)
        {
            this.message = message;
        }

        @NotNull
        @Override
        public String toString()
        {
            return message + "\n";
        }
    }

    /**
     * Represents a logMessage that logs an exception.
     *
     * @author Pim
     */
    private static class LogMessageException extends LogMessage
    {
        private final Exception exception;
        private final int numberOfLines;

        LogMessageException(final @NotNull String message, final @NotNull Exception exception, final int numberOfLines)
        {
            super(message);
            this.exception = exception;
            this.numberOfLines = numberOfLines;
        }

        LogMessageException(final @NotNull String message, final @NotNull Exception exception)
        {
            this(message, exception, 0);
        }

        @NotNull
        @Override
        public String toString()
        {
            return super.message + limitStackTraceLength(exception.getStackTrace(), numberOfLines);
        }
    }

    /**
     * Represents a logMessage that logs an error.
     *
     * @author Pim
     */
    private static class LogMessageError extends LogMessage
    {
        private final Error error;
        private final int numberOfLines;

        LogMessageError(final @NotNull String message, final @NotNull Error error, final int numberOfLines)
        {
            super(message);
            this.error = error;
            this.numberOfLines = numberOfLines;
        }

        LogMessageError(final @NotNull String message, final @NotNull Error error)
        {
            this(message, error, 0);
        }

        @NotNull
        @Override
        public String toString()
        {
            return super.toString() + limitStackTraceLength(error.getStackTrace(), numberOfLines);
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
            super(message);
        }

        @NotNull
        @Override
        public String toString()
        {
            return super.toString();
        }
    }
}
