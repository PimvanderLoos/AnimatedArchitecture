package nl.pim16aap2.bigdoors;

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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import nl.pim16aap2.bigdoors.util.ConfigLoader;
import nl.pim16aap2.bigdoors.util.Util;

/**
 * Represents my logger. Logs to the console synchronously and to the log file
 * asynchronously.
 *
 * @author Pim
 */
public class MyLogger
{
    private final JavaPlugin plugin;
    private final File logFile;
    private final BlockingQueue<LogMessage> messageQueue = new LinkedBlockingQueue<>();
    private static AtomicLong queueProcessor = null;
    private boolean success = false;

    public MyLogger(final JavaPlugin plugin, final File logFile)
    {
        this.plugin = plugin;
        this.logFile = logFile;
        prepareLog();
        if (success)
            new Thread(() -> processQueue()).start();
    }

    /**
     * Process the queue of messages that will be logged to the log file. It cannot
     * run on the main thread and only a single instance is possible. The thread is
     * blocked while it waits for new messages.
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
            Bukkit.getScheduler().callSyncMethod(plugin, () ->
            {
                writeToConsole(Level.SEVERE, "Cannot write to log file! Please contact pim16aap2!");
                e.printStackTrace();
                return null;
            });
        }
    }

    /**
     * Add a message to the queue of messages that will be written to the log file.
     *
     * @param logMessage The {@link LogMessage} to be written to the log file.
     */
    private void addToMessageQueue(final LogMessage logMessage)
    {
        messageQueue.add(logMessage);
    }

    // Initialize log
    private void prepareLog()
    {
        if (!logFile.exists())
            try
            {
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
    public void dumpStackTrace(@Nullable final String message)
    {
        dumpBoundedStackTrace(message, 0);
    }

    /**
     * Dump the stack trace to the log file at an arbitrary location. Only print a
     * given number of lines.
     *
     * @param message       An optional message to be printed along with the stack
     *                      trace.
     * @param numberOfLines The number of lines to be written to the log.
     */
    public void dumpBoundedStackTrace(@Nullable final String message, final int numberOfLines)
    {
        addToMessageQueue(this.new LogMessageException(message + "\n", new Exception(), numberOfLines));
    }

    public void handleMyStackTrace(final MyException e)
    {
        if (e.hasWarningMessage())
            warn(e.getWarningMessage());
        addToMessageQueue(this.new LogMessageException(e.getWarningMessage(), e));
    }

    /**
     * Write a message of a given level to the console.
     *
     * @param level The level of the message.
     * @param str   The meesage.
     */
    private void writeToConsole(final Level level, final String str)
    {
        Bukkit.getLogger().log(level, "[" + plugin.getName() + "] " + str);
    }

    /**
     * Send a message to whomever or whatever issued a command at a given level (if
     * applicable) with a given color (if applicable).
     *
     * @param sender The CommandSender()
     * @param level  The level of the message (info, warn, etc). Does not apply to
     *               players.
     * @param color  The color of the message. Only applies to players.
     * @param str    The message.
     */
    public void returnToSender(final CommandSender sender, final Level level, final @Nullable ChatColor color,
                               final @Nonnull String str)
    {
        if (sender instanceof Player)
            Util.messagePlayer((Player) sender, (color != null ? color : "") + str);
        else
            writeToConsole(level, ChatColor.stripColor(str));
    }

    /**
     * Send a message to whomever or whatever issued a command at INFO level with a
     * given color (if applicable).
     *
     * @param sender The CommandSender()
     * @param color  The color of the message. Only applies to players.
     * @param str    The message.
     */
    public void returnToSender(final CommandSender sender, @Nullable final ChatColor color, final String str)
    {
        returnToSender(sender, Level.INFO, color, str);
    }

    /**
     * Log a message to the log file and potentially to the console as well at a
     * given level.
     *
     * @param msg            The message to be logged.
     * @param level          The level at which the message is logged (info, warn,
     *                       etc).
     * @param printToConsole If the message should be written to the console.
     */
    private void logMessage(final String msg, final Level level, final boolean printToConsole)
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
    private void writeToLog(final String msg)
    {
        try
        {
            BufferedWriter bw = new BufferedWriter(new FileWriter(logFile, true));
            Date now = new Date();
            SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            bw.write("[" + format.format(now) + "] " + msg);
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
    public void logException(final Exception exception)
    {
        addToMessageQueue(this.new LogMessageException(null, exception));
        writeToConsole(Level.SEVERE, exception.toString());
        if (ConfigLoader.DEBUG)
            exception.printStackTrace();
    }

    /**
     * Log an exception to the log file.
     *
     * @param exception Exception to log.
     * @param message   Message to accompany the exception.
     */
    public void logException(final Exception exception, String message)
    {
        message += "\n";
        addToMessageQueue(this.new LogMessageException(message, exception));
        writeToConsole(Level.SEVERE, message + exception.toString());
        if (ConfigLoader.DEBUG)
            exception.printStackTrace();
    }

    /**
     * Log an error to the log file.
     *
     * @param exception Error to log.
     */
    public void logError(final Error error)
    {
        addToMessageQueue(this.new LogMessageError(null, error));
        writeToConsole(Level.SEVERE, error.toString());
        if (ConfigLoader.DEBUG)
            error.printStackTrace();
    }

    /**
     * Log an error to the log file.
     *
     * @param error   Error to log.
     * @param message Message to accompany the error.
     */
    public void logError(final Error error, String message)
    {
        message += "\n";
        addToMessageQueue(this.new LogMessageError(message, error));
        writeToConsole(Level.SEVERE, message + error.toString());
        if (ConfigLoader.DEBUG)
            error.printStackTrace();
    }

    /**
     * Log a message to the log file.
     *
     * @param message The message to log.
     */
    public void logMessage(final String message)
    {
        addToMessageQueue(this.new LogMessageString(message));
    }

    /**
     * Log a message at info level.
     *
     * @param str The message to log.
     */
    public void info(final String str)
    {
        logMessage(str, Level.INFO, true);
    }

    /**
     * Log a message at warning level.
     *
     * @param str The message to log.
     */
    public void warn(final String str)
    {
        logMessage(str, Level.WARNING, true);
    }

    /**
     * Log a message at severe level.
     *
     * @param str The message to log.
     */
    public void severe(final String str)
    {
        logMessage(str, Level.SEVERE, true);
    }

    /**
     * Limit the length of a stack trace to a provided number of lines. If the
     * provided number of lines is less than 1 or exceeds the number of elements,
     * all existing elements will get printed.
     *
     * @param stackTrace    The stack trace to be limited.
     * @param numberOfLines The number of lines to limit it to.
     * @return A string of the stack trace for at most numberOfLines lines if lines
     *         > 0.
     */
    private String limitStackTraceLength(final StackTraceElement[] stackTrace, int numberOfLines)
    {
        if (numberOfLines < 0)
            numberOfLines = 0;
        StringBuilder sb = new StringBuilder();
        for (int idx = 1; (idx == 0 || idx < (numberOfLines + 1)) && idx < stackTrace.length; ++idx)
            sb.append("    " + stackTrace[idx] + "\n");
        return sb.toString();
    }

    /**
     * Represents base class of the logMessage types.
     *
     * @author Pim
     */
    private abstract class LogMessage
    {
        protected final @Nullable String message;

        LogMessage(@Nullable final String message)
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

        public LogMessageException(@Nullable final String message, final Exception exception, final int numberOfLines)
        {
            super(message);
            this.exception = exception;
            this.numberOfLines = numberOfLines;
        }

        public LogMessageException(@Nullable final String message, final Exception exception)
        {
            this(message, exception, 0);
        }

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
    private class LogMessageError extends LogMessage
    {
        private final Error error;
        private final int numberOfLines;

        public LogMessageError(@Nullable final String message, final Error error, final int numberOfLines)
        {
            super(message);
            this.error = error;
            this.numberOfLines = numberOfLines;
        }

        public LogMessageError(@Nullable final String message, final Error error)
        {
            this(message, error, 0);
        }

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
    private class LogMessageString extends LogMessage
    {
        public LogMessageString(@Nullable final String message)
        {
            super(message);
        }

        @Override
        public String toString()
        {
            return super.toString();
        }
    }
}
